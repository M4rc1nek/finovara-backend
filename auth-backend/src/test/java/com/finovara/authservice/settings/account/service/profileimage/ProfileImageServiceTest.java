package com.finovara.authservice.settings.account.service.profileimage;

import com.finovara.authservice.settings.security.operationauthorization.service.AdditionalAuthorizationService;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.contracts.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.authorization.dto.ConfirmAuthorizationCodeDto;
import com.finovara.contracts.event.activity.secure.accountchange.activity.AccountChangesActivityEvent;
import com.finovara.contracts.model.activity.AccountChangesActivityType;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileImageServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserManagerService userManagerService;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private AdditionalAuthorizationService additionalAuthorizationService;
    @Mock
    private AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private ProfileImageService profileImageService;

    @TempDir
    Path uploadDir;
    @TempDir
    Path defaultDir;

    private User user;
    private ConfirmAuthorizationCodeDto resolvedAuthorizationCode;
    private static final Long USER_ID = 1L;
    private static final String AUTHORIZATION_CODE = "auth";

    @BeforeEach
    void setUp() throws Exception {
        user = new User();
        user.setId(USER_ID);
        user.setEmail("test@test.com");
        resolvedAuthorizationCode = mock(ConfirmAuthorizationCodeDto.class);

        Files.createFile(defaultDir.resolve("UserProf.png"));

        ReflectionTestUtils.setField(profileImageService, "profileImagesDirectory", uploadDir.toString());
        ReflectionTestUtils.setField(profileImageService, "profileImagesDefaultDirectory", defaultDir.toString());

        when(additionalAuthorizationCodeResolver.resolve(AUTHORIZATION_CODE)).thenReturn(resolvedAuthorizationCode);
    }

    private void stubUserFound() {
        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
    }

    private void stubValidRequest() {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0");
    }

    @Nested
    class UploadProfileImage {

        private final MockMultipartFile validFile =
                new MockMultipartFile("file", "avatar.png", "image/png", "image-data".getBytes());

        @Test
        void shouldSaveFileOnDiskAndUpdateUserPathWhenUploadingValidFile() {
            stubUserFound();
            stubValidRequest();

            profileImageService.uploadProfileImage(validFile, USER_ID, request, AUTHORIZATION_CODE);

            assertThat(user.getProfileImagePath()).isNotNull();
            assertThat(Files.exists(Path.of(user.getProfileImagePath()))).isTrue();
        }

        @Test
        void shouldPersistUserWhenUploadingValidFile() {
            stubUserFound();
            stubValidRequest();

            profileImageService.uploadProfileImage(validFile, USER_ID, request, AUTHORIZATION_CODE);

            verify(userRepository).save(user);
        }

        @Test
        void shouldConfirmAdditionalAuthorizationCodeWhenUploadingValidFile() {
            stubUserFound();
            stubValidRequest();

            profileImageService.uploadProfileImage(validFile, USER_ID, request, AUTHORIZATION_CODE);

            verify(additionalAuthorizationCodeResolver).resolve(AUTHORIZATION_CODE);
            verify(additionalAuthorizationService).confirmAdditionalAuthorizationCode(USER_ID, resolvedAuthorizationCode);
        }

        @Test
        void shouldTrackAccountActivityWhenUploadingValidFile() {
            stubUserFound();
            stubValidRequest();

            profileImageService.uploadProfileImage(validFile, USER_ID, request, AUTHORIZATION_CODE);

            ArgumentCaptor<AccountChangesActivityEvent> captor =
                    ArgumentCaptor.forClass(AccountChangesActivityEvent.class);

            verify(kafkaTemplate).send(eq("activity.account-changes"), captor.capture());
            assertThat(captor.getValue().type()).isEqualTo(AccountChangesActivityType.PROFILE_IMG_CHANGED);
        }

        @Test
        void shouldGenerateUniqueFilenameContainingOriginalNameWhenUploadingValidFile() {
            stubUserFound();
            stubValidRequest();

            profileImageService.uploadProfileImage(validFile, USER_ID, request, AUTHORIZATION_CODE);

            assertThat(user.getProfileImagePath()).contains("avatar.png");
        }

        @Test
        void shouldDeleteOldLocalImageWhenUploadingNewFile() throws Exception {
            Path oldFile = Files.createTempFile(uploadDir, "old-avatar", ".png");
            user.setProfileImagePath(oldFile.toString());
            stubUserFound();
            stubValidRequest();

            profileImageService.uploadProfileImage(validFile, USER_ID, request, AUTHORIZATION_CODE);

            assertThat(Files.exists(oldFile)).isFalse();
        }

        @Test
        void shouldNotDeleteExternalImageWhenUploadingNewFile() {
            user.setProfileImagePath("https://cdn.example.com/avatar.png");
            stubUserFound();
            stubValidRequest();

            profileImageService.uploadProfileImage(validFile, USER_ID, request, AUTHORIZATION_CODE);

            verify(userRepository).save(user);
        }

        @Test
        void shouldUploadJpegFileWhenContentTypeIsImageJpeg() {
            MockMultipartFile jpeg =
                    new MockMultipartFile("file", "photo.jpg", "image/jpeg", "jpeg-data".getBytes());
            stubUserFound();
            stubValidRequest();

            profileImageService.uploadProfileImage(jpeg, USER_ID, request, AUTHORIZATION_CODE);

            assertThat(user.getProfileImagePath()).contains("photo.jpg");
        }

        @Test
        void shouldAcceptFileWhenSizeIsExactly5MB() {
            MockMultipartFile exact =
                    new MockMultipartFile("file", "exact.png", "image/png", new byte[5 * 1024 * 1024]);
            stubUserFound();
            stubValidRequest();

            profileImageService.uploadProfileImage(exact, USER_ID, request, AUTHORIZATION_CODE);

            verify(userRepository).save(user);
        }

        @Test
        void shouldThrowExceptionWhenFileIsEmpty() {
            MockMultipartFile empty =
                    new MockMultipartFile("file", "avatar.png", "image/png", new byte[0]);
            stubUserFound();

            assertThatThrownBy(() ->
                    profileImageService.uploadProfileImage(empty, USER_ID, request, AUTHORIZATION_CODE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("File is empty");

            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldThrowExceptionWhenFileIsNotAnImage() {
            MockMultipartFile pdf =
                    new MockMultipartFile("file", "document.pdf", "application/pdf", "data".getBytes());
            stubUserFound();

            assertThatThrownBy(() ->
                    profileImageService.uploadProfileImage(pdf, USER_ID, request, AUTHORIZATION_CODE))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldThrowExceptionWhenContentTypeIsNull() {
            MockMultipartFile noContentType =
                    new MockMultipartFile("file", "avatar.png", null, "data".getBytes());
            stubUserFound();

            assertThatThrownBy(() ->
                    profileImageService.uploadProfileImage(noContentType, USER_ID, request, AUTHORIZATION_CODE))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldThrowExceptionWhenFileSizeExceeds5MB() {
            MockMultipartFile large =
                    new MockMultipartFile("file", "large.png", "image/png", new byte[6 * 1024 * 1024]);
            stubUserFound();

            assertThatThrownBy(() ->
                    profileImageService.uploadProfileImage(large, USER_ID, request, AUTHORIZATION_CODE))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldNotSaveUserWhenAdditionalAuthorizationCodeConfirmationFails() {
            doThrow(new IllegalArgumentException("Invalid authorization code"))
                    .when(additionalAuthorizationService)
                    .confirmAdditionalAuthorizationCode(USER_ID, resolvedAuthorizationCode);

            assertThatThrownBy(() ->
                    profileImageService.uploadProfileImage(validFile, USER_ID, request, AUTHORIZATION_CODE))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    class DeleteProfileImage {

        @Test
        void shouldDeleteLocalFileFromDiskWhenDeletingExistingImage() throws Exception {
            Path image = Files.createTempFile(uploadDir, "avatar", ".png");
            user.setProfileImagePath(image.toString());
            stubUserFound();
            stubValidRequest();

            profileImageService.deleteProfileImage(USER_ID, request, AUTHORIZATION_CODE);

            assertThat(Files.exists(image)).isFalse();
        }

        @Test
        void shouldSetUserPathToDefaultImageWhenDeletingExistingImage() throws Exception {
            Path image = Files.createTempFile(uploadDir, "avatar", ".png");
            user.setProfileImagePath(image.toString());
            stubUserFound();
            stubValidRequest();

            profileImageService.deleteProfileImage(USER_ID, request, AUTHORIZATION_CODE);

            assertThat(user.getProfileImagePath()).contains("UserProf.png");
        }

        @Test
        void shouldPersistUserWhenDeletingExistingImage() throws Exception {
            Path image = Files.createTempFile(uploadDir, "avatar", ".png");
            user.setProfileImagePath(image.toString());
            stubUserFound();
            stubValidRequest();

            profileImageService.deleteProfileImage(USER_ID, request, AUTHORIZATION_CODE);

            verify(userRepository).save(user);
        }

        @Test
        void shouldTrackAccountActivityWhenDeletingExistingImage() throws Exception {
            Path image = Files.createTempFile(uploadDir, "avatar", ".png");
            user.setProfileImagePath(image.toString());
            stubUserFound();
            stubValidRequest();

            profileImageService.deleteProfileImage(USER_ID, request, AUTHORIZATION_CODE);

            ArgumentCaptor<AccountChangesActivityEvent> captor =
                    ArgumentCaptor.forClass(AccountChangesActivityEvent.class);
            verify(kafkaTemplate).send(eq("activity.account-changes"), captor.capture());
            assertThat(captor.getValue().type()).isEqualTo(AccountChangesActivityType.PROFILE_IMG_DELETED);
        }

        @Test
        void shouldNotDeleteExternalImageFileWhenDeletingExternalImage() {
            user.setProfileImagePath("https://cdn.example.com/avatar.png");
            stubUserFound();
            stubValidRequest();

            profileImageService.deleteProfileImage(USER_ID, request, AUTHORIZATION_CODE);

            assertThat(user.getProfileImagePath()).contains("UserProf.png");
            verify(userRepository).save(user);
        }

        @Test
        void shouldConfirmAdditionalAuthorizationCodeWhenDeletingExistingImage() throws Exception {
            Path image = Files.createTempFile(uploadDir, "avatar", ".png");
            user.setProfileImagePath(image.toString());
            stubUserFound();
            stubValidRequest();

            profileImageService.deleteProfileImage(USER_ID, request, AUTHORIZATION_CODE);

            verify(additionalAuthorizationCodeResolver).resolve(AUTHORIZATION_CODE);
            verify(additionalAuthorizationService).confirmAdditionalAuthorizationCode(USER_ID, resolvedAuthorizationCode);
        }

        @Test
        void shouldThrowExceptionWhenPathIsNull() {
            user.setProfileImagePath(null);
            stubUserFound();

            assertThatThrownBy(() ->
                    profileImageService.deleteProfileImage(USER_ID, request, AUTHORIZATION_CODE))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldThrowExceptionWhenPathIsAlreadyDefault() {
            String defaultPath = defaultDir.resolve("UserProf.png").toString();
            user.setProfileImagePath(defaultPath);
            stubUserFound();

            assertThatThrownBy(() ->
                    profileImageService.deleteProfileImage(USER_ID, request, AUTHORIZATION_CODE))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldNotSaveUserWhenAdditionalAuthorizationCodeConfirmationFails() {
            doThrow(new IllegalArgumentException("Invalid authorization code"))
                    .when(additionalAuthorizationService)
                    .confirmAdditionalAuthorizationCode(USER_ID, resolvedAuthorizationCode);

            assertThatThrownBy(() ->
                    profileImageService.deleteProfileImage(USER_ID, request, AUTHORIZATION_CODE))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(userRepository, never()).save(any());
        }
    }
}