package com.finovara.financeservice.util.piggybank;

import com.finovara.financeservice.piggybank.model.PiggyBank;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PiggyBankCheckGoalCompletion {

    public static boolean isGoalCompleted(PiggyBank piggyBank) {
        if (piggyBank.getGoalAmount() == null) {
            return false;
        }
        return piggyBank.getAmount().compareTo(piggyBank.getGoalAmount()) >= 0;
    }

}
