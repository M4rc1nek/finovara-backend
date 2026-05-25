package com.finovara.corebackend.usersetting.account.service.verification;

import com.finovara.corebackend.exception.badrequest.InvalidInputException;
import com.finovara.corebackend.exception.unprocessablecontent.MissingRequirementException;
import com.finovara.corebackend.user.exception.conflict.EmailAlreadyExistsException;
import com.finovara.corebackend.user.model.User;
import com.finovara.corebackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CredentialValidationService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public void validateNewPassword(String newPassword, String confirmNewPassword, String currentPasswordHash) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new MissingRequirementException("Password cannot be empty");
        }

        if (!newPassword.equals(confirmNewPassword)) {
            throw new MissingRequirementException("New passwords have to be the same");
        }

        if (newPassword.length() < 8) {
            throw new MissingRequirementException("Password too short");
        }

        if (passwordEncoder.matches(newPassword, currentPasswordHash)) {
            throw new MissingRequirementException("This password is already set");
        }
    }

    public void validateEmailChange(User user, String newEmail) {
        if (newEmail == null || newEmail.isBlank()) {
            throw new MissingRequirementException("Email cannot be empty");
        }

        if (newEmail.equalsIgnoreCase(user.getEmail())) {
            throw new InvalidInputException("New mail cannot be the same");
        }

        if (userRepository.existsByEmail(newEmail)) {
            throw new EmailAlreadyExistsException("Email already in use");
        }
    }
}

