package com.finovara.authservice.settings.account.controller;

import com.finovara.authservice.security.SecurityUtils;
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

    @PostMapping
    public void uploadProfileImage(@RequestParam("file") MultipartFile file, HttpServletRequest request, @RequestParam(required = false) String authorizationCode, @RequestParam String sourceEventId) {
        profileImageService.uploadProfileImage(file, SecurityUtils.getCurrentUserId(), request, authorizationCode, sourceEventId);
    }

    @DeleteMapping
    public void deleteProfileImage(HttpServletRequest request, @RequestParam(required = false) String authorizationCode, @RequestParam String sourceEventId) {
        profileImageService.deleteProfileImage(SecurityUtils.getCurrentUserId(), request, authorizationCode, sourceEventId);
    }
}