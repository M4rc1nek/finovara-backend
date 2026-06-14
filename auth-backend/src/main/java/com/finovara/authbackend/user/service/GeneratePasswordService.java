package com.finovara.authbackend.user.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeneratePasswordService {

    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%^&*";
    private static final String ALL = LOWER + UPPER + DIGITS + SPECIAL;

    @Value("${password-generator.length}")
    private int length;

    private static final SecureRandom random = new SecureRandom();

    public String generatePassword() {
        if (length < 3) {
            throw new InvalidInputException("Password length must be at least 3");
        }
        List<Character> password = new ArrayList<>();

        password.add(randomChar(UPPER));
        password.add(randomChar(DIGITS));
        password.add(randomChar(SPECIAL));

        for (int i = password.size(); i < length; i++) {
            password.add(randomChar(ALL));
        }

        Collections.shuffle(password, random);

        StringBuilder stringBuilder = new StringBuilder();
        for (char character : password) {
            stringBuilder.append(character);
        }

        return stringBuilder.toString();
    }

    private static char randomChar(String source) {
        int index = random.nextInt(source.length());
        return source.charAt(index);
    }
}