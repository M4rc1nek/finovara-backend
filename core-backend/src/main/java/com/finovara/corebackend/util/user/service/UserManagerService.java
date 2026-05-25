package com.finovara.corebackend.util.user.service;

import com.finovara.corebackend.user.exception.notfound.UserNotFoundException;
import com.finovara.corebackend.user.model.User;
import com.finovara.corebackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserManagerService {

    private final UserRepository userRepository;

    public User getUserByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    public  User getUserByEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }
}
