package com.training.PaymentHub.repository;

import com.training.PaymentHub.model.entity.ExportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExportJobRepository extends JpaRepository<ExportJob, Long> {

    Optional<ExportJob> findByJobId(String jobId);

    List<ExportJob> findByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * Atomic Conditional Update chống xử lý trùng lặp (Idempotency) giữa nhiều Worker.
     * Chỉ chuyển sang PROCESSING nếu trạng thái hiện tại đang là PENDING.
     */
    @Modifying
    @Transactional
    @Query("UPDATE ExportJob e SET e.status = :newStatus, e.progress = :progress, e.updatedAt = :now " +
           "WHERE e.jobId = :jobId AND e.status = :expectedStatus")
    int claimJobAtomically(
            @Param("jobId") String jobId,
            @Param("expectedStatus") String expectedStatus,
            @Param("newStatus") String newStatus,
            @Param("progress") Integer progress,
            @Param("now") LocalDateTime now
    );
}

