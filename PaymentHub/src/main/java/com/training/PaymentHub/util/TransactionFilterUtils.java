package com.training.PaymentHub.util;

import com.training.PaymentHub.model.dto.request.TransactionSearchRequest;
import org.springframework.util.StringUtils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tiện ích chuẩn hóa thông tin bộ lọc giao dịch để hiển thị trong đầu file Excel
 * và trên giao diện danh sách file xuất.
 */
public final class TransactionFilterUtils {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String NO_FILTER_TEXT = "Tất cả dữ liệu (Không áp dụng bộ lọc)";

    private TransactionFilterUtils() {
    }

    public record FilterEntry(String label, String value) {
        public String formatted() {
            return label + ": " + value;
        }
    }

    /**
     * Trả về danh sách các cặp (Tên trường, Giá trị) đã tham gia lọc dữ liệu.
     */
    public static List<FilterEntry> extractFilterEntries(TransactionSearchRequest filter) {
        List<FilterEntry> entries = new ArrayList<>();
        if (filter == null) {
            return entries;
        }

        if (StringUtils.hasText(filter.getKeyword())) {
            entries.add(new FilterEntry("Từ khóa", filter.getKeyword().trim()));
        }
        if (StringUtils.hasText(filter.getTransactionCode())) {
            entries.add(new FilterEntry("Mã giao dịch", filter.getTransactionCode().trim()));
        }
        if (StringUtils.hasText(filter.getAccountNo())) {
            entries.add(new FilterEntry("Số tài khoản", filter.getAccountNo().trim()));
        }
        if (StringUtils.hasText(filter.getStatus())) {
            entries.add(new FilterEntry("Trạng thái giao dịch", formatStatus(filter.getStatus().trim())));
        }
        if (StringUtils.hasText(filter.getReferenceNo())) {
            entries.add(new FilterEntry("Mã tham chiếu", filter.getReferenceNo().trim()));
        }
        if (filter.getFromDate() != null) {
            entries.add(new FilterEntry("Từ ngày", filter.getFromDate().format(DATE_FMT)));
        }
        if (filter.getToDate() != null) {
            entries.add(new FilterEntry("Đến ngày", filter.getToDate().format(DATE_FMT)));
        }
        if (filter.getMinAmount() != null) {
            entries.add(new FilterEntry("Số tiền từ", formatAmount(filter.getMinAmount()) + " VNĐ"));
        }
        if (filter.getMaxAmount() != null) {
            entries.add(new FilterEntry("Số tiền đến", formatAmount(filter.getMaxAmount()) + " VNĐ"));
        }

        return entries;
    }

    /**
     * Tạo chuỗi tóm tắt bộ lọc trên 1 dòng để hiển thị trên Frontend.
     */
    public static String buildFilterSummary(TransactionSearchRequest filter) {
        List<FilterEntry> entries = extractFilterEntries(filter);
        if (entries.isEmpty()) {
            return NO_FILTER_TEXT;
        }
        return entries.stream()
                .map(FilterEntry::formatted)
                .reduce((a, b) -> a + "  |  " + b)
                .orElse(NO_FILTER_TEXT);
    }

    private static String formatStatus(String status) {
        if ("SUCCESS".equalsIgnoreCase(status)) {
            return "Thành công (SUCCESS)";
        }
        if ("FAILED".equalsIgnoreCase(status)) {
            return "Thất bại (FAILED)";
        }
        return status;
    }

    private static String formatAmount(java.math.BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("vi-VN"));
        symbols.setGroupingSeparator(',');
        DecimalFormat df = new DecimalFormat("#,##0.##", symbols);
        return df.format(amount);
    }
}
