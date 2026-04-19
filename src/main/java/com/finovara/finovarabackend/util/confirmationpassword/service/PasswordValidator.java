package com.finovara.finovarabackend.util.confirmationpassword.service;

import com.finovara.finovarabackend.exception.unauthorized.WrongPasswordException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordValidator {

    private final UserManagerService userManagerService;
    private final PasswordEncoder passwordEncoder;

    public void confirmPassword(Long userId, ConfirmPasswordDto confirmPasswordDto) {
        User user = userManagerService.getUserByIdOrThrow(userId);

        if (!passwordEncoder.matches(confirmPasswordDto.password(), user.getPassword())) {
            throw new WrongPasswordException("Incorrect password");
        }

    }

}
