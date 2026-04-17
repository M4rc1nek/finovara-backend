package com.finovara.finovarabackend.usersetting.accountsetting.profileimage.service;

import com.finovara.finovarabackend.accountactivity.security.accountchange.activities.model.AccountChangesActivityType;
import com.finovara.finovarabackend.accountactivity.security.accountchange.activities.service.AccountChangesActivityService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.usersetting.account.service.ProfileImageService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadProfileImageTest {

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

    private final Long USER_ID = 1L;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(profileImageService, "profileImagesDirectory", tempDirectory.toString());
    }

    @Test
    void shouldUploadProfileImage(){

        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", "image-data".getBytes());

        User user = new User();
        user.setId(USER_ID);
        user.setEmail("test@test.com");

        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

        profileImageService.uploadProfileImage(file, USER_ID, request);

        assertThat(user.getProfileImagePath()).isNotNull();
        assertThat(Files.exists(Path.of(user.getProfileImagePath()))).isTrue();

        verify(userRepository).save(user);

        verify(accountChangesActivityService).createAccountChangesActivity(user.getEmail(), AccountChangesActivityType.PROFILE_IMG_CHANGED, request);
    }

    @Test
    void shouldDeleteOldProfileImage() throws Exception {

        Path oldFile = Files.createTempFile(tempDirectory, "old-avatar", ".png");

        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", "image-data".getBytes());

        User user = new User();
        user.setId(USER_ID);
        user.setEmail("test@test.com");
        user.setProfileImagePath(oldFile.toString());

        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

        profileImageService.uploadProfileImage(file, USER_ID, request);

        assertThat(Files.exists(oldFile)).isFalse();
    }
}