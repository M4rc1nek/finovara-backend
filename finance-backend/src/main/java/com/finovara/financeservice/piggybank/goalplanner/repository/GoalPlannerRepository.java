package com.finovara.financeservice.piggybank.goalplanner.repository;

import com.finovara.financeservice.piggybank.goalplanner.model.GoalPlanner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GoalPlannerRepository extends JpaRepository<GoalPlanner, Long> {
}
