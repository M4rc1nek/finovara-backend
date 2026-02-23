package com.finovara.finovarabackend.usersetting.account.controller;

import com.finovara.finovarabackend.usersetting.account.service.ProfileImageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile-image")
@RequiredArgsConstructor
public class ProfileImageController {

    private final ProfileImageService profileImageService;

    @PostMapping("/{userId}")
    public void uploadProfileImage(@PathVariable Long userId, @RequestParam("file") MultipartFile file, HttpServletRequest request) {
        profileImageService.uploadProfileImage(file, userId, request);
    }

    @DeleteMapping("/{userId}")
    public void deleteProfileImage(@PathVariable Long userId) {
        profileImageService.deleteProfileImage(userId);
    }
}