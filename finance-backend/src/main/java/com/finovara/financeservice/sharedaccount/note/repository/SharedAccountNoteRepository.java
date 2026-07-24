package com.finovara.financeservice.sharedaccount.note.repository;

import com.finovara.financeservice.sharedaccount.note.model.SharedAccountNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SharedAccountNoteRepository extends JpaRepository<SharedAccountNote, Long> {

    @Query("SELECT n FROM SharedAccountNote n WHERE n.ownerId = :userId OR n.memberId = :userId")
    List<SharedAccountNote> findAllByOwnerIdOrMemberId(Long userId);

    @Query("SELECT n FROM SharedAccountNote n WHERE n.id = :id AND (n.ownerId = :userId OR n.memberId = :userId)")
    Optional<SharedAccountNote> findByIdAndOwnerIdOrMemberId(Long id, Long userId);

    @Modifying
    @Query("DELETE FROM SharedAccountNote sa WHERE sa.ownerId = :ownerId AND sa.memberId = :memberId")
    void deleteByOwnerIdAndMemberId(Long ownerId, Long memberId);
}