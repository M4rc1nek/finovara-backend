package com.finovara.finovarabackend.accountactivity.accountchange.archive.repository;

import com.finovara.finovarabackend.accountactivity.accountchange.archive.model.AccountChangeArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountChangeArchiveRepository extends JpaRepository<AccountChangeArchive, Long> {

    @Query("SELECT a FROM AccountChangeArchive a WHERE a.userAssigned.email = :email ORDER BY a.id DESC")
    List<AccountChangeArchive> findAllByUserAssignedEmailOrderByIdDesc(@Param("email") String email);
}
