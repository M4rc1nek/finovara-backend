package com.finovara.finovarabackend.usersettings.account.controller;

import com.finovara.finovarabackend.usersettings.account.service.ProfileImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile-image")
@RequiredArgsConstructor
public class ProfileImageController {

    private final ProfileImageService profileImageService;

    @PostMapping("/{userId}")
    public void uploadProfileImage(@PathVariable Long userId, @RequestParam("file") MultipartFile file) {
        profileImageService.uploadProfileImage(file, userId);
    }

    @DeleteMapping("/{userId}")
    public void deleteProfileImage(@PathVariable Long userId) {
        profileImageService.deleteProfileImage(userId);
    }
}