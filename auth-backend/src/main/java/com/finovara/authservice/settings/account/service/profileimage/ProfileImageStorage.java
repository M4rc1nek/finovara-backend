package com.finovara.authservice.settings.account.service.profileimage;

import com.finovara.contracts.exception.serviceunavailable.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Component
public class ProfileImageStorage {

    @Value("${application.upload.profile-images-directory}")
    private String profileImagesDirectory;

    public Path save(MultipartFile file) {
        try {
            Path directory = Paths.get(profileImagesDirectory);
            Files.createDirectories(directory);

            Path target = directory.resolve(UUID.randomUUID() + extensionOf(file));
            file.transferTo(target);
            return target;
        } catch (IOException exception) {
            throw new ServiceUnavailableException("Cannot save profile image", exception);
        }
    }

    public void deleteOldImage(String profileImagePath) {
        if (isLocalPath(profileImagePath) && !isDefaultImage(profileImagePath)) {
            deleteQuietly(Paths.get(profileImagePath));
        }
    }

    public void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            log.warn("Could not delete profile image file: {}", path, exception);
        }
    }

    private String extensionOf(MultipartFile file) {
        String filename = StringUtils.cleanPath(String.valueOf(file.getOriginalFilename()));
        String extension = StringUtils.getFilenameExtension(filename);

        if (extension == null || !extension.matches("[A-Za-z0-9]{1,5}")) {
            return "";
        }
        return "." + extension.toLowerCase(Locale.ROOT);
    }

    private boolean isLocalPath(String profileImagePath) {
        return profileImagePath != null
                && !profileImagePath.startsWith("http://")
                && !profileImagePath.startsWith("https://");
    }

    private boolean isDefaultImage(String profileImagePath) {
        return profileImagePath.contains("UserProf.png");
    }
}