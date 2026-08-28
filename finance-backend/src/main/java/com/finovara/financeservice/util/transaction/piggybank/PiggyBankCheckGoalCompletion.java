package com.finovara.financeservice.util.transaction.piggybank;

import com.finovara.financeservice.piggybank.model.PiggyBank;
import com.finovara.financeservice.sharedaccount.piggybank.model.SharedPiggyBank;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PiggyBankCheckGoalCompletion {

    public static boolean isGoalCompleted(PiggyBank piggyBank) {
        if (piggyBank.getGoalAmount() == null) {
            return false;
        }
        return piggyBank.getAmount().compareTo(piggyBank.getGoalAmount()) >= 0;
    }

    public static boolean isSharedPiggyBankGoalCompleted(SharedPiggyBank piggyBank) {
        if (piggyBank.getGoalAmount() == null) {
            return false;
        }
        return piggyBank.getAmount().compareTo(piggyBank.getGoalAmount()) >= 0;
    }
}
