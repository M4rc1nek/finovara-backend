package com.finovara.financeservice.sharedaccount.piggybank.repository;

import com.finovara.financeservice.sharedaccount.piggybank.model.SharedPiggyBank;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SharedPiggyBankRepository extends JpaRepository<SharedPiggyBank, Long> {

    @Query("SELECT pb FROM SharedPiggyBank pb WHERE pb.id = :piggyBankId AND (pb.ownerId = :userId OR pb.memberId = :userId)")
    Optional<SharedPiggyBank> findByIdAndOwnerIdOrMemberId(Long piggyBankId, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pb FROM SharedPiggyBank pb WHERE pb.id = :piggyBankId AND (pb.ownerId = :userId OR pb.memberId = :userId)")
    Optional<SharedPiggyBank> findByIdAndOwnerIdOrMemberIdForUpdate(Long piggyBankId, Long userId);

    @Query("SELECT pb FROM SharedPiggyBank pb WHERE pb.ownerId = :userId OR pb.memberId = :userId ORDER BY pb.createdAt DESC, pb.id DESC")
    List<SharedPiggyBank> findAllByUserId(Long userId);

    @Query("SELECT e FROM SharedPiggyBank e WHERE e.ownerId = :ownerId AND e.memberId = :memberId")
    List<SharedPiggyBank> findAllByOwnerIdAndMemberId(Long ownerId, Long memberId);

    @Query("SELECT LOWER(pb.name) FROM SharedPiggyBank pb WHERE pb.ownerId = :userId OR pb.memberId = :userId")
    List<String> findLowerCaseNamesByUserId(Long userId);

    @Query("""
                SELECT COUNT(p) > 0
                FROM SharedPiggyBank p
                WHERE LOWER(p.name) = LOWER(:name)
                  AND (p.ownerId = :userId OR p.memberId = :userId)
                  AND p.id <> :excludedId
            """)
    boolean existsByNameIgnoreCaseExcludingId(Long userId, String name, Long excludedId);
}