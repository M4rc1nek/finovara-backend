package com.finovara.authservice.security.oauth2;

import com.finovara.contracts.event.user.UserCreatedEvent;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.authservice.security.oauth2.dto.GoogleOAuth2UserInfo;
import com.finovara.authservice.user.model.OAuthProvider;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.authservice.settings.factory.SettingsFactory;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoogleOAuth2UserService {

    private static final OAuthProvider GOOGLE_PROVIDER = OAuthProvider.GOOGLE;

    private final UserRepository userRepository;
    private final SettingsFactory settingsFactory;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public User synchronize(OAuth2User oauth2User) {
        GoogleOAuth2UserInfo userInfo = GoogleOAuth2UserInfo.from(oauth2User.getAttributes());

        return userRepository.findByOauthProviderAndProviderUserId(GOOGLE_PROVIDER, userInfo.providerUserId())
                .map(user -> synchronizeExistingGoogleUser(user, userInfo))
                .orElseGet(() -> createGoogleUser(userInfo));
    }

    private User createGoogleUser(GoogleOAuth2UserInfo userInfo) {
        if (userRepository.existsByEmail(userInfo.email())) {
            throw new EntityAlreadyExistsException("User already exists");
        }

        User user = User.builder()
                .username(resolveUniqueUsername(userInfo.name(), userInfo.email()))
                .email(userInfo.email())
                .password(null)
                .createdAt(LocalDateTime.now())
                .profileImagePath(userInfo.picture())
                .oauthProvider(GOOGLE_PROVIDER)
                .providerUserId(userInfo.providerUserId())
                .build();
        user.setPasswordSet(false);

        user.setAccountSettings(settingsFactory.createDefaultAccountSettings(user));

        User savedUser = userRepository.save(user);
        kafkaTemplate.send("user.created", new UserCreatedEvent(savedUser.getId(), savedUser.getUsername(), savedUser.getEmail(), savedUser.getCreatedAt()));
        return savedUser;
    }

    private User synchronizeExistingGoogleUser(User user, GoogleOAuth2UserInfo userInfo) {
        if (userRepository.existsByEmailAndIdNot(userInfo.email(), user.getId())) {
            throw new EntityAlreadyExistsException("User already exists");
        }

        user.setEmail(userInfo.email());
        synchronizeProfileImage(user, userInfo);
        synchronizeUsername(user, userInfo);

        return userRepository.save(user);
    }

    private void synchronizeProfileImage(User user, GoogleOAuth2UserInfo userInfo) {
        boolean hasCustomProfileImage = hasCustomProfileImage(user.getProfileImagePath());

        if (!hasCustomProfileImage) {
            user.setProfileImagePath(userInfo.picture());
        }
    }

    private boolean hasCustomProfileImage(String profileImagePath) {
        return StringUtils.hasText(profileImagePath) && !isRemoteProfileImagePath(profileImagePath);
    }

    private boolean isRemoteProfileImagePath(String profileImagePath) {
        return profileImagePath.startsWith("http://") || profileImagePath.startsWith("https://");
    }

    private void synchronizeUsername(User user, GoogleOAuth2UserInfo userInfo) {
        String googleName = normalizeUsername(userInfo.name(), userInfo.email());

        if (googleName.equals(user.getUsername())) {
            return;
        }

        if (userRepository.existsByUsernameAndIdNot(googleName, user.getId())) {
            throw new EntityAlreadyExistsException("Username is already taken");
        }

        user.setUsername(googleName);
    }

    private String resolveUniqueUsername(String name, String email) {
        String baseUsername = normalizeUsername(name, email);
        if (!userRepository.existsByUsername(baseUsername)) {
            return baseUsername;
        }

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return baseUsername + "-" + suffix;
    }

    private String normalizeUsername(String name, String email) {
        if (StringUtils.hasText(name)) {
            return name.trim();
        }

        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : email;
    }
}
