package com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.archive.repository;

import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.archive.model.AccountChangeArchive;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountChangeArchiveRepository extends JpaRepository<AccountChangeArchive, Long> {

    List<AccountChangeArchive> findByUserId(Long userId, Pageable pageable);

    void deleteByUserId(Long userId);
}