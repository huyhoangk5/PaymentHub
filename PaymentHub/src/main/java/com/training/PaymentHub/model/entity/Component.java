package com.training.PaymentHub.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "PMH_COMPONENTS")
public class Component {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pmh_components_seq_gen")
    @SequenceGenerator(
            name = "pmh_components_seq_gen",
            sequenceName = "PMH_COMPONENTS_SEQ",
            allocationSize = 1
    )
    @Column(name = "ID")
    private Long id;

    @Column(name = "COMPONENT_CODE", nullable = false)
    private String componentCode;

    @Column(name = "COMPONENT_NAME", nullable = false)
    private String componentName;

    @Column(name = "MESSAGE_TYPE")
    private String messageType;

    @Column(name = "CONNECTION_METHOD")
    private String connectionMethod;

    @Column(name = "CHECK_TOKEN")
    private Boolean checkToken;

    @Column(name = "STATUS", nullable = false)
    private int status;

    @Column(name = "IS_ACTIVE", nullable = false)
    private int isActive;

    @Column(name = "IS_DISPLAY", nullable = false)
    private int isDisplay;

    @Column(name = "NEW_DATA")
    private String newData;

    @Column(name = "EFFECTIVE_DATE", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "END_EFFECTIVE_DATE")
    private LocalDate endEffectiveDate;
}
