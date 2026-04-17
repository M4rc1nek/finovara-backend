package com.finovara.finovarabackend.usersetting.account.service;

import com.finovara.finovarabackend.accountactivity.secure.accountchange.activity.model.AccountChangesActivityType;
import com.finovara.finovarabackend.accountactivity.secure.accountchange.activity.service.AccountChangesActivityService;
import com.finovara.finovarabackend.exception.serviceunavailable.ServiceUnavailableException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileImageService {

    private final UserRepository userRepository;
    private final UserManagerService userManagerService;

    private final AccountChangesActivityService accountChangesActivityService;

    @Value("${application.upload.profile-images-directory}")
    private String profileImagesDirectory;

    @Transactional
    public void uploadProfileImage(MultipartFile file, Long userId, HttpServletRequest request) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        validateFile(file);

        String oldFilePath = user.getProfileImagePath();

        try {
            Path directory = Paths.get(profileImagesDirectory);
            Files.createDirectories(directory);

            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = directory.resolve(filename);

            Files.write(filePath, file.getBytes());

            user.setProfileImagePath(filePath.toString());
            userRepository.save(user);
            accountChangesActivityService.createAccountChangesActivity(user.getEmail(), AccountChangesActivityType.PROFILE_IMG_CHANGED, request);


            if (oldFilePath != null) {
                Files.deleteIfExists(Paths.get(oldFilePath));
            }

        } catch (IOException exception) {
            throw new ServiceUnavailableException("Cannot save profile image", exception);
        }
    }

    @Transactional
    public void deleteProfileImage(Long userId) {
        User user = userManagerService.getUserByIdOrThrow(userId);

        if (user.getProfileImagePath() == null) {
            throw new IllegalArgumentException("Profile image does not exist");
        }

        try {
            Files.deleteIfExists(Paths.get(user.getProfileImagePath()));
            user.setProfileImagePath(null);
            userRepository.save(user);

        } catch (IOException e) {
            throw new ServiceUnavailableException("Cannot delete profile image", e);
        }
    }


    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (!file.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("File is not an image");
        }
        // Dodaję limit rozmiaru (opcjonalne, ale dobre)
        if (file.getSize() > 5 * 1024 * 1024) { // 5MB
            throw new IllegalArgumentException("File is too large (max 5MB)");
        }
    }
}