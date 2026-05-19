package com.finovara.finovarabackend.usersetting.account.service.profileimage;

import com.finovara.finovarabackend.accountactivity.secure.accountchange.activity.model.AccountChangesActivityType;
import com.finovara.finovarabackend.accountactivity.secure.accountchange.activity.service.AccountChangesActivityService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileImageServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private AccountChangesActivityService accountChangesActivityService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private ProfileImageService profileImageService;

    @TempDir
    Path tempDirectory;

    private User user;
    private Long userId;

    @BeforeEach
    void setUp() {
        userId = 1L;

        user = new User();
        user.setId(userId);
        user.setEmail("test@test.com");

        ReflectionTestUtils.setField(profileImageService, "profileImagesDirectory", tempDirectory.toString());

        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
    }

    @Nested
    class UploadProfileImage {

        @Test
        void shouldUploadProfileImage() {
            MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", "image-data".getBytes());

            profileImageService.uploadProfileImage(file, userId, request);

            assertThat(user.getProfileImagePath()).isNotNull();
            assertThat(Files.exists(Path.of(user.getProfileImagePath()))).isTrue();

            verify(userRepository).save(user);

            verify(accountChangesActivityService).createAccountChangesActivity(user.getId(), AccountChangesActivityType.PROFILE_IMG_CHANGED, request);
        }

        @Test
        void shouldDeleteOldProfileImageBeforeUpload() throws Exception {
            Path oldFile = Files.createTempFile(tempDirectory, "old-avatar", ".png");

            user.setProfileImagePath(oldFile.toString());

            MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", "image-data".getBytes());

            profileImageService.uploadProfileImage(file, userId, request);

            assertThat(Files.exists(oldFile)).isFalse();
        }

        @Test
        void shouldNotDeleteExternalImageUrl() {
            user.setProfileImagePath("https://cdn.test.com/avatar.png");

            MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", "image-data".getBytes());

            profileImageService.uploadProfileImage(file, userId, request);

            assertThat(user.getProfileImagePath()).doesNotContain("cdn.test.com");

            verify(userRepository).save(user);
        }

        @Test
        void shouldThrowExceptionWhenFileIsEmpty() {
            MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[0]);

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> profileImageService.uploadProfileImage(file, userId, request));

            assertThat(exception.getMessage()).isEqualTo("File is empty");

            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldThrowExceptionWhenFileIsNotImage() {
            MockMultipartFile file = new MockMultipartFile("file", "document.pdf", "application/pdf", "data".getBytes());

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> profileImageService.uploadProfileImage(file, userId, request));

            assertThat(exception.getMessage()).isEqualTo("File is not an image");

            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldThrowExceptionWhenFileIsTooLarge() {
            byte[] largeFile = new byte[6 * 1024 * 1024];

            MockMultipartFile file = new MockMultipartFile("file", "large-image.png", "image/png", largeFile);

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> profileImageService.uploadProfileImage(file, userId, request));

            assertThat(exception.getMessage()).isEqualTo("File is too large (max 5MB)");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    class DeleteProfileImage {

        @Test
        void shouldDeleteProfileImage(@TempDir Path tempDir) throws Exception {

            Path file = Files.createTempFile(tempDir, "avatar", ".png");

            user.setProfileImagePath(file.toString());

            profileImageService.deleteProfileImage(userId, request);

            assertThat(user.getProfileImagePath()).isNull();
            assertThat(Files.exists(file)).isFalse();

            verify(userRepository).save(user);

            verify(accountChangesActivityService).createAccountChangesActivity(userId, AccountChangesActivityType.PROFILE_IMG_DELETED, request);
        }

        @Test
        void shouldDeleteOnlyDatabaseReferenceForExternalImage() {
            user.setProfileImagePath("https://cdn.test.com/avatar.png");

            profileImageService.deleteProfileImage(userId, request);

            assertThat(user.getProfileImagePath()).isNull();

            verify(userRepository).save(user);

            verify(accountChangesActivityService).createAccountChangesActivity(userId, AccountChangesActivityType.PROFILE_IMG_DELETED, request);
        }

        @Test
        void shouldThrowExceptionWhenNoProfileImage() {
            user.setProfileImagePath(null);

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> profileImageService.deleteProfileImage(userId, request));

            assertThat(exception.getMessage()).isEqualTo("Profile image does not exist");

            verify(userRepository, never()).save(any());
        }
    }
}