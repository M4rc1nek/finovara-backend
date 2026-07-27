package com.finovara.financeservice.piggybank.goalplanner.repository;

import com.finovara.financeservice.piggybank.goalplanner.model.GoalPlanner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GoalPlannerRepository extends JpaRepository<GoalPlanner, Long> {

    @Query("SELECT gp FROM GoalPlanner gp WHERE gp.piggyBankAssigned.id = :piggyBankId AND gp.userId = :userId")
    Optional<GoalPlanner> findByPiggyBankIdAndUserId(Long piggyBankId, Long userId);
}
