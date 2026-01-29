package com.finovara.finovarabackend.usersettings.finance.roundup.dto;

public record RoundUpDto(
        Long piggyBankId,
        Boolean roundUpActive
) {
}

//To oznacza, że:
//Aby ustawić Round-up jako aktywny lub nieaktywny, FE musi wysłać PUT na:
// /api/finance-roundup/{expenseId}/{piggyBankId}