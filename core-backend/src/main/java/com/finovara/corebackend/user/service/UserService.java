package com.finovara.corebackend.user.service;

import com.finovara.activityservice.contracts.clientdata.browser.UserBrowser;
import com.finovara.activityservice.contracts.clientdata.ip.ClientIp;
import com.finovara.activityservice.contracts.clientdata.location.UserLocation;
import com.finovara.activityservice.contracts.event.secure.login.activity.LoginActivityEvent;
import com.finovara.activityservice.contracts.model.activity.LoginActivityStatus;

import static com.finovara.activityservice.contracts.clientdata.browser.UserBrowser.getBrowser;
import static com.finovara.activityservice.contracts.clientdata.ip.ClientIp.getClientIpAddress;
import static com.finovara.activityservice.contracts.clientdata.location.UserLocation.getLocationFromIp;
import com.finovara.corebackend.exception.conflict.NameAlreadyExistsException;
import com.finovara.corebackend.exception.conflict.StateConflictException;
import com.finovara.corebackend.exception.unauthorized.WrongPasswordException;
import com.finovara.corebackend.security.jwt.JwtService;
import com.finovara.corebackend.user.dto.UserLoginDto;
import com.finovara.corebackend.user.dto.UserRegisterDto;
import com.finovara.corebackend.user.exception.conflict.EmailAlreadyExistsException;
import com.finovara.corebackend.user.model.User;
import com.finovara.corebackend.user.repository.UserRepository;
import com.finovara.corebackend.usersetting.factory.SettingsFactory;
import com.finovara.corebackend.util.email.EmailDomainValidator;
import com.finovara.corebackend.util.profile.ProfileImageUrlBuilder;
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

import java.nio.file.Paths;
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
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final EmailDomainValidator emailDomainValidator;

    @Value("${application.upload.profile-images-default-directory}")
    private String profileImagesDefaultDirectory;

    public UserRegisterDto registerUser(UserRegisterDto dto) {
        if (userRepository.existsByUsername(dto.username())) {
            throw new NameAlreadyExistsException("Username is already taken");
        }

        if (userRepository.existsByEmail(dto.email())) {
            throw new EmailAlreadyExistsException("Email is already taken");
        }

        emailDomainValidator.validateDomainHasMxRecord(dto.email());

        String defaultImagePath = Paths.get(profileImagesDefaultDirectory).resolve("UserProf.png").toString();

        User user = User.builder().username(dto.username()).email(dto.email()).password(passwordEncoder.encode(dto.password())).passwordSet(true).profileImagePath(defaultImagePath).createdAt(LocalDateTime.now()).build();

        user.setExpenseSettings(settingsFactory.createDefaultExpenseSettings(user));
        user.setRecurringSettings(settingsFactory.createDefaultRecurringSettings(user));
        user.setNotificationEmailSettings(settingsFactory.createDefaultNotificationSettings(user));
        user.setAccountSettings(settingsFactory.createDefaultAccountSettings(user));

        User savedUser = userRepository.save(user);

        String jwtToken = jwtService.generateToken(savedUser);

        String userProfileImage = ProfileImageUrlBuilder.buildProfileImageUrl(savedUser.getProfileImagePath());

        return new UserRegisterDto(savedUser.getId(), savedUser.getUsername(), null, userProfileImage, savedUser.getEmail(), jwtToken);
    }

    public UserLoginDto loginUser(String email, String rawPassword, HttpServletRequest request) {
        User userByEmail = userRepository.findByEmail(email).orElse(null);

        if (userByEmail != null && !userByEmail.isPasswordSet()) {
            publishLoginActivity(userByEmail.getId(), LoginActivityStatus.UNSUCCESSFUL, request);
            throw new StateConflictException("Local password is not set for this account. Use Google login or set a local password first.");
        }

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, rawPassword));

            if (userByEmail == null) {
                throw new WrongPasswordException("Incorrect email or password");
            }

            publishLoginActivity(userByEmail.getId(), LoginActivityStatus.SUCCESSFUL, request);
            String jwtToken = jwtService.generateToken(userByEmail);
            String userProfileImage = ProfileImageUrlBuilder.buildProfileImageUrl(userByEmail.getProfileImagePath());

            return new UserLoginDto(userByEmail.getId(), userByEmail.getUsername(), userByEmail.getEmail(), null, userProfileImage, jwtToken);

        } catch (AuthenticationException e) {
            if (userByEmail != null) {
                publishLoginActivity(userByEmail.getId(), LoginActivityStatus.UNSUCCESSFUL, request);
            }
            throw new WrongPasswordException("Incorrect email or password");
        }
    }

    private void publishLoginActivity(Long userId, LoginActivityStatus status, HttpServletRequest request) {
        String ipAddress = getClientIpAddress(request);
        kafkaTemplate.send("activity.login", new LoginActivityEvent(userId, status, getBrowser(request), ipAddress, getLocationFromIp(ipAddress), LocalDateTime.now()));
    }
}
