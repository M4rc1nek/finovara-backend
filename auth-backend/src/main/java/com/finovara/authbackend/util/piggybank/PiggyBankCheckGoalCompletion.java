package com.finovara.authbackend.util.piggybank;

import com.finovara.authbackend.piggybank.model.PiggyBank;
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
