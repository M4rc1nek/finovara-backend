package com.finovara.finovarabackend.usersetting.account.service.emailpolicy;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.exception.unprocessablecontent.MissingRequirementException;
import com.finovara.finovarabackend.user.exception.conflict.EmailAlreadyExistsException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.usersetting.account.dto.ChangeEmailDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
@Component
@RequiredArgsConstructor
public class EmailValidator {

    private final UserRepository userRepository;

    public void validateEmail(User user, ChangeEmailDto changeEmailDto) {
        String newEmail = changeEmailDto.email();

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
