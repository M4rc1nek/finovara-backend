package com.finovara.authservice.sharedaccount.repository;

import com.finovara.authservice.sharedaccount.model.SharedAccountMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SharedAccountMemberRepository extends JpaRepository<SharedAccountMember, Long> {

    boolean existsByUserId(Long userId);

    Optional<SharedAccountMember> findByUserId(Long userId);

    @Query("""
            SELECT m FROM SharedAccountMember m
            WHERE m.sharedAccount.id = :accountId
            """)
    List<SharedAccountMember> findMembersByAccountId(Long accountId);

}
