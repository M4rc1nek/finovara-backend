package com.finovara.finovarabackend.user.controller;

import com.finovara.finovarabackend.user.dto.UserLoginDto;
import com.finovara.finovarabackend.user.dto.UserRegisterDto;
import com.finovara.finovarabackend.user.service.GeneratePasswordService;
import com.finovara.finovarabackend.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/auth")
@RestController
public class UserController {

    private final UserService userService;
    private final GeneratePasswordService generatePasswordService;

    @PostMapping("/register")
    public ResponseEntity<UserRegisterDto> registerUser(@RequestBody @Valid UserRegisterDto userRegisterDto) {
        return ResponseEntity.ok(userService.registerUser(userRegisterDto));
    }

    @PostMapping("/login")
    public ResponseEntity<UserLoginDto> loginUser(@RequestBody @Valid UserLoginDto userLogin, HttpServletRequest request) {
        return ResponseEntity.ok(userService.loginUser(userLogin.email(), userLogin.password(), request));
    }

    @PostMapping("/generatePassword")
    public ResponseEntity<String> generatePasswordForUser() {
        return ResponseEntity.ok(generatePasswordService.generatePassword());
    }
}

