package com.finovara.authservice.user.service;

import com.finovara.authservice.exception.conflict.LocalPasswordNotSetException;
import com.finovara.authservice.exception.unauthorized.InvalidCredentialsException;
import com.finovara.authservice.riskverification.service.RiskGuardService;
import com.finovara.authservice.security.jwt.JwtService;
import com.finovara.authservice.settings.factory.SettingsFactory;
import com.finovara.authservice.user.dto.UserLoginDto;
import com.finovara.authservice.user.dto.UserRegisterDto;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.authservice.util.email.EmailDomainValidator;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.contracts.mainaccount.activity.event.secure.login.activity.LoginActivityEvent;
import com.finovara.contracts.mainaccount.activity.model.LoginActivityStatus;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.securitymonitoring.model.RiskTriggerType;
import com.finovara.contracts.user.event.account.UserCreatedEvent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Paths;
import java.time.LocalDateTime;

import static com.finovara.contracts.user.clientdata.browser.UserBrowser.getBrowser;
import static com.finovara.contracts.user.clientdata.ip.ClientIp.getClientIpAddress;
import static com.finovara.contracts.user.clientdata.location.UserLocation.getLocationFromIp;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SettingsFactory settingsFactory;
    private final OutboxService outboxService;
    private final EmailDomainValidator emailDomainValidator;
    private final RiskGuardService riskGuardService;
    private final TransactionTemplate transactionTemplate;

    @Value("${application.upload.profile-images-default-directory}")
    private String profileImagesDefaultDirectory;

    public UserRegisterDto registerUser(UserRegisterDto dto) {
        if (userRepository.existsByUsernameIgnoreCase(dto.username())) {
            throw new EntityAlreadyExistsException("Username is already taken");
        }
        if (userRepository.existsByEmailIgnoreCase(dto.email())) {
            throw new EntityAlreadyExistsException("Email is already taken");
        }

        emailDomainValidator.validateDomainHasMxRecord(dto.email());

        String encodedPassword = passwordEncoder.encode(dto.password());
        String defaultImagePath = Paths.get(profileImagesDefaultDirectory).resolve("UserProf.png").toString();

        User savedUser = persistNewUser(dto, encodedPassword, defaultImagePath);

        String jwtToken = jwtService.generateToken(savedUser);
        return new UserRegisterDto(savedUser.getId(), savedUser.getUsername(), null,
                savedUser.getProfileImageUrl(), savedUser.getEmail(), jwtToken);
    }

    public UserLoginDto loginUser(String email, String rawPassword, String sourceEventId, HttpServletRequest request) {
        User user = userRepository.findUserByEmail(email).orElse(null);

        if (user == null) {
            throw new InvalidCredentialsException("Incorrect email or password");
        }

        if (!user.isPasswordSet()) {
            publishLoginActivity(user.getId(), LoginActivityStatus.UNSUCCESSFUL, request);
            throw new LocalPasswordNotSetException("Local password is not set for this account. Use Google login or set a local password first.");
        }

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            publishLoginActivity(user.getId(), LoginActivityStatus.UNSUCCESSFUL, request);
            throw new InvalidCredentialsException("Incorrect email or password");
        }

        riskGuardService.guard(user.getId(), RiskTriggerType.LOGIN, user.getEmail(), sourceEventId, request);

        publishLoginActivity(user.getId(), LoginActivityStatus.SUCCESSFUL, request);
        String jwtToken = jwtService.generateToken(user);

        return new UserLoginDto(user.getId(), user.getUsername(), user.getEmail(), null, user.getProfileImageUrl(), jwtToken, sourceEventId);
    }

    private User persistNewUser(UserRegisterDto dto, String encodedPassword, String defaultImagePath) {
        try {
            return transactionTemplate.execute(status -> {
                User user = User.builder()
                        .username(dto.username())
                        .email(dto.email())
                        .password(encodedPassword)
                        .passwordSet(true)
                        .profileImagePath(defaultImagePath)
                        .createdAt(LocalDateTime.now())
                        .build();

                user.setAccountSettings(settingsFactory.createDefaultAccountSettings());
                user.setSecuritySettings(settingsFactory.createDefaultSecuritySettings());

                userRepository.saveAndFlush(user);

                outboxService.save("User", user.getId().toString(), "user.created",
                        new UserCreatedEvent(user.getId(), user.getUsername(), user.getEmail(), user.getCreatedAt()));
                return user;
            });
        } catch (DataIntegrityViolationException exception) {
            throw new EntityAlreadyExistsException("Username or email is already taken");
        }
    }

    private void publishLoginActivity(Long userId, LoginActivityStatus status, HttpServletRequest request) {
        String ipAddress = getClientIpAddress(request);
        outboxService.save("User", userId.toString(), "user.logged-in",
                new LoginActivityEvent(userId, status, getBrowser(request), ipAddress, getLocationFromIp(ipAddress), LocalDateTime.now()));
    }
}