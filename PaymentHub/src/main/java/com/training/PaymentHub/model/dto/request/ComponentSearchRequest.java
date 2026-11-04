package com.training.PaymentHub.model.dto.request;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ComponentSearchRequest {
    private List<String> componentCodes;
    private List<String> componentNames;
    private List<Integer> statuses;
    private List<Integer> isActives;


    private String componentCode;
    private String componentName;
    private String checkToken;
    private Integer status;
    private Integer isActive;
    private String connectionMethod;
    private Integer isDisplay;
    private LocalDate fromDate;
    private LocalDate toDate;

    private int page = 0;
    private int size = 10;
}
