package com.training.PaymentHub.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "PMH_USER_PERMISSIONS",
    uniqueConstraints = @UniqueConstraint(name = "UK_PMH_USER_PERM", columnNames = {"USER_ID", "PERMISSION_ID"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPermission {

    public static final String ACCESS_GRANT = "GRANT";
    public static final String ACCESS_REVOKE = "REVOKE";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pmh_user_perm_seq_gen")
    @SequenceGenerator(name = "pmh_user_perm_seq_gen", sequenceName = "PMH_USER_PERMISSIONS_SEQ", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "PERMISSION_ID", nullable = false)
    private Permission permission;

    @Column(name = "ACCESS_TYPE", nullable = false, length = 10)
    private String accessType;
}
