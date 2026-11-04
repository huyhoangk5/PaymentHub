package com.training.PaymentHub.specification;

import com.training.PaymentHub.model.dto.request.TransactionSearchRequest;
import com.training.PaymentHub.model.entity.TransactionLog;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class TransactionLogSpec {

    public static Specification<TransactionLog> filter(TransactionSearchRequest req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (req == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            if (StringUtils.hasText(req.getKeyword())) {
                String kw = "%" + req.getKeyword().trim().toLowerCase() + "%";
                Predicate codePred = cb.like(cb.lower(root.get("transactionCode")), kw);
                Predicate accPred = cb.like(cb.lower(root.get("accountNo")), kw);
                Predicate refPred = cb.like(cb.lower(root.get("referenceNo")), kw);
                predicates.add(cb.or(codePred, accPred, refPred));
            }

            if (StringUtils.hasText(req.getTransactionCode())) {
                String code = "%" + req.getTransactionCode().trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("transactionCode")), code));
            }

            if (StringUtils.hasText(req.getAccountNo())) {
                String acc = "%" + req.getAccountNo().trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("accountNo")), acc));
            }

            if (StringUtils.hasText(req.getStatus())) {
                predicates.add(cb.equal(cb.upper(root.get("status")), req.getStatus().trim().toUpperCase()));
            }

            if (StringUtils.hasText(req.getReferenceNo())) {
                String ref = "%" + req.getReferenceNo().trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("referenceNo")), ref));
            }

            if (req.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), req.getFromDate().atStartOfDay()));
            }

            if (req.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), req.getToDate().atTime(LocalTime.MAX)));
            }

            if (req.getMinAmount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), req.getMinAmount()));
            }

            if (req.getMaxAmount() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), req.getMaxAmount()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Xây dựng câu lệnh SQL PreparedStatement an toàn (chống 100% SQL Injection)
     * với cùng bộ điều kiện lọc của Specification để phục vụ JDBC Streaming 1.000.000 bản ghi.
     */
    public static SqlQueryBundle buildStreamingSql(TransactionSearchRequest req, boolean countOnly) {
        StringBuilder sql = new StringBuilder();
        if (countOnly) {
            sql.append("SELECT COUNT(1) FROM TRANSACTION_LOG WHERE 1=1");
        } else {
            sql.append("SELECT ID, TRANSACTION_CODE, ACCOUNT_NO, AMOUNT, STATUS, CREATED_AT, DESCRIPTION, REFERENCE_NO, UPDATED_AT ")
               .append("FROM TRANSACTION_LOG WHERE 1=1");
        }

        List<Object> params = new ArrayList<>();

        if (req != null) {
            if (StringUtils.hasText(req.getKeyword())) {
                String kw = "%" + req.getKeyword().trim().toLowerCase() + "%";
                sql.append(" AND (LOWER(TRANSACTION_CODE) LIKE ? OR LOWER(ACCOUNT_NO) LIKE ? OR LOWER(REFERENCE_NO) LIKE ?)");
                params.add(kw);
                params.add(kw);
                params.add(kw);
            }

            if (StringUtils.hasText(req.getTransactionCode())) {
                String code = "%" + req.getTransactionCode().trim().toLowerCase() + "%";
                sql.append(" AND LOWER(TRANSACTION_CODE) LIKE ?");
                params.add(code);
            }

            if (StringUtils.hasText(req.getAccountNo())) {
                String acc = "%" + req.getAccountNo().trim().toLowerCase() + "%";
                sql.append(" AND LOWER(ACCOUNT_NO) LIKE ?");
                params.add(acc);
            }

            if (StringUtils.hasText(req.getStatus())) {
                sql.append(" AND UPPER(STATUS) = ?");
                params.add(req.getStatus().trim().toUpperCase());
            }

            if (StringUtils.hasText(req.getReferenceNo())) {
                String ref = "%" + req.getReferenceNo().trim().toLowerCase() + "%";
                sql.append(" AND LOWER(REFERENCE_NO) LIKE ?");
                params.add(ref);
            }

            if (req.getFromDate() != null) {
                sql.append(" AND CREATED_AT >= ?");
                params.add(Timestamp.valueOf(req.getFromDate().atStartOfDay()));
            }

            if (req.getToDate() != null) {
                sql.append(" AND CREATED_AT <= ?");
                params.add(Timestamp.valueOf(req.getToDate().atTime(LocalTime.MAX)));
            }

            if (req.getMinAmount() != null) {
                sql.append(" AND AMOUNT >= ?");
                params.add(req.getMinAmount());
            }

            if (req.getMaxAmount() != null) {
                sql.append(" AND AMOUNT <= ?");
                params.add(req.getMaxAmount());
            }
        }

        if (!countOnly) {
            sql.append(" ORDER BY ID DESC");
        }

        return new SqlQueryBundle(sql.toString(), params.toArray());
    }

    public record SqlQueryBundle(String sql, Object[] params) {}
}

