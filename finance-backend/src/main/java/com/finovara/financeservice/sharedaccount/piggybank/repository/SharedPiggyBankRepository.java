package com.finovara.financeservice.sharedaccount.piggybank.repository;

import com.finovara.financeservice.sharedaccount.piggybank.model.SharedPiggyBank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SharedPiggyBankRepository extends JpaRepository<SharedPiggyBank, Long> {

    @Query("SELECT pb FROM SharedPiggyBank pb WHERE pb.id = :piggyBankId " +
            "AND (pb.ownerId = :userId OR pb.memberId = :userId)")
    Optional<SharedPiggyBank> findByIdAndUserId(Long piggyBankId, Long userId);

    @Query("SELECT pb FROM SharedPiggyBank pb WHERE pb.ownerId = :userId OR pb.memberId = :userId")
    List<SharedPiggyBank> findAllByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM SharedPiggyBank pb WHERE pb.ownerId = :ownerId AND pb.memberId = :memberId")
    void deleteByOwnerIdAndMemberId(Long ownerId, Long memberId);

    @Query("SELECT COUNT(pb) FROM SharedPiggyBank pb WHERE pb.ownerId = :userId OR pb.memberId = :userId")
    long countPiggyBanksByUserId(Long userId);

    @Query("""
                SELECT COUNT(p) > 0
                FROM SharedPiggyBank p
                WHERE LOWER(p.name) = LOWER(:name)
                  AND (p.ownerId = :userId OR p.memberId = :userId)
            """)
    boolean existsByNameIgnoreCase(Long userId, String name);

}