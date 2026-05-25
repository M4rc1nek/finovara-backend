package com.finovara.corebackend.util.confirmationpassword.service;

import com.finovara.corebackend.exception.unauthorized.WrongPasswordException;
import com.finovara.corebackend.user.model.User;
import com.finovara.corebackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import com.finovara.corebackend.util.user.service.UserManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordValidator {

    private final UserManagerService userManagerService;
    private final PasswordEncoder passwordEncoder;

    public void validatePassword(Long userId, ConfirmPasswordDto confirmPasswordDto) {
        User user = userManagerService.getUserByIdOrThrow(userId);

        if (!passwordEncoder.matches(confirmPasswordDto.password(), user.getPassword())) {
            throw new WrongPasswordException("Incorrect password");
        }

    }

}
