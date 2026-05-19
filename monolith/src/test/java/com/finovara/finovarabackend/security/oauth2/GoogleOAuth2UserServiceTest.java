package com.finovara.finovarabackend.security.oauth2;

import com.finovara.finovarabackend.exception.conflict.NameAlreadyExistsException;
import com.finovara.finovarabackend.user.exception.conflict.EmailAlreadyExistsException;
import com.finovara.finovarabackend.user.model.OAuthProvider;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.usersetting.factory.SettingsFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleOAuth2UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SettingsFactory settingsFactory;

    @InjectMocks
    private GoogleOAuth2UserService googleOAuth2UserService;

    private OAuth2User oauth2User;

    private static final String PROVIDER_USER_ID = "google-123";
    private static final String EMAIL = "test@example.com";
    private static final String NAME = "Jan Kowalski";
    private static final String PICTURE = "https://picture.url/photo.jpg";

    @BeforeEach
    void setUp() {
        oauth2User = mock(OAuth2User.class);
        when(oauth2User.getAttributes()).thenReturn(Map.of("sub", PROVIDER_USER_ID, "email", EMAIL, "name", NAME, "picture", PICTURE));
    }

    @Nested
    class WhenUserDoesNotExist {

        @BeforeEach
        void setUp() {
            when(userRepository.findByOauthProviderAndProviderUserId(OAuthProvider.GOOGLE, PROVIDER_USER_ID)).thenReturn(Optional.empty());
        }

        @Test
        void shouldCreateNewUser() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(userRepository.existsByUsername(NAME)).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            googleOAuth2UserService.synchronize(oauth2User);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());

            User saved = captor.getValue();
            assertThat(saved.getEmail()).isEqualTo(EMAIL);
            assertThat(saved.getUsername()).isEqualTo(NAME);
            assertThat(saved.getProfileImagePath()).isEqualTo(PICTURE);
            assertThat(saved.getOauthProvider()).isEqualTo(OAuthProvider.GOOGLE);
            assertThat(saved.getProviderUserId()).isEqualTo(PROVIDER_USER_ID);
            assertThat(saved.getPassword()).isNull();
            assertThat(saved.isPasswordSet()).isFalse();
        }

        @Test
        void shouldInitializeAllSettings() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(userRepository.existsByUsername(NAME)).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            googleOAuth2UserService.synchronize(oauth2User);

            verify(settingsFactory).createDefaultExpenseSettings(any());
            verify(settingsFactory).createDefaultRecurringSettings(any());
            verify(settingsFactory).createDefaultNotificationSettings(any());
            verify(settingsFactory).createDefaultAccountSettings(any());
        }

        @Test
        void shouldThrowWhenEmailAlreadyExists() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> googleOAuth2UserService.synchronize(oauth2User)).isInstanceOf(EmailAlreadyExistsException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldAppendSuffixWhenBaseUsernameAlreadyTaken() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(userRepository.existsByUsername(NAME)).thenReturn(true);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = googleOAuth2UserService.synchronize(oauth2User);

            assertThat(result.getUsername()).startsWith(NAME + "-").hasSize(NAME.length() + 9);
        }

        @ParameterizedTest
        @MethodSource("blankNameInputs")
        void shouldFallbackToEmailPrefixWhenNameIsBlankOrMissing(String name, String email, String expectedUsername) {
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("sub", PROVIDER_USER_ID);
            attrs.put("email", email);
            if (name != null) {
                attrs.put("name", name);
            }
            when(oauth2User.getAttributes()).thenReturn(attrs);
            when(userRepository.existsByEmail(email)).thenReturn(false);
            when(userRepository.existsByUsername(expectedUsername)).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = googleOAuth2UserService.synchronize(oauth2User);

            assertThat(result.getUsername()).isEqualTo(expectedUsername);
        }

        static Stream<Arguments> blankNameInputs() {
            return Stream.of(Arguments.of("", "test@example.com", "test"),
                    Arguments.of("   ", "test@example.com", "test"),
                    Arguments.of(null, "user@domain.com", "user"));
        }
    }

    @Nested
    class WhenUserAlreadyExists {

        private User existingUser;

        @BeforeEach
        void setUp() {
            existingUser = User.builder().username(NAME).email(EMAIL).oauthProvider(OAuthProvider.GOOGLE).providerUserId(PROVIDER_USER_ID).build();
            existingUser.setId(1L);

            when(userRepository.findByOauthProviderAndProviderUserId(OAuthProvider.GOOGLE, PROVIDER_USER_ID)).thenReturn(Optional.of(existingUser));
        }

        @Test
        void shouldUpdateEmailAndProfileImage() {
            String newEmail = "newemail@example.com";
            String newPicture = "https://new.picture/photo.jpg";
            when(oauth2User.getAttributes()).thenReturn(Map.of("sub", PROVIDER_USER_ID, "email", newEmail, "name", NAME, "picture", newPicture));
            when(userRepository.existsByEmailAndIdNot(newEmail, 1L)).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = googleOAuth2UserService.synchronize(oauth2User);

            assertThat(result.getEmail()).isEqualTo(newEmail);
            assertThat(result.getProfileImagePath()).isEqualTo(newPicture);
        }

        @Test
        void shouldNotCreateNewSettingsForExistingUser() {
            when(userRepository.existsByEmailAndIdNot(EMAIL, 1L)).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            googleOAuth2UserService.synchronize(oauth2User);

            verify(settingsFactory, never()).createDefaultExpenseSettings(any());
            verify(settingsFactory, never()).createDefaultRecurringSettings(any());
            verify(settingsFactory, never()).createDefaultNotificationSettings(any());
            verify(settingsFactory, never()).createDefaultAccountSettings(any());
        }

        @Test
        void shouldThrowWhenEmailTakenByAnotherUser() {
            when(userRepository.existsByEmailAndIdNot(EMAIL, 1L)).thenReturn(true);

            assertThatThrownBy(() -> googleOAuth2UserService.synchronize(oauth2User)).isInstanceOf(EmailAlreadyExistsException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldUpdateUsernameWhenGoogleNameChanged() {
            String newName = "New name";
            when(oauth2User.getAttributes()).thenReturn(Map.of("sub", PROVIDER_USER_ID, "email", EMAIL, "name", newName, "picture", PICTURE));
            when(userRepository.existsByEmailAndIdNot(EMAIL, 1L)).thenReturn(false);
            when(userRepository.existsByUsernameAndIdNot(newName, 1L)).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = googleOAuth2UserService.synchronize(oauth2User);

            assertThat(result.getUsername()).isEqualTo(newName);
        }

        @Test
        void shouldNotUpdateUsernameWhenGoogleNameUnchanged() {
            when(userRepository.existsByEmailAndIdNot(EMAIL, 1L)).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            googleOAuth2UserService.synchronize(oauth2User);

            verify(userRepository, never()).existsByUsernameAndIdNot(any(), any());
            assertThat(existingUser.getUsername()).isEqualTo(NAME);
        }

        @Test
        void shouldThrowWhenNewUsernameTakenByAnotherUser() {
            String newName = "Existing Name";
            when(oauth2User.getAttributes()).thenReturn(Map.of("sub", PROVIDER_USER_ID, "email", EMAIL, "name", newName, "picture", PICTURE));
            when(userRepository.existsByEmailAndIdNot(EMAIL, 1L)).thenReturn(false);
            when(userRepository.existsByUsernameAndIdNot(newName, 1L)).thenReturn(true);

            assertThatThrownBy(() -> googleOAuth2UserService.synchronize(oauth2User)).isInstanceOf(NameAlreadyExistsException.class);

            verify(userRepository, never()).save(any());
        }
    }
}