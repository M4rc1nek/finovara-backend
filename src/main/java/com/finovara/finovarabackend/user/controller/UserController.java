package com.finovara.finovarabackend.user.controller;

import com.finovara.finovarabackend.user.dto.UserRegisterLoginDTO;
import com.finovara.finovarabackend.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserRegisterLoginDTO> registerUser(@RequestBody @Valid UserRegisterLoginDTO userRegisterLoginDTO) {
        return ResponseEntity.ok(userService.registerUser(userRegisterLoginDTO));
    }

    @PostMapping("/login")
    public ResponseEntity<UserRegisterLoginDTO> loginUser(@RequestBody @Valid UserRegisterLoginDTO userLogin) {
        return ResponseEntity.ok(userService.loginUser(userLogin.email(), userLogin.password()));
    }
}

