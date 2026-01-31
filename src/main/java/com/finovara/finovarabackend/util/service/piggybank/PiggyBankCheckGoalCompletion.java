package com.finovara.finovarabackend.util.service.piggybank;

import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import org.springframework.stereotype.Service;

@Service
public class PiggyBankCheckGoalCompletion {

    public boolean isGoalCompleted(PiggyBank piggyBank) {
        if (piggyBank.getGoalAmount() == null) {
            return false;
        }
        return piggyBank.getAmount().compareTo(piggyBank.getGoalAmount()) >= 0;
    }

}
