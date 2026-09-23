package com.finovara.authservice.settings.account.controller;

import com.finovara.authservice.settings.account.service.profileimage.ProfileImageService;
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
    public void uploadProfileImage(@PathVariable Long userId, @RequestParam("file") MultipartFile file, HttpServletRequest request, @RequestParam(required = false) String authorizationCode, @RequestParam String sourceEventId) {
        profileImageService.uploadProfileImage(file, userId, request, authorizationCode, sourceEventId);
    }

    @DeleteMapping("/{userId}")
    public void deleteProfileImage(@PathVariable Long userId, HttpServletRequest request, @RequestParam(required = false) String authorizationCode, @RequestParam String sourceEventId) {
        profileImageService.deleteProfileImage(userId, request, authorizationCode, sourceEventId);
    }
}