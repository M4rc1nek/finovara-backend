package com.finovara.finovarabackend.usersetting.piggybank.repository;

import com.finovara.finovarabackend.usersetting.piggybank.model.PiggyBankSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PiggyBankSettingsRepository extends JpaRepository<PiggyBankSettings, Long> {

}
