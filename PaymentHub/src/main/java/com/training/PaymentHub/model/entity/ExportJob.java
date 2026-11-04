package com.training.PaymentHub.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "EXPORT_JOB")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "export_job_seq_gen")
    @SequenceGenerator(name = "export_job_seq_gen", sequenceName = "EXPORT_JOB_SEQ", allocationSize = 1)
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "JOB_ID", nullable = false, unique = true, length = 255)
    private String jobId;

    @Column(name = "USER_ID", length = 255)
    private String userId;

    @Column(name = "STATUS", length = 50)
    private String status;

    @Column(name = "PROGRESS")
    @Builder.Default
    private Integer progress = 0;

    @Column(name = "FILE_URL", length = 1000)
    private String fileUrl;

    @Column(name = "TOTAL_RECORDS")
    private Long totalRecords;

    @Column(name = "ERROR_MESSAGE", length = 1000)
    private String errorMessage;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.progress == null) {
            this.progress = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

