package com.finovara.activitylogservice.activitylog.mainaccount.secure.login.archive.repository;

import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.archive.model.LoginActivityArchive;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoginActivityArchiveRepository extends JpaRepository<LoginActivityArchive, Long> {

    List<LoginActivityArchive> findByUserId(Long userId, Pageable pageable);

    List<LoginActivityArchive> findAllByUserId(Long userId);

}
