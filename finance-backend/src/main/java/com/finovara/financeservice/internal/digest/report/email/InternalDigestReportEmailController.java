package com.finovara.financeservice.internal.digest.email;

import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/internal/digest/email")
@RequiredArgsConstructor
public class InternalDigestEmailController {

    private final InternalDigestEmailService internalDigestEmailService;

    @GetMapping("/expenses/sum")
    public ResponseEntity<BigDecimal> sumExpenses(@RequestParam Long userId) {
        return ResponseEntity.ok(internalDigestEmailService.sumExpenses(userId));
    }

    @GetMapping("/expenses/highest")
    public ResponseEntity<BigDecimal> getHighestExpense(@RequestParam Long userId) {
        return ResponseEntity.ok(internalDigestEmailService.getHighestExpense(userId));
    }

    @GetMapping("/revenues/sum")
    public ResponseEntity<BigDecimal> sumRevenues(@RequestParam Long userId) {
        return ResponseEntity.ok(internalDigestEmailService.sumRevenues(userId));
    }

    @GetMapping("/revenues/highest")
    public ResponseEntity<BigDecimal> getHighestRevenue(@RequestParam Long userId) {
        return ResponseEntity.ok(internalDigestEmailService.getHighestRevenue(userId));
    }

    @GetMapping("/budget/remaining")
    public ResponseEntity<BigDecimal> getRemainingMonthlyBudget(@RequestParam Long userId) {
        return ResponseEntity.ok(internalDigestEmailService.getRemainingMonthlyBudget(userId));
    }

    @GetMapping("/days-without-expense")
    public ResponseEntity<Integer> daysWithoutExpense(@RequestParam Long userId) {
        return ResponseEntity.ok(internalDigestEmailService.daysWithoutExpense(userId));
    }

    @GetMapping("/saved-money")
    public ResponseEntity<BigDecimal> savedMoney(@RequestParam Long userId) {
        return ResponseEntity.ok(internalDigestEmailService.savedMoney(userId));
    }

    @GetMapping("/recurring/upcoming")
    public ResponseEntity<List<RecurringSettings>> getUpcomingRecurringPayments(@RequestParam Long userId) {
        return ResponseEntity.ok(internalDigestEmailService.getUpcomingRecurringPayments(userId));
    }
}