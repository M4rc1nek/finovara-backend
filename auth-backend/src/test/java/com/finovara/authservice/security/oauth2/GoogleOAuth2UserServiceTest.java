package com.finovara.authservice.security.oauth2;

import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.contracts.event.notification.CreateDefaultNotificationEmailSettingsEvent;
import com.finovara.authservice.user.model.OAuthProvider;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.authservice.settings.factory.SettingsFactory;
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
import org.springframework.kafka.core.KafkaTemplate;
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

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

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
        when(oauth2User.getAttributes()).thenReturn(buildAttributes(NAME, EMAIL, PICTURE));
    }

    private static Map<String, Object> buildAttributes(String name, String email, String picture) {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("sub", PROVIDER_USER_ID);
        attrs.put("email", email);
        attrs.put("picture", picture);
        if (name != null) {
            attrs.put("name", name);
        }
        return attrs;
    }

    private void stubSaveReturnsInput() {
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User user = inv.getArgument(0);
            if (user.getId() == null) {
                user.setId(1L);
            }
            return user;
        });
    }

    private void verifyNoSettingsCreated() {
        verify(settingsFactory, never()).createDefaultExpenseSettings(any());
        verify(settingsFactory, never()).createDefaultRecurringSettings(any());
        verify(settingsFactory, never()).createDefaultAccountSettings(any());
        verify(kafkaTemplate, never()).send(eq("user.created"), any());
    }

    @Nested
    class WhenUserDoesNotExist {

        @BeforeEach
        void setUp() {
            when(userRepository.findByOauthProviderAndProviderUserId(OAuthProvider.GOOGLE, PROVIDER_USER_ID))
                    .thenReturn(Optional.empty());
        }

        @Test
        void shouldCreateNewUser() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(userRepository.existsByUsername(NAME)).thenReturn(false);
            stubSaveReturnsInput();

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
        void shouldInitializeLocalSettingsAndPublishNotificationSettingsCreation() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(userRepository.existsByUsername(NAME)).thenReturn(false);
            stubSaveReturnsInput();

            googleOAuth2UserService.synchronize(oauth2User);

            verify(settingsFactory).createDefaultAccountSettings(any());
            ArgumentCaptor<CreateDefaultNotificationEmailSettingsEvent> eventCaptor =
                    ArgumentCaptor.forClass(CreateDefaultNotificationEmailSettingsEvent.class);
            verify(kafkaTemplate).send(eq("user.created"), eventCaptor.capture());
            assertThat(eventCaptor.getValue().userId()).isEqualTo(1L);
        }

        @Test
        void shouldThrowWhenEmailAlreadyExists() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> googleOAuth2UserService.synchronize(oauth2User))
                    .isInstanceOf(EntityAlreadyExistsException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldAppendSuffixWhenBaseUsernameAlreadyTaken() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(userRepository.existsByUsername(NAME)).thenReturn(true);
            stubSaveReturnsInput();

            User result = googleOAuth2UserService.synchronize(oauth2User);

            assertThat(result.getUsername())
                    .startsWith(NAME + "-")
                    .hasSize(NAME.length() + 9);
        }

        @ParameterizedTest
        @MethodSource("blankNameInputs")
        void shouldFallbackToEmailPrefixWhenNameIsBlankOrMissing(String name, String email, String expectedUsername) {
            when(oauth2User.getAttributes()).thenReturn(buildAttributes(name, email, PICTURE));
            when(userRepository.existsByEmail(email)).thenReturn(false);
            when(userRepository.existsByUsername(expectedUsername)).thenReturn(false);
            stubSaveReturnsInput();

            User result = googleOAuth2UserService.synchronize(oauth2User);

            assertThat(result.getUsername()).isEqualTo(expectedUsername);
        }

        static Stream<Arguments> blankNameInputs() {
            return Stream.of(
                    Arguments.of("", "test@example.com", "test"),
                    Arguments.of("   ", "test@example.com", "test"),
                    Arguments.of(null, "user@domain.com", "user")
            );
        }
    }

    @Nested
    class WhenUserAlreadyExists {

        private User existingUser;

        @BeforeEach
        void setUp() {
            existingUser = User.builder()
                    .id(1L)
                    .username(NAME)
                    .email(EMAIL)
                    .oauthProvider(OAuthProvider.GOOGLE)
                    .providerUserId(PROVIDER_USER_ID)
                    .build();

            when(userRepository.findByOauthProviderAndProviderUserId(OAuthProvider.GOOGLE, PROVIDER_USER_ID))
                    .thenReturn(Optional.of(existingUser));
        }

        @Test
        void shouldUpdateEmail() {
            String newEmail = "newemail@example.com";
            when(oauth2User.getAttributes()).thenReturn(buildAttributes(NAME, newEmail, PICTURE));
            when(userRepository.existsByEmailAndIdNot(newEmail, 1L)).thenReturn(false);
            stubSaveReturnsInput();

            User result = googleOAuth2UserService.synchronize(oauth2User);

            assertThat(result.getEmail()).isEqualTo(newEmail);
        }

        @Test
        void shouldUpdateRemoteProfileImageWhenGooglePictureChanges() {
            existingUser.setProfileImagePath("https://old.picture/photo.jpg");
            String newPicture = "https://new.picture/photo.jpg";
            when(oauth2User.getAttributes()).thenReturn(buildAttributes(NAME, EMAIL, newPicture));
            when(userRepository.existsByEmailAndIdNot(EMAIL, 1L)).thenReturn(false);
            stubSaveReturnsInput();

            User result = googleOAuth2UserService.synchronize(oauth2User);

            assertThat(result.getProfileImagePath()).isEqualTo(newPicture);
        }

        @Test
        void shouldKeepUploadedProfileImageWhenGooglePictureChanges() {
            String uploadedProfileImage = "uploads/profile-images/custom-avatar.png";
            existingUser.setProfileImagePath(uploadedProfileImage);
            when(oauth2User.getAttributes()).thenReturn(buildAttributes(NAME, EMAIL, "https://new.picture/photo.jpg"));
            when(userRepository.existsByEmailAndIdNot(EMAIL, 1L)).thenReturn(false);
            stubSaveReturnsInput();

            User result = googleOAuth2UserService.synchronize(oauth2User);

            assertThat(result.getProfileImagePath()).isEqualTo(uploadedProfileImage);
        }

        @Test
        void shouldKeepPublicLocalProfileImageUrlWhenGooglePictureChanges() {
            String publicProfileImageUrl = "/profile-images/custom-avatar.png";
            existingUser.setProfileImagePath(publicProfileImageUrl);
            when(oauth2User.getAttributes()).thenReturn(buildAttributes(NAME, EMAIL, "https://new.picture/photo.jpg"));
            when(userRepository.existsByEmailAndIdNot(EMAIL, 1L)).thenReturn(false);
            stubSaveReturnsInput();

            User result = googleOAuth2UserService.synchronize(oauth2User);

            assertThat(result.getProfileImagePath()).isEqualTo(publicProfileImageUrl);
        }

        @Test
        void shouldNotCreateNewSettingsForExistingUser() {
            when(userRepository.existsByEmailAndIdNot(EMAIL, 1L)).thenReturn(false);
            stubSaveReturnsInput();

            googleOAuth2UserService.synchronize(oauth2User);

            verifyNoSettingsCreated();
        }

        @Test
        void shouldThrowWhenEmailTakenByAnotherUser() {
            when(userRepository.existsByEmailAndIdNot(EMAIL, 1L)).thenReturn(true);

            assertThatThrownBy(() -> googleOAuth2UserService.synchronize(oauth2User))
                    .isInstanceOf(EntityAlreadyExistsException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldUpdateUsernameWhenGoogleNameChanged() {
            String newName = "New Name";
            when(oauth2User.getAttributes()).thenReturn(buildAttributes(newName, EMAIL, PICTURE));
            when(userRepository.existsByEmailAndIdNot(EMAIL, 1L)).thenReturn(false);
            when(userRepository.existsByUsernameAndIdNot(newName, 1L)).thenReturn(false);
            stubSaveReturnsInput();

            User result = googleOAuth2UserService.synchronize(oauth2User);

            assertThat(result.getUsername()).isEqualTo(newName);
        }

        @Test
        void shouldNotUpdateUsernameWhenGoogleNameUnchanged() {
            when(userRepository.existsByEmailAndIdNot(EMAIL, 1L)).thenReturn(false);
            stubSaveReturnsInput();

            googleOAuth2UserService.synchronize(oauth2User);

            verify(userRepository, never()).existsByUsernameAndIdNot(any(), any());
            assertThat(existingUser.getUsername()).isEqualTo(NAME);
        }

        @Test
        void shouldThrowWhenNewUsernameTakenByAnotherUser() {
            String newName = "Existing Name";
            when(oauth2User.getAttributes()).thenReturn(buildAttributes(newName, EMAIL, PICTURE));
            when(userRepository.existsByEmailAndIdNot(EMAIL, 1L)).thenReturn(false);
            when(userRepository.existsByUsernameAndIdNot(newName, 1L)).thenReturn(true);

            assertThatThrownBy(() -> googleOAuth2UserService.synchronize(oauth2User))
                    .isInstanceOf(EntityAlreadyExistsException.class);

            verify(userRepository, never()).save(any());
        }
    }
}
