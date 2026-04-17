package com.finovara.finovarabackend.user.service;

import com.finovara.finovarabackend.accountactivity.security.login.activities.model.LoginActivityStatus;
import com.finovara.finovarabackend.accountactivity.security.login.activities.service.LoginActivityService;
import com.finovara.finovarabackend.exception.conflict.NameAlreadyExistsException;
import com.finovara.finovarabackend.exception.unauthorized.WrongPasswordException;
import com.finovara.finovarabackend.security.service.JwtService;
import com.finovara.finovarabackend.user.dto.UserLoginDto;
import com.finovara.finovarabackend.user.dto.UserRegisterDto;
import com.finovara.finovarabackend.user.exception.conflict.EmailAlreadyExistsException;
import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.usersetting.factory.SettingsFactory;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

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

    public UserRegisterDto registerUser(UserRegisterDto userRegisterDto) {

        if (userRepository.existsByUsername(userRegisterDto.username())) {
            log.info("User cannot register. Username is already taken. Username: {}", userRegisterDto.username());
            throw new NameAlreadyExistsException("Username is already taken");
        }

        if (userRepository.existsByEmail(userRegisterDto.email())) {
            log.info("User cannot register. Email is already taken.  UserEmail: {}", userRegisterDto.email());
            throw new EmailAlreadyExistsException("Email is already taken");
        }

        User user = User.builder()
                .username(userRegisterDto.username())
                .email(userRegisterDto.email())
                .password(passwordEncoder.encode(userRegisterDto.password()))
                .createdAt(LocalDateTime.now())
                .build();
        user.setExpenseSettings(settingsFactory.createDefaultExpenseSettings(user));
        user.setRevenueSettings(settingsFactory.createDefaultRevenueSettings(user));
        user.setNotificationEmailSettings(settingsFactory.createDefaultNotificationSettings(user));
        User savedUser = userRepository.save(user);

        String jwtToken = jwtService.generateToken(
                new org.springframework.security.core.userdetails.User(
                        savedUser.getEmail(),
                        savedUser.getPassword(),
                        List.of()
                )
        );

        return new UserRegisterDto(user.getId(), savedUser.getUsername(), null, savedUser.getEmail(), jwtToken);

    }

    public UserLoginDto loginUser(String email, String rawPassword, HttpServletRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, rawPassword));
            loginActivityService.createLoginActivity(email, LoginActivityStatus.successful, request);
        } catch (AuthenticationException e) {
            loginActivityService.createLoginActivity(email, LoginActivityStatus.unsuccessful, request);
            throw new WrongPasswordException("Incorrect email or password");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                List.of()
        );
        String jwtToken = jwtService.generateToken(userDetails);
        return new UserLoginDto(user.getId(), user.getUsername(), email, null, jwtToken);
    }

}
