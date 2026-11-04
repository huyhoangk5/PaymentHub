package com.training.PaymentHub.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "PMH_ROLES")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pmh_roles_seq_gen")
    @SequenceGenerator(name = "pmh_roles_seq_gen", sequenceName = "PMH_ROLES_SEQ", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "ROLE_CODE", nullable = false, length = 255)
    private String roleCode;

    @Column(name = "ROLE_NAME", nullable = false, length = 255)
    private String roleName;

    @Column(name = "DESCRIPTION", length = 255)
    private String description;

    // Tự động sinh bảng trung gian PMH_ROLE_PERMISSIONS
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "PMH_ROLE_PERMISSIONS",
        joinColumns = @JoinColumn(name = "ROLE_ID", referencedColumnName = "ID"),
        inverseJoinColumns = @JoinColumn(name = "PERMISSION_ID", referencedColumnName = "ID")
    )
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();
}