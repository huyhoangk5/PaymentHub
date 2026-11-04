package com.training.PaymentHub.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "PMH_PERMISSIONS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pmh_perm_seq_gen")
    @SequenceGenerator(name = "pmh_perm_seq_gen", sequenceName = "PMH_PERMISSIONS_SEQ", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "PERMISSION_CODE", nullable = false, unique = true, length = 100)
    private String code; // Ví dụ: COMPONENT_CREATE, COMPONENT_APPROVE, USER_VIEW

    @Column(name = "PERMISSION_NAME", nullable = false, length = 255)
    private String name;

    @Column(name = "MODULE", length = 100)
    private String module; // Ví dụ: COMPONENTS, AUDIT_LOG, USER_MANAGEMENT

    @Column(name = "DESCRIPTION", length = 500)
    private String description;
}