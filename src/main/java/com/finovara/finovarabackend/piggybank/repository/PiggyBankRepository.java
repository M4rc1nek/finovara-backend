package com.finovara.finovarabackend.piggybank.repository;

import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PiggyBankRepository extends JpaRepository<PiggyBank, Long> {
    @Query("SELECT pb FROM PiggyBank pb  WHERE pb.id = :piggyBankId AND pb.userAssigned.id = :userId")
    Optional<PiggyBank> findByIdAndUserAssignedId(@Param("piggyBankId") Long piggyBankId, @Param("userId") Long userId);

    @Query("SELECT pb FROM PiggyBank pb  WHERE pb.id = :piggyBankId AND pb.userAssigned.email = :email")
    Optional<PiggyBank> findByIdAndUserAssignedEmail(@Param("piggyBankId") Long piggyBankId, @Param("email") String email);

    @Query("SELECT pb FROM PiggyBank pb WHERE pb.userAssigned.id = :userId")
    List<PiggyBank> findAllByUserAssignedId(@Param("userId") Long userId);

    @Query("SELECT COUNT(pb) FROM PiggyBank pb WHERE pb.userAssigned.id = :userId")
    long countPiggyBanksByUserId(@Param("userId") Long userId);


    boolean existsByNameAndUserAssignedId(String Name, Long userId);
}
