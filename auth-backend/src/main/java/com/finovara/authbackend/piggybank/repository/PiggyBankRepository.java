package com.finovara.authbackend.piggybank.repository;

import com.finovara.authbackend.piggybank.model.PiggyBank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PiggyBankRepository extends JpaRepository<PiggyBank, Long> {
    @Query("SELECT pb FROM PiggyBank pb WHERE pb.id = :piggyBankId AND pb.userId = :userId")
    Optional<PiggyBank> findByIdAndUserId(Long piggyBankId, Long userId);

    @Query("SELECT pb FROM PiggyBank pb WHERE pb.userId = :userId")
    List<PiggyBank> findAllByUserId(Long userId);

    @Query("SELECT COUNT(pb) FROM PiggyBank pb WHERE pb.userId = :userId")
    long countPiggyBanksByUserId(Long userId);

    @Query("""
                SELECT COUNT(p) > 0 
                FROM PiggyBank p 
                WHERE LOWER(p.name) = LOWER(:name) 
                  AND p.userId = :userId
            """)
    boolean existsByNameIgnoreCase(Long userId, String name);
}
