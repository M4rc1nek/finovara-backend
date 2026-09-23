package com.finovara.authservice.settings.account.service.profileimage;

import com.finovara.authservice.riskverification.service.RiskGuardService;
import com.finovara.authservice.settings.security.operationauthorization.service.AdditionalAuthorizationService;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.contracts.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.authorization.dto.ConfirmAuthorizationCodeDto;
import com.finovara.contracts.activity.event.secure.accountchange.activity.AccountChangesActivityEvent;
import com.finovara.contracts.model.activity.AccountChangesActivityType;
import com.finovara.contracts.securitymonitoring.dto.RiskTriggerType;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private RiskGuardService riskGuardService;
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
    private static final String USER_EMAIL = "test@test.com";
    private static final String AUTHORIZATION_CODE = "auth";
    private static final String SOURCE_EVENT_ID = "risk-event-id";

    @BeforeEach
    void setUp() throws Exception {
        user = new User();
        user.setId(USER_ID);
        user.setEmail(USER_EMAIL);
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

            profileImageService.uploadProfileImage(validFile, USER_ID, request, AUTHORIZATION_CODE, SOURCE_EVENT_ID);

            assertThat(user.getProfileImagePath()).isNotNull();
            assertThat(Files.exists(Path.of(user.getProfileImagePath()))).isTrue();
        }

        @Test
        void shouldPersistUserWhenUploadingValidFile() {
            stubUserFound();
            stubValidRequest();

            profileImageService.uploadProfileImage(validFile, USER_ID, request, AUTHORIZATION_CODE, SOURCE_EVENT_ID);

            verify(userRepository).save(user);
        }

        @Test
        void shouldConfirmAdditionalAuthorizationCodeWhenUploadingValidFile() {
            stubUserFound();
            stubValidRequest();

            profileImageService.uploadProfileImage(validFile, USER_ID, request, AUTHORIZATION_CODE, SOURCE_EVENT_ID);

            verify(additionalAuthorizationCodeResolver).resolve(AUTHORIZATION_CODE);
            verify(additionalAuthorizationService).confirmAdditionalAuthorizationCode(USER_ID, resolvedAuthorizationCode);
        }

        @Test
        void shouldGuardAgainstRiskWhenUploadingValidFile() {
            stubUserFound();
            stubValidRequest();

            profileImageService.uploadProfileImage(validFile, USER_ID, request, AUTHORIZATION_CODE, SOURCE_EVENT_ID);

            verify(riskGuardService).guard(USER_ID, RiskTriggerType.PROFILE_IMAGE_CHANGED, USER_EMAIL, SOURCE_EVENT_ID, request);
        }

        @Test
        void shouldTrackAccountActivityWhenUploadingValidFile() {
            stubUserFound();
            stubValidRequest();

            profileImageService.uploadProfileImage(validFile, USER_ID, request, AUTHORIZATION_CODE, SOURCE_EVENT_ID);

            ArgumentCaptor<AccountChangesActivityEvent> captor =
                    ArgumentCaptor.forClass(AccountChangesActivityEvent.class);

            verify(kafkaTemplate).send(eq("account.changed"), captor.capture());
            assertThat(captor.getValue().type()).isEqualTo(AccountChangesActivityType.PROFILE_IMG_CHANGED);
        }

        @Test
        void shouldGenerateUniqueFilenameContainingOriginalNameWhenUploadingValidFile() {
            stubUserFound();
            stubValidRequest();

            profileImageService.uploadProfileImage(validFile, USER_ID, request, AUTHORIZATION_CODE, SOURCE_EVENT_ID);

            assertThat(user.getProfileImagePath()).contains("avatar.png");
        }

        @Test
        void shouldDeleteOldLocalImageWhenUploadingNewFile() throws Exception {
            Path oldFile = Files.createTempFile(uploadDir, "old-avatar", ".png");
            user.setProfileImagePath(oldFile.toString());
            stubUserFound();
            stubValidRequest();

            profileImageService.uploadProfileImage(validFile, USER_ID, request, AUTHORIZATION_CODE, SOURCE_EVENT_ID);

            assertThat(Files.exists(oldFile)).isFalse();
        }

        @Test
        void shouldNotDeleteExternalImageWhenUploadingNewFile() {
            user.setProfileImagePath("https://cdn.example.com/avatar.png");
            stubUserFound();
            stubValidRequest();

            profileImageService.uploadProfileImage(validFile, USER_ID, request, AUTHORIZATION_CODE, SOURCE_EVENT_ID);

            verify(userRepository).save(user);
        }

        @Test
        void shouldUploadJpegFileWhenContentTypeIsImageJpeg() {
            MockMultipartFile jpeg =
                    new MockMultipartFile("file", "photo.jpg", "image/jpeg", "jpeg-data".getBytes());
            stubUserFound();
            stubValidRequest();

            profileImageService.uploadProfileImage(jpeg, USER_ID, request, AUTHORIZATION_CODE, SOURCE_EVENT_ID);

            assertThat(user.getProfileImagePath()).contains("photo.jpg");
        }

        @Test
        void shouldAcceptFileWhenSizeIsExactly5MB() {
            MockMultipartFile exact =
                    new MockMultipartFile("file", "exact.png", "image/png", new byte[5 * 1024 * 1024]);
            stubUserFound();
            stubValidRequest();

            profileImageService.uploadProfileImage(exact, USER_ID, request, AUTHORIZATION_CODE, SOURCE_EVENT_ID);

            verify(userRepository).save(user);
        }

        @Test
        void shouldThrowIllegalArgumentExceptionWhenFileIsEmpty() {
            MockMultipartFile empty =
                    new MockMultipartFile("file", "avatar.png", "image/png", new byte[0]);
            stubUserFound();

            assertThrows(IllegalArgumentException.class, () ->
                    profileImageService.uploadProfileImage(empty, USER_ID, request, AUTHORIZATION_CODE, SOURCE_EVENT_ID));

            verifyNoInteractions(riskGuardService);
            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldThrowIllegalArgumentExceptionWhenFileIsNotAnImage() {
            MockMultipartFile pdf =
                    new MockMultipartFile("file", "document.pdf", "application/pdf", "data".getBytes());
            stubUserFound();

            assertThrows(IllegalArgumentException.class, () ->
                    profileImageService.uploadProfileImage(pdf, USER_ID, request, AUTHORIZATION_CODE, SOURCE_EVENT_ID));

            verifyNoInteractions(riskGuardService);
            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldThrowIllegalArgumentExceptionWhenContentTypeIsNull() {
            MockMultipartFile noContentType =
                    new MockMultipartFile("file", "avatar.png", null, "data".getBytes());
            stubUserFound();

            assertThrows(IllegalArgumentException.class, () ->
                    profileImageService.uploadProfileImage(noContentType, USER_ID, request, AUTHORIZATION_CODE, SOURCE_EVENT_ID));

            verifyNoInteractions(riskGuardService);
            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldThrowIllegalArgumentExceptionWhenFileSizeExceeds5MB() {
            MockMultipartFile large =
                    new MockMultipartFile("file", "large.png", "image/png", new byte[6 * 1024 * 1024]);
            stubUserFound();

            assertThrows(IllegalArgumentException.class, () ->
                    profileImageService.uploadProfileImage(large, USER_ID, request, AUTHORIZATION_CODE, SOURCE_EVENT_ID));

            verifyNoInteractions(riskGuardService);
            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldThrowIllegalArgumentExceptionWhenAdditionalAuthorizationCodeConfirmationFails() {
            doThrow(new IllegalArgumentException("Invalid authorization code"))
                    .when(additionalAuthorizationService)
                    .confirmAdditionalAuthorizationCode(USER_ID, resolvedAuthorizationCode);

            assertThrows(IllegalArgumentException.class, () ->
                    profileImageService.uploadProfileImage(validFile, USER_ID, request, AUTHORIZATION_CODE, SOURCE_EVENT_ID));

            verifyNoInteractions(riskGuardService);
            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldThrowIllegalStateExceptionWhenRiskGuardFails() {
            stubUserFound();
            doThrow(new IllegalStateException("Risk detected"))
                    .when(riskGuardService)
                    .guard(USER_ID, RiskTriggerType.PROFILE_IMAGE_CHANGED, USER_EMAIL, SOURCE_EVENT_ID, request);

            assertThrows(IllegalStateException.class, () ->
                    profileImageService.uploadProfileImage(validFile, USER_ID, request, AUTHORIZATION_CODE, SOURCE_EVENT_ID));

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

            profileImageService.deleteProfileImage(USER_ID, request, AUTHORIZATION_CODE, SOURCE_EVENT_ID);

            assertThat(Files.exists(image)).isFalse();
        }

        @Test
        void shouldSetUserPathToDefaultImageWhenDeletingExistingImage() throws Exception {
            Path image = Files.createTempFile(uploadDir, "avatar", ".png");
            user.setProfileImagePath(image.toString());
            stubUserFound();
            stubValidRequest();

            profileImageService.deleteProfileImage(USER_ID, request, AUTHORIZATION_CODE, SOURCE_EVENT_ID);

            assertThat(user.getProfileImagePath()).contains("UserProf.png");
        }

        @Test
        void shouldPersistUserWhenDeletingExistingImage() throws Exception {
            Path image = Files.createTempFile(uploadDir, "avatar", ".png");
            user.setProfileImagePath(image.toString());
            stubUserFound();
            stubValidRequest();

            profileImageService.deleteProfileImage(USER_ID, request, AUTHORIZATION_CODE, SOURCE_EVENT_ID);

            verify(userRepository).save(user);
        }

        @Test
        void shouldTrackAccountActivityWhenDeletingExistingImage() throws Exception {
            Path image = Files.createTempFile(uploadDir, "avatar", ".png");
            user.setProfileImagePath(image.toString());
            stubUserFound();
            stubValidRequest();

            profileImageService.deleteProfileImage(USER_ID, request, AUTHORIZATION_CODE, SOURCE_EVENT_ID);

            ArgumentCaptor<AccountChangesActivityEvent> captor =
                    ArgumentCaptor.forClass(AccountChangesActivityEvent.class);
            verify(kafkaTemplate).send(eq("account.changed"), captor.capture());
            assertThat(captor.getValue().type()).isEqualTo(AccountChangesActivityType.PROFILE_IMG_DELETED);
        }

        @Test
        void shouldGuardAgainstRiskWhenDeletingExistingImage() throws Exception {
            Path image = Files.createTempFile(uploadDir, "avatar", ".png");
            user.setProfileImagePath(image.toString());
            stubUserFound();
            stubValidRequest();

            profileImageService.deleteProfileImage(USER_ID, request, AUTHORIZATION_CODE, SOURCE_EVENT_ID);

            verify(riskGuardService).guard(USER_ID, RiskTriggerType.PROFILE_IMAGE_CHANGED, USER_EMAIL, SOURCE_EVENT_ID, request);
        }

        @Test
        void shouldNotDeleteExternalImageFileWhenDeletingExternalImage() {
            user.setProfileImagePath("https://cdn.example.com/avatar.png");
            stubUserFound();
            stubValidRequest();

            profileImageService.deleteProfileImage(USER_ID, request, AUTHORIZATION_CODE, SOURCE_EVENT_ID);

            assertThat(user.getProfileImagePath()).contains("UserProf.png");
            verify(userRepository).save(user);
        }

        @Test
        void shouldConfirmAdditionalAuthorizationCodeWhenDeletingExistingImage() throws Exception {
            Path image = Files.createTempFile(uploadDir, "avatar", ".png");
            user.setProfileImagePath(image.toString());
            stubUserFound();
            stubValidRequest();

            profileImageService.deleteProfileImage(USER_ID, request, AUTHORIZATION_CODE, SOURCE_EVENT_ID);

            verify(additionalAuthorizationCodeResolver).resolve(AUTHORIZATION_CODE);
            verify(additionalAuthorizationService).confirmAdditionalAuthorizationCode(USER_ID, resolvedAuthorizationCode);
        }

        @Test
        void shouldThrowIllegalArgumentExceptionWhenPathIsNull() {
            user.setProfileImagePath(null);
            stubUserFound();

            assertThrows(IllegalArgumentException.class, () ->
                    profileImageService.deleteProfileImage(USER_ID, request, AUTHORIZATION_CODE, SOURCE_EVENT_ID));

            verifyNoInteractions(riskGuardService);
            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldThrowIllegalArgumentExceptionWhenPathIsAlreadyDefault() {
            String defaultPath = defaultDir.resolve("UserProf.png").toString();
            user.setProfileImagePath(defaultPath);
            stubUserFound();

            assertThrows(IllegalArgumentException.class, () ->
                    profileImageService.deleteProfileImage(USER_ID, request, AUTHORIZATION_CODE, SOURCE_EVENT_ID));

            verifyNoInteractions(riskGuardService);
            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldThrowIllegalArgumentExceptionWhenAdditionalAuthorizationCodeConfirmationFails() {
            doThrow(new IllegalArgumentException("Invalid authorization code"))
                    .when(additionalAuthorizationService)
                    .confirmAdditionalAuthorizationCode(USER_ID, resolvedAuthorizationCode);

            assertThrows(IllegalArgumentException.class, () ->
                    profileImageService.deleteProfileImage(USER_ID, request, AUTHORIZATION_CODE, SOURCE_EVENT_ID));

            verifyNoInteractions(riskGuardService);
            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldThrowIllegalStateExceptionWhenRiskGuardFails() throws Exception {
            Path image = Files.createTempFile(uploadDir, "avatar", ".png");
            user.setProfileImagePath(image.toString());
            stubUserFound();
            doThrow(new IllegalStateException("Risk detected"))
                    .when(riskGuardService)
                    .guard(USER_ID, RiskTriggerType.PROFILE_IMAGE_CHANGED, USER_EMAIL, SOURCE_EVENT_ID, request);

            assertThrows(IllegalStateException.class, () ->
                    profileImageService.deleteProfileImage(USER_ID, request, AUTHORIZATION_CODE, SOURCE_EVENT_ID));

            verify(userRepository, never()).save(any());
        }
    }
}