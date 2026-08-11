package com.finovara.authservice.util.user.service;

import com.finovara.authservice.user.dto.UserDataDto;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.contracts.authorization.dto.UserDataResponse;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserManagerService {

    private final UserRepository userRepository;

    public UserDataResponse getUserData(Long userId){
        Optional<String> email = userRepository.findEmailById(userId);
        Optional<String> username = userRepository.findUsernameById(userId);
        return new UserDataResponse(userId, username, email);
    }

    public UserDataDto getUserDataWithProfileImg(Long userId) {
        return userRepository.findBasicInfoByIds(List.of(userId))
                .stream()
                .findFirst()
                .orElseThrow(() -> new RequestedEntityNotFoundException("User not found"));
    }

    public User getUserByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("User not found"));
    }

    public User getUserByEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RequestedEntityNotFoundException("User not found"));
    }

    public String getUsernameByIdOrThrow(Long userId) {
        return userRepository.findUsernameById(userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("User not found"));
    }

    public String getUserEmailById(Long userId) {
        return userRepository.findEmailById(userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("User not found"));
    }
}
