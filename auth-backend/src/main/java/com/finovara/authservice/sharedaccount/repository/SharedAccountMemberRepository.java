package com.finovara.authservice.sharedaccount.repository;

import com.finovara.authservice.sharedaccount.model.SharedAccountMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SharedAccountMemberRepository extends JpaRepository<SharedAccountMember, Long> {

    Optional<SharedAccountMember> findByUserId(Long userId);

    @Query("""
            SELECT m FROM SharedAccountMember m
            WHERE m.sharedAccount.id = :accountId
            """)
    List<SharedAccountMember> findMembersByAccountId(Long accountId);

    @Query("""
            SELECT m FROM SharedAccountMember m
            WHERE m.sharedAccount.id = (
                SELECT m2.sharedAccount.id FROM SharedAccountMember m2 WHERE m2.userId = :userId
            )
            """)
    List<SharedAccountMember> findMembersByUserId(Long userId);

    @Query("SELECT COUNT(m) > 0 FROM SharedAccountMember m WHERE m.userId IN :userIds")
    boolean existsByUserIdIn(List<Long> userIds);
}
