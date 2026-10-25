package com.finovara.authservice.settings.account.service.profileimage;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileImageServiceTest {

    @Mock
    private ProfileImageStorage storage;
    @Mock
    private ProfileImageUpdateService updateService;
    @Mock
    private MultipartFile file;
    @Mock
    private HttpServletRequest request;
    @InjectMocks
    private ProfileImageService service;

    @Nested
    class UploadProfileImage {
        @BeforeEach
        void setUp() {
            when(file.isEmpty()).thenReturn(false);
            when(file.getContentType()).thenReturn("image/png");
            when(file.getSize()).thenReturn(5L);
        }

        @Test
        void shouldSaveReplaceAndDeletePreviousImageWhenFileIsValid() {
            Path path = Path.of("new.png");
            when(storage.save(file)).thenReturn(path);
            when(updateService.replaceProfileImage(1L, path.toString(), request, "code", "event")).thenReturn("old.png");

            service.uploadProfileImage(file, 1L, request, "code", "event");

            verify(storage).deleteOldImage("old.png");
        }

        @Test
        void shouldThrowExceptionWhenFileIsLargerThanFiveMegabytes() {
            when(file.getSize()).thenReturn(5L * 1024 * 1024 + 1);

            assertThrows(IllegalArgumentException.class, () -> service.uploadProfileImage(file, 1L, request, "code", "event"));
            verifyNoInteractions(storage, updateService);
        }

        @Test
        void shouldDeleteNewFileWhenReplacingProfileImageFails() {
            Path path = Path.of("new.png");
            when(storage.save(file)).thenReturn(path);
            when(updateService.replaceProfileImage(1L, path.toString(), request, "code", "event")).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> service.uploadProfileImage(file, 1L, request, "code", "event"));
            verify(storage).deleteQuietly(path);
            verify(storage, never()).deleteOldImage("old.png");
        }
    }

    @Nested
    class DeleteProfileImage {
        @Test
        void shouldDeletePreviousImageWhenResetSucceeds() {
            when(updateService.resetProfileImage(1L, request, "code", "event")).thenReturn("old.png");

            service.deleteProfileImage(1L, request, "code", "event");

            verify(storage).deleteOldImage("old.png");
        }
    }
}
