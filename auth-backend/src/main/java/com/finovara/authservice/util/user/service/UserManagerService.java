package com.finovara.authservice.util.user.service;

import com.finovara.authservice.util.user.UserDataProjection;
import com.finovara.contracts.user.authorization.dto.UserDataDto;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.contracts.user.authorization.dto.UserDataResponse;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserManagerService {

    private final UserRepository userRepository;

    public UserDataResponse getUserData(Long userId) {
        UserDataProjection data = userRepository.findUserDataById(userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("User not found"));
        return new UserDataResponse(userId, Optional.ofNullable(data.getUsername()), Optional.ofNullable(data.getEmail()));
    }

    public UserDataDto getUserDataWithProfileImg(Long userId) {
        return userRepository.findBasicInfoByIds(List.of(userId))
                .stream()
                .findFirst()
                .orElseThrow(() -> new RequestedEntityNotFoundException("User not found"));
    }

    public List<Long> getAllUserIds() {
        return userRepository.findAllUserIds();
    }

    public User getUserByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
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
