package com.finovara.authservice.user.service;

import com.finovara.authservice.exception.conflict.LocalPasswordNotSetException;
import com.finovara.authservice.exception.unauthorized.InvalidCredentialsException;
import com.finovara.authservice.security.jwt.JwtService;
import com.finovara.authservice.settings.factory.SettingsFactory;
import com.finovara.authservice.settings.security.twofactor.service.TwoFactorLoginService;
import com.finovara.authservice.user.dto.UserLoginDto;
import com.finovara.authservice.user.dto.UserRegisterDto;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.authservice.util.email.EmailDomainValidator;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.contracts.mainaccount.activity.event.secure.login.activity.LoginActivityEvent;
import com.finovara.contracts.mainaccount.activity.model.LoginActivityStatus;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.user.event.account.UserCreatedEvent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Paths;
import java.time.LocalDateTime;

import static com.finovara.contracts.user.clientdata.browser.UserBrowser.getBrowser;
import static com.finovara.contracts.user.clientdata.ip.ClientIp.getClientIpAddress;
import static com.finovara.contracts.user.clientdata.location.UserLocation.getLocationFromIp;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SettingsFactory settingsFactory;
    private final OutboxService outboxService;
    private final EmailDomainValidator emailDomainValidator;
    private final TwoFactorLoginService twoFactorLoginService;

    @Value("${application.upload.profile-images-default-directory}")
    private String profileImagesDefaultDirectory;

    @Transactional
    public UserRegisterDto registerUser(UserRegisterDto dto) {
        if (userRepository.existsByUsernameIgnoreCase(dto.username())) {
            throw new EntityAlreadyExistsException("Username is already taken");
        }
        if (userRepository.existsByEmailIgnoreCase(dto.email())) {
            throw new EntityAlreadyExistsException("Email is already taken");
        }

        emailDomainValidator.validateDomainHasMxRecord(dto.email());

        User savedUser = persistNewUser(dto);

        String jwtToken = jwtService.generateToken(savedUser);
        return new UserRegisterDto(savedUser.getId(), savedUser.getUsername(), null,
                savedUser.getProfileImageUrl(), savedUser.getEmail(), jwtToken);
    }

    public UserLoginDto loginUser(String email, String rawPassword, String sourceEventId, HttpServletRequest request, HttpServletResponse response) {
        User user = userRepository.findUserByEmail(email).orElse(null);

        if (user == null) {
            throw new InvalidCredentialsException("Incorrect email or password");
        }

        if (!user.isPasswordSet()) {
            log.warn("Password login rejected because local password is not set. userId={}", user.getId());
            publishLoginActivity(user.getId(), LoginActivityStatus.UNSUCCESSFUL, request);
            throw new LocalPasswordNotSetException("Local password is not set for this account. Use Google login or set a local password first.");
        }

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            log.warn("Password login failed. userId={}", user.getId());
            publishLoginActivity(user.getId(), LoginActivityStatus.UNSUCCESSFUL, request);
            throw new InvalidCredentialsException("Incorrect email or password");
        }

        return twoFactorLoginService.startPasswordLogin(user.getId(), sourceEventId, request, response);
    }

    private User persistNewUser(UserRegisterDto dto) {
        try {
            String encodedPassword = passwordEncoder.encode(dto.password());
            String defaultImagePath = Paths.get(profileImagesDefaultDirectory).resolve("UserProf.png").toString();
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

            userRepository.save(user);

            outboxService.save("User", user.getId().toString(), "user.created",
                    new UserCreatedEvent(user.getId(), user.getUsername(), user.getEmail(), user.getCreatedAt()));
            return user;
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