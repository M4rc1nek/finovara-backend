package com.finovara.securitymonitoring.transaction.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "risk.transaction")
public class TransactionRiskProperties {

    private BigDecimal expenseHighAmount;
    private BigDecimal firstTransactionAmount;
    private BigDecimal revenueHighAmount;
    private BigDecimal revenueLowAmount;
    private BigDecimal piggyBankHighAmount;
    private BigDecimal piggyBankDiffAmount;

    private int expenseHighPoints;
    private int expenseRecordPoints;
    private int firstTransactionPoints;
    private int expenseCategoryPoints;
    private int revenueHighPoints;
    private int revenueLowPoints;
    private int piggyBankHighPoints;
    private int piggyBankDiffPoints;
}