package com.finovara.authbackend.security.oauth2.password;

import com.finovara.authbackend.security.oauth2.dto.OAuth2PasswordDto;
import com.finovara.authbackend.user.model.User;
import com.finovara.authbackend.user.repository.UserRepository;
import com.finovara.authbackend.util.user.service.UserManagerService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OAuth2PasswordSetter {
    private final UserManagerService userManagerService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Transactional
    public void createPassword(Long userId, OAuth2PasswordDto dto) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setPasswordSet(true);
        userRepository.save(user);
    }
}