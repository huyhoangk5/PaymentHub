package com.training.PaymentHub.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "PMH_USERS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pmh_users_seq_gen")
    @SequenceGenerator(name = "pmh_users_seq_gen", sequenceName = "PMH_USERS_SEQ", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "USERNAME", nullable = false, length = 255)
    private String username;

    @Column(name = "EMAIL", nullable = false, length = 255)
    private String email;

    @Column(name = "PASSWORD", nullable = false, length = 255)
    private String password;

    @Column(name = "ENABLED", length = 255)
    private String enabled;

    @Column(name = "FULL_NAME", length = 255)
    private String fullName;

    @Column(name = "LOCKOUT_UNTIL")
    private LocalDateTime lockoutUntil;

    @Column(name = "REFRESH_TOKEN")
    private String refreshToken;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "PMH_USER_ROLES",
        joinColumns = @JoinColumn(name = "USER_ID", referencedColumnName = "ID"),
        inverseJoinColumns = @JoinColumn(name = "ROLE_ID", referencedColumnName = "ID")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();
}