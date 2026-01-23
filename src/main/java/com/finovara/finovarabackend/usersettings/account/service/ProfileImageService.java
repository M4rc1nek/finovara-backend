package com.finovara.finovarabackend.usersettings.account.service;

import com.finovara.finovarabackend.exception.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
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

    @Value("${application.upload.profile-images-directory}")
    private String profileImagesDirectory;

    @Transactional
    public void uploadProfileImage(MultipartFile file, Long userId) {
        User user = getUserByIdOrThrow(userId);
        validateFile(file);

        // Pobierz STARY plik PRZED zmianami (ważne!)
        String oldFilePath = user.getProfileImagePath();

        try {
            Path directory = Paths.get(profileImagesDirectory);
            Files.createDirectories(directory);

            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename(); // Losowa nazwa
            Path filePath = directory.resolve(filename); // Pełna ścieżka

            // Zapisz nowy plik
            Files.write(filePath, file.getBytes());

            // Zaktualizuj użytkownika (tylko raz)
            user.setProfileImagePath(filePath.toString());
            userRepository.save(user);

            // Teraz usuń STARY plik (jeśli istniał)
            if (oldFilePath != null) {
                Files.deleteIfExists(Paths.get(oldFilePath));
            }

        } catch (IOException exception) {
            throw new RuntimeException("Cannot save profile image", exception);
        }
    }

    //Dodaj walidacje ze nie moze usunac zdjecia prof jezeli go nie ma
    // gdy usune profilowe - natychmiastowe usuniecie, gdy dodam profilowe - natychmiastowe dodanie wszedzie.

    @Transactional
    public void deleteProfileImage(Long userId) {
        User user = getUserByIdOrThrow(userId);

        if (user.getProfileImagePath() == null) {
            throw new IllegalArgumentException("Profile image does not exist");
        }

        try {
            Files.deleteIfExists(Paths.get(user.getProfileImagePath()));
            user.setProfileImagePath(null);
            userRepository.save(user);

        } catch (IOException e) {
            throw new RuntimeException("Cannot delete profile image", e);
        }
    }

    private User getUserByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
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