package com.finovara.authservice.util.user.service;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserManagerService {

    private final UserRepository userRepository;

    public User getUserByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("User not found"));
    }

    public  User getUserByEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RequestedEntityNotFoundException("User not found"));
    }

    public String getUsernameByIdOrThrow(Long userId){
        return userRepository.findUsernameById(userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("User not found"));
    }
}
