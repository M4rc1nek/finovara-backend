package com.finovara.finovarabackend.usersetting.account.service.profileimage.service;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.usersetting.account.service.profileimage.ProfileImageService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteProfileImageTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserManagerService userManagerService;

    @InjectMocks
    private ProfileImageService profileImageService;

    private final Long USER_ID = 1L;

    @Test
    void shouldDeleteProfileImage(@TempDir Path tempDir) throws Exception {

        Path file = Files.createTempFile(tempDir, "avatar", ".png");

        User user = new User();
        user.setId(USER_ID);
        user.setProfileImagePath(file.toString());

        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

        profileImageService.deleteProfileImage(USER_ID);

        assertThat(user.getProfileImagePath()).isNull();
        assertThat(Files.exists(file)).isFalse();

        verify(userRepository).save(user);
    }

    @Test
    void shouldThrowExceptionWhenNoProfileImage() {

        User user = new User();
        user.setId(USER_ID);
        user.setProfileImagePath(null);

        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

        assertThrows(IllegalArgumentException.class, () -> profileImageService.deleteProfileImage(user.getId()));

        verify(userRepository, never()).save(any());
    }
}