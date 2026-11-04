package com.training.PaymentHub.model.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class GroupCategorySearchRequest {
    private String paramName;
    private String paramValue;
    private String paramType;
    private String componentCode;
    private Integer status;
    private Integer isActive;

    private List<String> paramNames;
    private List<String> paramValues;
    private List<String> paramTypes;
    private List<String> componentCodes;
    private List<Integer> statuses;
    private List<Integer> isActives;
    private int page = 0;
    private int size = 10;
}