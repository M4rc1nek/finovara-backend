package com.finovara.corebackend.usersetting.piggybank.repository;

import com.finovara.corebackend.usersetting.piggybank.model.PiggyBankSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PiggyBankSettingsRepository extends JpaRepository<PiggyBankSettings, Long> {

}
