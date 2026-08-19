package com.finovara.authservice.settings.account.service.profileimage;

import com.finovara.contracts.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.activity.event.secure.accountchange.activity.AccountChangesActivityEvent;
import com.finovara.contracts.model.activity.AccountChangesActivityType;

import static com.finovara.contracts.clientdata.browser.UserBrowser.getBrowser;
import static com.finovara.contracts.clientdata.ip.ClientIp.getClientIpAddress;
import static com.finovara.contracts.clientdata.location.UserLocation.getLocationFromIp;
import com.finovara.contracts.exception.serviceunavailable.ServiceUnavailableException;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.authservice.settings.security.operationauthorization.service.AdditionalAuthorizationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileImageService {

    private final UserRepository userRepository;
    private final UserManagerService userManagerService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AdditionalAuthorizationService additionalAuthorizationService;
    private final AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;

    @Value("${application.upload.profile-images-directory}")
    private String profileImagesDirectory;

    @Value("${application.upload.profile-images-default-directory}")
    private String profileImagesDefaultDirectory;

    @Transactional
    public void uploadProfileImage(MultipartFile file, Long userId, HttpServletRequest request, String authorizationCode) {
        additionalAuthorizationService.confirmAdditionalAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(authorizationCode));
        
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
            publishActivity(user.getId(), AccountChangesActivityType.PROFILE_IMG_CHANGED, request);

            if (isLocalProfileImagePath(oldFilePath) && !isDefaultProfileImage(oldFilePath)) {
                Files.deleteIfExists(Paths.get(oldFilePath));
            }

        } catch (IOException exception) {
            throw new ServiceUnavailableException("Cannot save profile image", exception);
        }
    }

    @Transactional
    public void deleteProfileImage(Long userId, HttpServletRequest request, String authorizationCode) {
        additionalAuthorizationService.confirmAdditionalAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(authorizationCode));
        
        User user = userManagerService.getUserByIdOrThrow(userId);
        String currentPath = user.getProfileImagePath();

        if (currentPath == null || isDefaultProfileImage(currentPath)) {
            throw new IllegalArgumentException("Profile image does not exist or is already default");
        }

        try {
            if (isLocalProfileImagePath(currentPath)) {
                Files.deleteIfExists(Paths.get(currentPath));
            }

            String defaultPath = Paths.get(profileImagesDefaultDirectory).resolve("UserProf.png").toString();
            user.setProfileImagePath(defaultPath);

            publishActivity(userId, AccountChangesActivityType.PROFILE_IMG_DELETED, request);
            userRepository.save(user);

        } catch (IOException e) {
            throw new ServiceUnavailableException("Cannot delete profile image", e);
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
        if (file.getSize() > 5 * 1024 * 1024) { // 5MB
            throw new IllegalArgumentException("File is too large (max 5MB)");
        }
    }

    private boolean isLocalProfileImagePath(String profileImagePath) {
        return profileImagePath != null && !profileImagePath.startsWith("http://") && !profileImagePath.startsWith("https://");
    }

    private boolean isDefaultProfileImage(String profileImagePath) {
        return profileImagePath != null && profileImagePath.contains("UserProf.png");
    }

    private void publishActivity(Long userId, AccountChangesActivityType type, HttpServletRequest request) {
        String ipAddress = getClientIpAddress(request);
        kafkaTemplate.send("activity.account-changes", new AccountChangesActivityEvent(userId, type, getBrowser(request), ipAddress, getLocationFromIp(ipAddress), LocalDateTime.now()));
    }
}
