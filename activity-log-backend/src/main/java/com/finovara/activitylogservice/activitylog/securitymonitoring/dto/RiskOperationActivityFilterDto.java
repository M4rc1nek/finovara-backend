package com.finovara.activitylogservice.activitylog.securitymonitoring.dto;

import com.finovara.contracts.securitymonitoring.model.RiskAction;
import com.finovara.contracts.securitymonitoring.model.RiskTriggerType;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record RiskOperationActivityFilterDto(
       String sourceEventId,
       RiskTriggerType triggerType,
       Integer score,
       RiskAction action,
       @DateTimeFormat(pattern = "dd.MM.yyyy") LocalDate operationDate,
       @DateTimeFormat(pattern = "dd.MM.yyyy") LocalDate createdAt
) {}
