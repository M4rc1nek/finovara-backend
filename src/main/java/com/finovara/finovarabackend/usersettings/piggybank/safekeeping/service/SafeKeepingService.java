package com.finovara.finovarabackend.usersettings.piggybank.safekeeping.service;

import com.finovara.finovarabackend.piggybank.repository.PiggyBankRepository;
import com.finovara.finovarabackend.piggybank.service.PiggyBankService;
import com.finovara.finovarabackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SafeKeepingService {
    private final UserRepository userRepository;
    private final PiggyBankRepository piggyBankRepository;
    private final PiggyBankService piggyBankService;


    public void holdFunds(String email){

    }




}
