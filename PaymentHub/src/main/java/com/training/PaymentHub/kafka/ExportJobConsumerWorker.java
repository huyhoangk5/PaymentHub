package com.training.PaymentHub.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.training.PaymentHub.constant.ExportJobStatus;
import com.training.PaymentHub.model.dto.ExportJobKafkaMessage;
import com.training.PaymentHub.model.dto.ExportNotificationEvent;
import com.training.PaymentHub.model.dto.request.TransactionSearchRequest;
import com.training.PaymentHub.model.entity.ExportJob;
import com.training.PaymentHub.repository.ExportJobRepository;
import com.training.PaymentHub.service.MinioStorageService;
import com.training.PaymentHub.service.RedisProgressService;
import com.training.PaymentHub.specification.TransactionLogSpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExportJobConsumerWorker {

    private final ObjectMapper objectMapper;
    private final ExportJobRepository exportJobRepository;
    private final JdbcTemplate jdbcTemplate;
    private final MinioStorageService minioStorageService;
    private final RedisProgressService redisProgressService;

    private static final int JDBC_FETCH_SIZE = 10_000;
    private static final int SXSSF_WINDOW_SIZE = 500;
    private static final int MAX_ROWS_PER_SHEET = 1_000_000;
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final String[] HEADERS = {
            "STT",
            "Mã giao dịch",
            "Số tài khoản",
            "Số tiền (VNĐ)",
            "Trạng thái",
            "Mã tham chiếu",
            "Mô tả",
            "Ngày tạo",
            "Ngày cập nhật"
    };

    @KafkaListener(
            topics = "${app.kafka.export-topic:export_jobs}",
            groupId = "${spring.kafka.consumer.group-id:paymenthub-export-worker-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeExportJob(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String jobId = record.key();
        ExportJobKafkaMessage message = null;
        try {
            message = objectMapper.readValue(record.value(), ExportJobKafkaMessage.class);
            jobId = message.getJobId();
        } catch (Exception e) {
            log.error("Không thể parse Kafka message tại offset {}: {}", record.offset(), e.getMessage());
            ack.acknowledge();
            return;
        }

        final String currentJobId = jobId;
        final String userId = message.getUserId();
        final TransactionSearchRequest filter = message.getFilter() != null
                ? message.getFilter()
                : redisProgressService.getJobFilter(currentJobId);

        log.info("Worker nhận Export Job: jobId={}, userId={}, partition={}, offset={}",
                currentJobId, userId, record.partition(), record.offset());

        // 1. Atomic Conditional Update để chống xử lý trùng lặp giữa các Worker
        int claimed = exportJobRepository.claimJobAtomically(
                currentJobId,
                ExportJobStatus.PENDING,
                ExportJobStatus.PROCESSING,
                10,
                LocalDateTime.now()
        );

        if (claimed == 0) {
            log.warn("Job {} không ở trạng thái PENDING (đã được xử lý bởi Worker khác hoặc đã kết thúc). Bỏ qua.", currentJobId);
            ack.acknowledge();
            return;
        }

        // 2. Phát thông báo bắt đầu PROCESSING (1 lần duy nhất khi bắt đầu, FE tự chạy thanh % giả lập 0->99%)
        redisProgressService.publishNotification(ExportNotificationEvent.builder()
                .jobId(currentJobId)
                .userId(userId)
                .status(ExportJobStatus.PROCESSING)
                .progress(10)
                .updatedAt(LocalDateTime.now())
                .build());

        Path tempExcelFile = null;
        SXSSFWorkbook workbook = null;
        boolean minioUploaded = false;
        long startTime = System.currentTimeMillis();

        try {
            tempExcelFile = Files.createTempFile("pmh-export-" + currentJobId + "-", ".xlsx");
            workbook = new SXSSFWorkbook(SXSSF_WINDOW_SIZE);
            workbook.setCompressTempFiles(true);

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle amountStyle = createAmountStyle(workbook);

            TransactionLogSpec.SqlQueryBundle queryBundle = TransactionLogSpec.buildStreamingSql(filter, false);
            log.info("Bắt đầu truy vấn Streaming cho Job {}: SQL={}", currentJobId, queryBundle.sql());

            final SXSSFWorkbook wbRef = workbook;
            final AtomicInteger sheetIndex = new AtomicInteger(1);
            final AtomicInteger currentSheetRowIdx = new AtomicInteger(0);
            final AtomicLong totalProcessed = new AtomicLong(0);
            final Sheet[] activeSheet = new Sheet[]{createSheetWithHeader(wbRef, sheetIndex.get(), headerStyle)};
            currentSheetRowIdx.set(1);

            jdbcTemplate.query(con -> {
                PreparedStatement ps = con.prepareStatement(
                        queryBundle.sql(),
                        ResultSet.TYPE_FORWARD_ONLY,
                        ResultSet.CONCUR_READ_ONLY
                );
                ps.setFetchSize(JDBC_FETCH_SIZE);
                Object[] params = queryBundle.params();
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
                return ps;
            }, (ResultSet rs) -> {
                if (currentSheetRowIdx.get() > MAX_ROWS_PER_SHEET) {
                    int nextSheetNo = sheetIndex.incrementAndGet();
                    activeSheet[0] = createSheetWithHeader(wbRef, nextSheetNo, headerStyle);
                    currentSheetRowIdx.set(1);
                }

                long stt = totalProcessed.incrementAndGet();
                Row row = activeSheet[0].createRow(currentSheetRowIdx.getAndIncrement());

                row.createCell(0).setCellValue(stt);
                row.createCell(1).setCellValue(nullToEmpty(rs.getString("TRANSACTION_CODE")));
                row.createCell(2).setCellValue(nullToEmpty(rs.getString("ACCOUNT_NO")));

                Cell amountCell = row.createCell(3);
                BigDecimal amount = rs.getBigDecimal("AMOUNT");
                if (amount != null) {
                    amountCell.setCellValue(amount.doubleValue());
                    amountCell.setCellStyle(amountStyle);
                } else {
                    amountCell.setCellValue(0D);
                }

                row.createCell(4).setCellValue(nullToEmpty(rs.getString("STATUS")));
                row.createCell(5).setCellValue(nullToEmpty(rs.getString("REFERENCE_NO")));
                row.createCell(6).setCellValue(nullToEmpty(rs.getString("DESCRIPTION")));

                Timestamp createdTs = rs.getTimestamp("CREATED_AT");
                row.createCell(7).setCellValue(createdTs != null ? createdTs.toLocalDateTime().format(DATE_TIME_FMT) : "");

                Timestamp updatedTs = rs.getTimestamp("UPDATED_AT");
                row.createCell(8).setCellValue(updatedTs != null ? updatedTs.toLocalDateTime().format(DATE_TIME_FMT) : "");
            });

            // 3. Ghi workbook ra file tạm trên ổ đĩa
            try (OutputStream os = Files.newOutputStream(tempExcelFile)) {
                workbook.write(os);
            }

            long totalRecords = totalProcessed.get();
            log.info("Đã kết xuất xong {} bản ghi ra file tạm cho Job {} trong {} ms. Đang tải lên MinIO...",
                    totalRecords, currentJobId, (System.currentTimeMillis() - startTime));

            // 4. Upload file lên MinIO
            String fileUrl = minioStorageService.uploadExportFile(currentJobId, tempExcelFile);
            minioUploaded = true;

            // 5. Cập nhật trạng thái SUCCESS vào Database trước khi thông báo cho Frontend
            updateJobSuccessInDbWithRetry(currentJobId, fileUrl, totalRecords);

            // 6. Phát sự kiện hoàn tất (100%) qua Redis Pub/Sub -> WebSocket đẩy về Frontend
            String downloadUrl = "/api/transactions/export/" + currentJobId + "/download";
            redisProgressService.publishNotification(ExportNotificationEvent.builder()
                    .jobId(currentJobId)
                    .userId(userId)
                    .status(ExportJobStatus.SUCCESS)
                    .progress(100)
                    .fileUrl(fileUrl)
                    .downloadUrl(downloadUrl)
                    .totalRecords(totalRecords)
                    .updatedAt(LocalDateTime.now())
                    .build());

            log.info("Hoàn tất toàn bộ Export Job {} ({} bản ghi) trong {} ms!",
                    currentJobId, totalRecords, (System.currentTimeMillis() - startTime));

            ack.acknowledge();

        } catch (Exception e) {
            log.error("Lỗi khi xử lý Export Job {}: {}", currentJobId, e.getMessage(), e);

            // Hành động bù trừ (Compensating Action): Nếu đã upload MinIO nhưng lỗi cập nhật DB thì xóa file mồ côi
            if (minioUploaded) {
                minioStorageService.deleteExportFile(currentJobId);
            }

            String errMsg = truncateError(e.getMessage());
            markJobFailed(currentJobId, userId, errMsg);
            ack.acknowledge();

        } finally {
            // 7. Giải phóng bộ nhớ và xóa toàn bộ file tạm SXSSF
            if (workbook != null) {
                try {
                    workbook.dispose();
                    workbook.close();
                } catch (Exception ignored) {
                }
            }
            if (tempExcelFile != null) {
                try {
                    Files.deleteIfExists(tempExcelFile);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private Sheet createSheetWithHeader(SXSSFWorkbook workbook, int sheetNumber, CellStyle headerStyle) {
        String sheetName = sheetNumber == 1 ? "Lich_su_giao_dich" : "Lich_su_giao_dich_" + sheetNumber;
        SXSSFSheet sheet = workbook.createSheet(sheetName);
        int[] widths = {10, 24, 18, 20, 14, 22, 30, 22, 22};
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }
        return sheet;
    }

    private CellStyle createHeaderStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createAmountStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.00"));
        return style;
    }

    private void updateJobSuccessInDbWithRetry(String jobId, String fileUrl, long totalRecords) {
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                ExportJob job = exportJobRepository.findByJobId(jobId)
                        .orElseThrow(() -> new IllegalStateException("Không tìm thấy ExportJob: " + jobId));
                job.setStatus(ExportJobStatus.SUCCESS);
                job.setProgress(100);
                job.setFileUrl(fileUrl);
                job.setTotalRecords(totalRecords);
                job.setErrorMessage(null);
                job.setUpdatedAt(LocalDateTime.now());
                exportJobRepository.save(job);
                return;
            } catch (Exception e) {
                if (attempt == maxAttempts) {
                    throw e;
                }
                try {
                    Thread.sleep(500L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ie);
                }
            }
        }
    }

    private void markJobFailed(String jobId, String userId, String errorMessage) {
        try {
            exportJobRepository.findByJobId(jobId).ifPresent(job -> {
                job.setStatus(ExportJobStatus.FAILED);
                job.setErrorMessage(errorMessage);
                job.setUpdatedAt(LocalDateTime.now());
                exportJobRepository.save(job);
            });
        } catch (Exception dbEx) {
            log.error("Không thể cập nhật trạng thái FAILED vào DB cho Job {}: {}", jobId, dbEx.getMessage());
        }

        redisProgressService.publishNotification(ExportNotificationEvent.builder()
                .jobId(jobId)
                .userId(userId)
                .status(ExportJobStatus.FAILED)
                .progress(0)
                .errorMessage(errorMessage)
                .updatedAt(LocalDateTime.now())
                .build());
    }

    private String nullToEmpty(String val) {
        return val != null ? val : "";
    }

    private String truncateError(String msg) {
        if (msg == null) return "Lỗi không xác định trong quá trình xuất file";
        return msg.length() > 950 ? msg.substring(0, 950) : msg;
    }
}
