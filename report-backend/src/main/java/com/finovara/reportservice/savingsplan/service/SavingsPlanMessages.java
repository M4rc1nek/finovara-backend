package com.finovara.reportservice.savingsplan.service;

public enum SavingsPlanMessages {
    GOAL_REACHED("The goal is already reached."),
    GOAL_WITHOUT_CUTS("The goal can be reached with the current plan."),
    GOAL_NOT_REACHED("The goal cannot be reached by the target date."),
    LOWER_GOAL("Keep the deadline and lower the goal."),
    LATER_DATE("Keep the current monthly amount and extend the deadline.");

    private final String text;

    SavingsPlanMessages(String text) {
        this.text = text;
    }

    public String text() {
        return text;
    }
}
