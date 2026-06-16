package com.finovara.financeservice.settings.piggybank.repository;

import com.finovara.financeservice.settings.piggybank.model.PiggyBankSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PiggyBankSettingsRepository extends JpaRepository<PiggyBankSettings, Long> {

}
