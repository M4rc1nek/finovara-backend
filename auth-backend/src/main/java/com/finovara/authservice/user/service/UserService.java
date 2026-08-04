package com.finovara.authservice.user.service;

import com.finovara.authservice.exception.conflict.LocalPasswordNotSetException;
import com.finovara.authservice.exception.unauthorized.InvalidCredentialsException;
import com.finovara.contracts.event.activity.secure.login.activity.LoginActivityEvent;
import com.finovara.contracts.event.user.UserCreatedEvent;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.contracts.model.activity.LoginActivityStatus;
import com.finovara.authservice.security.jwt.JwtService;
import com.finovara.authservice.user.dto.UserLoginDto;
import com.finovara.authservice.user.dto.UserRegisterDto;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.authservice.settings.factory.SettingsFactory;
import com.finovara.authservice.util.email.EmailDomainValidator;
import com.finovara.contracts.outbox.OutboxService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Paths;
import java.time.LocalDateTime;

import static com.finovara.contracts.clientdata.browser.UserBrowser.getBrowser;
import static com.finovara.contracts.clientdata.ip.ClientIp.getClientIpAddress;
import static com.finovara.contracts.clientdata.location.UserLocation.getLocationFromIp;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    private final SettingsFactory settingsFactory;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OutboxService outboxService;
    private final EmailDomainValidator emailDomainValidator;

    @Value("${application.upload.profile-images-default-directory}")
    private String profileImagesDefaultDirectory;

    @Transactional
    public UserRegisterDto registerUser(UserRegisterDto dto) {
        if (userRepository.existsByUsername(dto.username())) {
            throw new EntityAlreadyExistsException("Username is already taken");
        }

        if (userRepository.existsByEmail(dto.email())) {
            throw new EntityAlreadyExistsException("Email is already taken");
        }

        emailDomainValidator.validateDomainHasMxRecord(dto.email());

        String defaultImagePath = Paths.get(profileImagesDefaultDirectory).resolve("UserProf.png").toString();

        User user = User.builder()
                .username(dto.username())
                .email(dto.email())
                .password(passwordEncoder.encode(dto.password()))
                .passwordSet(true)
                .profileImagePath(defaultImagePath)
                .createdAt(LocalDateTime.now())
                .build();

        user.setAccountSettings(settingsFactory.createDefaultAccountSettings(user));
        user.setSecuritySettings(settingsFactory.createDefaultSecuritySettings(user));

        User savedUser = userRepository.save(user);
        String jwtToken = jwtService.generateToken(savedUser);
        String userProfileImage = savedUser.getProfileImageUrl();
        outboxService.save("User", savedUser.getId().toString(), "user.created", new UserCreatedEvent(savedUser.getId(), savedUser.getUsername(), savedUser.getEmail(), savedUser.getCreatedAt()));

        return new UserRegisterDto(savedUser.getId(), savedUser.getUsername(), null, userProfileImage, savedUser.getEmail(), jwtToken);
    }

    public UserLoginDto loginUser(String email, String rawPassword, HttpServletRequest request) {
        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null && !user.isPasswordSet()) {
            publishLoginActivity(user.getId(), LoginActivityStatus.UNSUCCESSFUL, request);
            throw new LocalPasswordNotSetException("Local password is not set for this account. Use Google login or set a local password first.");
        }

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, rawPassword));

            if (user == null) {
                throw new InvalidCredentialsException("Incorrect email or password");
            }

            publishLoginActivity(user.getId(), LoginActivityStatus.SUCCESSFUL, request);
            String jwtToken = jwtService.generateToken(user);
            String userProfileImage = user.getProfileImageUrl();

            return new UserLoginDto(user.getId(), user.getUsername(), user.getEmail(), null, userProfileImage, jwtToken);

        } catch (AuthenticationException exception) {
            if (user != null) {
                publishLoginActivity(user.getId(), LoginActivityStatus.UNSUCCESSFUL, request);
            }
            throw new InvalidCredentialsException("Incorrect email or password");
        }
    }

    private void publishLoginActivity(Long userId, LoginActivityStatus status, HttpServletRequest request) {
        String ipAddress = getClientIpAddress(request);
        kafkaTemplate.send("activity.login", new LoginActivityEvent(userId, status, getBrowser(request), ipAddress, getLocationFromIp(ipAddress), LocalDateTime.now()));
    }
}
