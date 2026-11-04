package com.training.PaymentHub.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "PMH_GROUP_CATEGORY")
public class GroupCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pmh_group_category_seq_gen")
    @SequenceGenerator(
        name = "pmh_group_category_seq_gen",
        sequenceName = "PMH_GROUP_CATEGORY_SEQ",
        allocationSize = 1
    )
    @Column(name = "ID")
    private Long id;

    @Column(name = "PARAM_NAME", nullable = false, length = 255)
    private String paramName;

    @Column(name = "PARAM_VALUE", nullable = false, length = 255)
    private String paramValue;

    @Column(name = "PARAM_TYPE", nullable = false, length = 255)
    private String paramType;

    @Column(name = "DESCRIPTION", length = 4000)
    private String description;

    @Column(name = "COMPONENT_CODE", nullable = false, length = 255)
    private String componentCode;

    @Column(name = "STATUS", nullable = false)
    private Integer status;

    @Column(name = "IS_ACTIVE", nullable = false)
    private Integer isActive;

    @Column(name = "IS_DISPLAY", nullable = false)
    private Integer isDisplay;

    @Column(name = "NEW_DATA", length = 4000)
    private String newData;

    @Column(name = "EFFECTIVE_DATE", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "END_EFFECTIVE_DATE")
    private LocalDate endEffectiveDate;
}
