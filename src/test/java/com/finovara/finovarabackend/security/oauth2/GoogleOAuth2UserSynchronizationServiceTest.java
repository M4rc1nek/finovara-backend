/*
package com.finovara.finovarabackend.security.oauth2;

import com.finovara.finovarabackend.user.exception.conflict.EmailAlreadyExistsException;
import com.finovara.finovarabackend.user.model.OAuthProvider;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.usersetting.factory.SettingsFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleOAuth2UserSynchronizationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SettingsFactory settingsFactory;

    @InjectMocks
    private GoogleOAuth2UserService service;

    @Test
    void shouldCreateNewGoogleUserWhenProviderUserDoesNotExist() {
        OAuth2User oauth2User = googleUser();

        when(userRepository.findByOauthProviderAndProviderUserId(OAuthProvider.GOOGLE, "google-sub-123"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("Jane Doe")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = service.synchronize(oauth2User);

        assertThat(result.getUsername()).isEqualTo("Jane Doe");
        assertThat(result.getEmail()).isEqualTo("jane@example.com");
        assertThat(result.getProfileImagePath()).isEqualTo("https://lh3.googleusercontent.com/avatar");
        assertThat(result.getOauthProvider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(result.getProviderUserId()).isEqualTo("google-sub-123");
        assertThat(result.getPassword()).isEqualTo("encoded-password");
    }

    @Test
    void shouldThrowWhenEmailAlreadyExistsWithoutGoogleProviderLink() {
        OAuth2User oauth2User = googleUser();

        when(userRepository.findByOauthProviderAndProviderUserId(OAuthProvider.GOOGLE, "google-sub-123"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.synchronize(oauth2User))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage("User already exists");
    }

    @Test
    void shouldSynchronizeExistingGoogleUser() {
        OAuth2User oauth2User = googleUser();
        User existingUser = User.builder()
                .id(1L)
                .username("Old Name")
                .email("old@example.com")
                .oauthProvider(OAuthProvider.GOOGLE)
                .providerUserId("google-sub-123")
                .build();

        when(userRepository.findByOauthProviderAndProviderUserId(OAuthProvider.GOOGLE, "google-sub-123"))
                .thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmailAndIdNot("jane@example.com", 1L)).thenReturn(false);
        when(userRepository.existsByUsernameAndIdNot("Jane Doe", 1L)).thenReturn(false);
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        service.synchronize(oauth2User);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("Jane Doe");
        assertThat(savedUser.getEmail()).isEqualTo("jane@example.com");
        assertThat(savedUser.getProfileImagePath()).isEqualTo("https://lh3.googleusercontent.com/avatar");
    }

    private OAuth2User googleUser() {
        Map<String, Object> attributes = Map.of(
                "sub", "google-sub-123",
                "email", "jane@example.com",
                "name", "Jane Doe",
                "picture", "https://lh3.googleusercontent.com/avatar"
        );

        return new DefaultOAuth2User(List.of(), attributes, "sub");
    }
}
*/
