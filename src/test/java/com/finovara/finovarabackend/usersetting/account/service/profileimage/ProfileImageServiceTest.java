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
    }

    @Nested
    class DeleteProfileImage {
        @Test
        void shouldDeleteProfileImage(@TempDir Path tempDir) throws Exception {
            Path file = Files.createTempFile(tempDir, "avatar", ".png");
            user.setProfileImagePath(file.toString());

            profileImageService.deleteProfileImage(userId);

            assertThat(user.getProfileImagePath()).isNull();
            assertThat(Files.exists(file)).isFalse();

            verify(userRepository).save(user);
        }

        @Test
        void shouldThrowExceptionWhenNoProfileImage() {
            user.setProfileImagePath(null);

            assertThrows(IllegalArgumentException.class, () -> profileImageService.deleteProfileImage(userId));

            verify(userRepository, never()).save(any());
        }
    }
}