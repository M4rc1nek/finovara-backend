package com.finovara.authservice.security.oauth2.password;

import com.finovara.authservice.security.oauth2.dto.OAuth2PasswordDto;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.authservice.util.user.service.UserManagerService;
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