package com.finovara.finovarabackend.usersetting.account.repository;

import com.finovara.finovarabackend.usersetting.account.model.AccountSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<AccountSettings, Long> {

}
