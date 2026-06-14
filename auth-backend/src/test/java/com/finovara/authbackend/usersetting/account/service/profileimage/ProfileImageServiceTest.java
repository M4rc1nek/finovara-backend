package com.finovara.authbackend.usersetting.account.service.profileimage;

import com.finovara.contracts.event.activity.secure.accountchange.activity.AccountChangesActivityEvent;
import com.finovara.contracts.model.activity.AccountChangesActivityType;
import com.finovara.authbackend.user.model.User;
import com.finovara.authbackend.user.repository.UserRepository;
import com.finovara.authbackend.util.user.service.UserManagerService;
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
    private HttpServletRequest request;

    @InjectMocks
    private ProfileImageService profileImageService;

    @TempDir
    Path uploadDir;
    @TempDir
    Path defaultDir;

    private User user;
    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() throws Exception {
        user = new User();
        user.setId(USER_ID);
        user.setEmail("test@test.com");

        Files.createFile(defaultDir.resolve("UserProf.png"));

        ReflectionTestUtils.setField(profileImageService, "profileImagesDirectory", uploadDir.toString());
        ReflectionTestUtils.setField(profileImageService, "profileImagesDefaultDirectory", defaultDir.toString());

        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
    }

    @Nested
    class UploadProfileImage {

        private final MockMultipartFile validFile =
                new MockMultipartFile("file", "avatar.png", "image/png", "image-data".getBytes());

        @Test
        void shouldSaveFileOnDiskAndUpdateUserPath() {
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0");

            profileImageService.uploadProfileImage(validFile, USER_ID, request);

            assertThat(user.getProfileImagePath()).isNotNull();
            assertThat(Files.exists(Path.of(user.getProfileImagePath()))).isTrue();
        }

        @Test
        void shouldPersistUserAfterUpload() {
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0");

            profileImageService.uploadProfileImage(validFile, USER_ID, request);

            verify(userRepository).save(user);
        }

        @Test
        void shouldTrackAccountActivityAfterUpload() {
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0");

            profileImageService.uploadProfileImage(validFile, USER_ID, request);

            ArgumentCaptor<AccountChangesActivityEvent> captor =
                    ArgumentCaptor.forClass(AccountChangesActivityEvent.class);

            verify(kafkaTemplate).send(eq("activity.account-changes"), captor.capture());

            assertThat(captor.getValue().type())
                    .isEqualTo(AccountChangesActivityType.PROFILE_IMG_CHANGED);
        }

        @Test
        void shouldGenerateUniqueFilenameContainingOriginalName() {
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0");

            profileImageService.uploadProfileImage(validFile, USER_ID, request);

            assertThat(user.getProfileImagePath()).contains("avatar.png");
        }

        @Test
        void shouldDeleteOldLocalImageAfterUpload() throws Exception {
            Path oldFile = Files.createTempFile(uploadDir, "old-avatar", ".png");
            user.setProfileImagePath(oldFile.toString());

            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0");

            profileImageService.uploadProfileImage(validFile, USER_ID, request);

            assertThat(Files.exists(oldFile)).isFalse();
        }

        @Test
        void shouldNotDeleteExternalImageAfterUpload() {
            user.setProfileImagePath("https://cdn.example.com/avatar.png");

            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0");

            profileImageService.uploadProfileImage(validFile, USER_ID, request);

            verify(userRepository).save(user);
        }

        @Test
        void shouldUploadJpegFile() {
            MockMultipartFile jpeg =
                    new MockMultipartFile("file", "photo.jpg", "image/jpeg", "jpeg-data".getBytes());

            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0");

            profileImageService.uploadProfileImage(jpeg, USER_ID, request);

            assertThat(user.getProfileImagePath()).contains("photo.jpg");
        }

        @Test
        void shouldAcceptFileExactlyAt5MB() {
            MockMultipartFile exact =
                    new MockMultipartFile("file", "exact.png", "image/png", new byte[5 * 1024 * 1024]);

            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0");

            profileImageService.uploadProfileImage(exact, USER_ID, request);

            verify(userRepository).save(user);
        }

        @Test
        void shouldThrowWhenFileIsEmpty() {
            MockMultipartFile empty =
                    new MockMultipartFile("file", "avatar.png", "image/png", new byte[0]);

            assertThatThrownBy(() ->
                    profileImageService.uploadProfileImage(empty, USER_ID, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("File is empty");

            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenFileIsNotAnImage() {
            MockMultipartFile pdf =
                    new MockMultipartFile("file", "document.pdf", "application/pdf", "data".getBytes());

            assertThatThrownBy(() ->
                    profileImageService.uploadProfileImage(pdf, USER_ID, request))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenFileSizeExceeds5MB() {
            MockMultipartFile large =
                    new MockMultipartFile("file", "large.png", "image/png", new byte[6 * 1024 * 1024]);

            assertThatThrownBy(() ->
                    profileImageService.uploadProfileImage(large, USER_ID, request))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    class DeleteProfileImage {

        // request stubbing moved into individual tests to avoid unnecessary stubbing in tests

        @Test
        void shouldDeleteLocalFileFromDisk() throws Exception {
            Path image = Files.createTempFile(uploadDir, "avatar", ".png");
            user.setProfileImagePath(image.toString());
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0");

            profileImageService.deleteProfileImage(USER_ID, request);

            assertThat(Files.exists(image)).isFalse();
        }

        @Test
        void shouldSetUserPathToDefaultImageAfterDelete() throws Exception {
            Path image = Files.createTempFile(uploadDir, "avatar", ".png");
            user.setProfileImagePath(image.toString());
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0");

            profileImageService.deleteProfileImage(USER_ID, request);

            assertThat(user.getProfileImagePath()).contains("UserProf.png");
        }

        @Test
        void shouldPersistUserAfterDelete() throws Exception {
            Path image = Files.createTempFile(uploadDir, "avatar", ".png");
            user.setProfileImagePath(image.toString());
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0");

            profileImageService.deleteProfileImage(USER_ID, request);

            verify(userRepository).save(user);
        }

        @Test
        void shouldTrackAccountActivityAfterDelete() throws Exception {
            Path image = Files.createTempFile(uploadDir, "avatar", ".png");
            user.setProfileImagePath(image.toString());

            ArgumentCaptor<AccountChangesActivityEvent> captor =
                    ArgumentCaptor.forClass(AccountChangesActivityEvent.class);
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0");

            profileImageService.deleteProfileImage(USER_ID, request);

            verify(kafkaTemplate).send(eq("activity.account-changes"), captor.capture());

            assertThat(captor.getValue().type())
                    .isEqualTo(AccountChangesActivityType.PROFILE_IMG_DELETED);
        }

        @Test
        void shouldThrowWhenPathIsNull() {
            user.setProfileImagePath(null);

            assertThatThrownBy(() ->
                    profileImageService.deleteProfileImage(USER_ID, request))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(userRepository, never()).save(any());
        }
    }
}