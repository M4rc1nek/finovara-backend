package com.finovara.securitymonitoring.riskengine.model;

import com.finovara.securitymonitoring.accountchange.model.AccountChangeProfile;
import com.finovara.securitymonitoring.login.model.LoginProfile;
import com.finovara.securitymonitoring.transaction.model.TransactionProfile;

import java.math.BigDecimal;
public record RiskContext(
        Long userId,
        RiskTriggerType triggerType,
        BigDecimal amount,
        Object category,
        String ipAddress,
        String location,
        String browser,
        TransactionProfile transactionProfile,
        LoginProfile loginProfile,
        AccountChangeProfile accountChangeProfile
) {}