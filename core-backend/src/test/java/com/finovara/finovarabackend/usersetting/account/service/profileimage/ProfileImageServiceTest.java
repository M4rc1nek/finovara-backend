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

        private final MockMultipartFile validFile = new MockMultipartFile("file", "avatar.png", "image/png", "image-data".getBytes());

        @Test
        void shouldSaveFileOnDiskAndUpdateUserPath() {
            profileImageService.uploadProfileImage(validFile, USER_ID, request);

            assertThat(user.getProfileImagePath()).isNotNull();
            assertThat(Files.exists(Path.of(user.getProfileImagePath()))).isTrue();
        }

        @Test
        void shouldPersistUserAfterUpload() {
            profileImageService.uploadProfileImage(validFile, USER_ID, request);

            verify(userRepository).save(user);
        }

        @Test
        void shouldTrackAccountActivityAfterUpload() {
            profileImageService.uploadProfileImage(validFile, USER_ID, request);

            verify(accountChangesActivityService).createAccountChangesActivity(USER_ID, AccountChangesActivityType.PROFILE_IMG_CHANGED, request);
        }

        @Test
        void shouldGenerateUniqueFilenameContainingOriginalName() {
            profileImageService.uploadProfileImage(validFile, USER_ID, request);

            assertThat(user.getProfileImagePath()).contains("avatar.png");
        }

        @Test
        void shouldDeleteOldLocalImageAfterUpload() throws Exception {
            Path oldFile = Files.createTempFile(uploadDir, "old-avatar", ".png");
            user.setProfileImagePath(oldFile.toString());

            profileImageService.uploadProfileImage(validFile, USER_ID, request);

            assertThat(Files.exists(oldFile)).isFalse();
        }

        @Test
        void shouldNotDeleteOldExternalImageAfterUpload() {
            user.setProfileImagePath("https://cdn.example.com/avatar.png");

            profileImageService.uploadProfileImage(validFile, USER_ID, request);

            verify(userRepository).save(user);
        }

        @Test
        void shouldNotDeleteDefaultProfileImageOnUpload() {
            user.setProfileImagePath(defaultDir.resolve("UserProf.png").toString());

            profileImageService.uploadProfileImage(validFile, USER_ID, request);

            assertThat(Files.exists(defaultDir.resolve("UserProf.png"))).isTrue();
        }

        @Test
        void shouldUploadJpegFile() {
            MockMultipartFile jpeg = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "jpeg-data".getBytes());

            profileImageService.uploadProfileImage(jpeg, USER_ID, request);

            assertThat(user.getProfileImagePath()).contains("photo.jpg");
        }

        @Test
        void shouldAcceptFileExactlyAt5MB() {
            MockMultipartFile exactFile = new MockMultipartFile("file", "exact.png", "image/png",
                    new byte[5 * 1024 * 1024]);

            profileImageService.uploadProfileImage(exactFile, USER_ID, request);

            verify(userRepository).save(user);
        }

        @Test
        void shouldThrowWhenFileIsEmpty() {
            MockMultipartFile emptyFile = new MockMultipartFile("file", "avatar.png", "image/png", new byte[0]);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> profileImageService.uploadProfileImage(emptyFile, USER_ID, request));

            assertThat(ex.getMessage()).isEqualTo("File is empty");
            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenFileIsNotAnImage() {
            MockMultipartFile pdf = new MockMultipartFile("file", "document.pdf", "application/pdf", "data".getBytes());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> profileImageService.uploadProfileImage(pdf, USER_ID, request));

            assertThat(ex.getMessage()).isEqualTo("File is not an image");
            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenFileHasNullContentType() {
            MockMultipartFile nullType = new MockMultipartFile("file", "avatar.png", null, "data".getBytes());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> profileImageService.uploadProfileImage(nullType, USER_ID, request));

            assertThat(ex.getMessage()).isEqualTo("File is not an image");
            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenFileSizeExceeds5MB() {
            MockMultipartFile largeFile = new MockMultipartFile("file", "large.png", "image/png",
                    new byte[6 * 1024 * 1024]);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    profileImageService.uploadProfileImage(largeFile, USER_ID, request));

            assertThat(ex.getMessage()).isEqualTo("File is too large (max 5MB)");
            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldNotSaveFileWhenValidationFails() {
            MockMultipartFile badFile = new MockMultipartFile("file", "bad.exe",
                    "application/octet-stream", "bad".getBytes());

            assertThrows(IllegalArgumentException.class, () -> profileImageService.uploadProfileImage(badFile, USER_ID, request));

            assertThat(uploadDir.toFile().listFiles()).isEmpty();
        }
    }

    @Nested
    class DeleteProfileImage {
        @Test
        void shouldDeleteLocalFileFromDisk() throws Exception {
            Path imageFile = Files.createTempFile(uploadDir, "avatar", ".png");
            user.setProfileImagePath(imageFile.toString());

            profileImageService.deleteProfileImage(USER_ID, request);

            assertThat(Files.exists(imageFile)).isFalse();
        }

        @Test
        void shouldSetUserPathToDefaultImageAfterDelete() throws Exception {
            Path imageFile = Files.createTempFile(uploadDir, "avatar", ".png");
            user.setProfileImagePath(imageFile.toString());

            profileImageService.deleteProfileImage(USER_ID, request);

            assertThat(user.getProfileImagePath()).contains("UserProf.png");
        }

        @Test
        void shouldPersistUserAfterDelete() throws Exception {
            Path imageFile = Files.createTempFile(uploadDir, "avatar", ".png");
            user.setProfileImagePath(imageFile.toString());

            profileImageService.deleteProfileImage(USER_ID, request);

            verify(userRepository).save(user);
        }

        @Test
        void shouldTrackAccountActivityAfterDelete() throws Exception {
            Path imageFile = Files.createTempFile(uploadDir, "avatar", ".png");
            user.setProfileImagePath(imageFile.toString());

            profileImageService.deleteProfileImage(USER_ID, request);

            verify(accountChangesActivityService).createAccountChangesActivity(USER_ID, AccountChangesActivityType.PROFILE_IMG_DELETED, request);
        }

        @Test
        void shouldSetPathToDefaultWhenDeletingExternalImage() {
            user.setProfileImagePath("https://cdn.example.com/avatar.png");

            profileImageService.deleteProfileImage(USER_ID, request);

            assertThat(user.getProfileImagePath()).contains("UserProf.png");
            verify(userRepository).save(user);
        }

        @Test
        void shouldHandleExternalHttpImageWithoutFileDeletion() {
            user.setProfileImagePath("http://cdn.example.com/avatar.png");

            profileImageService.deleteProfileImage(USER_ID, request);

            verify(userRepository).save(user);
        }

        @Test
        void shouldNotFailWhenLocalFileMissingOnDisk() {
            user.setProfileImagePath(uploadDir.resolve("ghost-avatar.png").toString());

            profileImageService.deleteProfileImage(USER_ID, request);

            verify(userRepository).save(user);
        }

        @Test
        void shouldThrowWhenProfileImagePathIsNull() {
            user.setProfileImagePath(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    profileImageService.deleteProfileImage(USER_ID, request));

            assertThat(ex.getMessage()).isEqualTo("Profile image does not exist or is already default");
            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenUserAlreadyHasDefaultProfileImage() {
            user.setProfileImagePath(defaultDir.resolve("UserProf.png").toString());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    profileImageService.deleteProfileImage(USER_ID, request));

            assertThat(ex.getMessage()).isEqualTo("Profile image does not exist or is already default");
            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenPathContainsUserProfPng() {
            user.setProfileImagePath("/some/path/UserProf.png");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    profileImageService.deleteProfileImage(USER_ID, request));

            assertThat(ex.getMessage()).isEqualTo("Profile image does not exist or is already default");
            verify(userRepository, never()).save(any());
        }
    }
}