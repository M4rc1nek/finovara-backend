package com.finovara.finovarabackend.user.service;

import com.finovara.finovarabackend.accountactivity.secure.login.activity.model.LoginActivityStatus;
import com.finovara.finovarabackend.accountactivity.secure.login.activity.service.LoginActivityService;
import com.finovara.finovarabackend.exception.conflict.NameAlreadyExistsException;
import com.finovara.finovarabackend.exception.conflict.StateConflictException;
import com.finovara.finovarabackend.exception.unauthorized.WrongPasswordException;
import com.finovara.finovarabackend.security.jwt.JwtService;
import com.finovara.finovarabackend.user.dto.UserLoginDto;
import com.finovara.finovarabackend.user.dto.UserRegisterDto;
import com.finovara.finovarabackend.user.exception.conflict.EmailAlreadyExistsException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.usersetting.factory.SettingsFactory;
import com.finovara.finovarabackend.util.email.EmailDomainValidator;
import com.finovara.finovarabackend.util.profile.ProfileImageUrlBuilder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    private final SettingsFactory settingsFactory;
    private final LoginActivityService loginActivityService;
    private final EmailDomainValidator emailDomainValidator;

    public UserRegisterDto registerUser(UserRegisterDto dto) {
        if (userRepository.existsByUsername(dto.username())) {
            throw new NameAlreadyExistsException("Username is already taken");
        }

        if (userRepository.existsByEmail(dto.email())) {
            throw new EmailAlreadyExistsException("Email is already taken");
        }

        emailDomainValidator.validateDomainHasMxRecord(dto.email());

        User user = User.builder()
                .username(dto.username())
                .email(dto.email())
                .password(passwordEncoder.encode(dto.password()))
                .passwordSet(true)
                .createdAt(LocalDateTime.now())
                .build();

        user.setExpenseSettings(settingsFactory.createDefaultExpenseSettings(user));
        user.setRecurringSettings(settingsFactory.createDefaultRecurringSettings(user));
        user.setNotificationEmailSettings(settingsFactory.createDefaultNotificationSettings(user));
        user.setAccountSettings(settingsFactory.createDefaultAccountSettings(user));

        User savedUser = userRepository.save(user);

        String jwtToken = jwtService.generateToken(savedUser);
        return new UserRegisterDto(
                savedUser.getId(),
                savedUser.getUsername(),
                null,
                savedUser.getEmail(),
                jwtToken
        );
    }

    public UserLoginDto loginUser(String email, String rawPassword, HttpServletRequest request) {
        User userByEmail = userRepository.findByEmail(email).orElse(null);

        if (userByEmail != null && !userByEmail.isPasswordSet()) {
            loginActivityService.createLoginActivity(userByEmail.getId(), LoginActivityStatus.UNSUCCESSFUL, request);
            throw new StateConflictException("Local password is not set for this account. Use Google login or set a local password first.");
        }

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, rawPassword));

            if (userByEmail == null) {
                throw new WrongPasswordException("Incorrect email or password");
            }

            loginActivityService.createLoginActivity(userByEmail.getId(), LoginActivityStatus.SUCCESSFUL, request);

            String jwtToken = jwtService.generateToken(userByEmail);
            String userProfileImage = ProfileImageUrlBuilder.buildProfileImageUrl(userByEmail.getProfileImagePath());

            return new UserLoginDto(
                    userByEmail.getId(),
                    userByEmail.getUsername(),
                    userByEmail.getEmail(),
                    null,
                    userProfileImage,
                    jwtToken
            );

        } catch (AuthenticationException e) {
            if (userByEmail != null) {
                loginActivityService.createLoginActivity(userByEmail.getId(), LoginActivityStatus.UNSUCCESSFUL, request);
            }
            throw new WrongPasswordException("Incorrect email or password");
        }
    }
}
