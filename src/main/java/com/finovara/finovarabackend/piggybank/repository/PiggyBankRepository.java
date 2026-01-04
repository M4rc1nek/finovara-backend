package com.finovara.finovarabackend.piggybank.repository;

import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PiggyBankRepository extends JpaRepository<PiggyBank, Long> {
    Optional<PiggyBank> findByIdAndUserAssignedEmail(Long id, String email);

    List<PiggyBank> findAllByUserAssignedEmail(String email);

    boolean existsByName(String Name);

    // UsernameAlreadyExistsException zamienilem na NameAlreadyExistsException - wykorzystuje w UserService i PiggyBankService, czy tak moze byc?

}
