package com.finovara.finovarabackend.revenue.repository;

import com.finovara.finovarabackend.revenue.model.Revenue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RevenueRepository extends JpaRepository<Revenue, Long> {

    Optional<Revenue> findByIdAndUserAssignedId(Long revenueId, Long userId);

    List<Revenue> findAllByUserAssignedId(Long userId);

    List<Revenue> findAllByUserAssignedIdAndCreatedAtBetween(Long userId, LocalDate from, LocalDate to);

}
