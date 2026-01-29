package com.finovara.finovarabackend.user.service;

import com.finovara.finovarabackend.exception.EmailAlreadyExistsException;
import com.finovara.finovarabackend.exception.NameAlreadyExistsException;
import com.finovara.finovarabackend.exception.UserNotFoundException;
import com.finovara.finovarabackend.exception.WrongPasswordException;
import com.finovara.finovarabackend.security.service.JwtService;
import com.finovara.finovarabackend.user.dto.UserRegisterLoginDTO;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public UserRegisterLoginDTO registerUser(UserRegisterLoginDTO userRegisterLoginDTO) {

        if (userRepository.existsByUsername(userRegisterLoginDTO.username())) {
            throw new NameAlreadyExistsException("Username is already taken");
        }

        if (userRepository.existsByEmail(userRegisterLoginDTO.email())) {
            throw new EmailAlreadyExistsException("Email is already taken");
        }

        User user = User.builder()
                .username(userRegisterLoginDTO.username())
                .email(userRegisterLoginDTO.email())
                .password(passwordEncoder.encode(userRegisterLoginDTO.password()))
                .createdAt(LocalDateTime.now())
                .build();
        User savedUser = userRepository.save(user);

        String jwtToken = jwtService.generateToken(
                new org.springframework.security.core.userdetails.User(
                        savedUser.getEmail(),
                        savedUser.getPassword(),
                        List.of()
                )
        );

        return new UserRegisterLoginDTO(user.getId(), savedUser.getUsername(), null, savedUser.getEmail(), jwtToken);

    }

    public UserRegisterLoginDTO loginUser(String email, String rawPassword) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, rawPassword)
            );
        } catch (AuthenticationException e) {
            throw new WrongPasswordException("Incorrect email or password");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                List.of()
        );
        String jwtToken = jwtService.generateToken(userDetails);

        return new UserRegisterLoginDTO(user.getId(), user.getUsername(), null, user.getEmail(), jwtToken);
    }

}
