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
       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate operationDate,
       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAt
) {}
