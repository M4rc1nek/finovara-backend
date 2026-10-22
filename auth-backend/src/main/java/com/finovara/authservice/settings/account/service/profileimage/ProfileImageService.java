package com.finovara.authservice.settings.account.service.profileimage;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class ProfileImageService {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;

    private final ProfileImageStorage profileImageStorage;
    private final ProfileImageUpdateService profileImageUpdateService;

    public void uploadProfileImage(MultipartFile file, Long userId, HttpServletRequest request,
                                   String authorizationCode, String sourceEventId) {
        validateFile(file);

        Path newImage = profileImageStorage.save(file);
        String previousImagePath = updateImagePathOrDiscardFile(newImage, userId, request, authorizationCode, sourceEventId);

        profileImageStorage.deleteOldImage(previousImagePath);
    }

    public void deleteProfileImage(Long userId, HttpServletRequest request, String authorizationCode, String sourceEventId) {
        String previousImagePath = profileImageUpdateService.resetProfileImage(userId, request, authorizationCode, sourceEventId);

        profileImageStorage.deleteOldImage(previousImagePath);
    }

    private String updateImagePathOrDiscardFile(Path newImage, Long userId, HttpServletRequest request, String authorizationCode, String sourceEventId) {
        try {
            return profileImageUpdateService.replaceProfileImage(userId, newImage.toString(), request, authorizationCode, sourceEventId);
        } catch (RuntimeException exception) {
            profileImageStorage.deleteQuietly(newImage);
            throw exception;
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File is not an image");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File is too large (max 5MB)");
        }
    }
}