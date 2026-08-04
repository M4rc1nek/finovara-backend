package com.finovara.authservice.util.authorization.generator;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SecretGenerator {

    private static final String UNAMBIGUOUS_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%^&*";
    private static final String ALL = LOWER + UPPER + DIGITS + SPECIAL;

    private static final SecureRandom random = new SecureRandom();

    @Value("${secret-generator.password.length}")
    private int passwordLength;

    @Value("${secret-generator.additional-authorization-code.length}")
    private int additionalAuthorizationCodeLength;

    public int generateSecureCode() {
        return random.nextInt(900_000) + 100_000;
    }

    public String generateAdditionalAuthorizationCode() {
        if (additionalAuthorizationCodeLength < 8) {
            throw new InvalidInputException("Additional authorization code length must be at least 8");
        }

        StringBuilder code = new StringBuilder();
        for (int i = 0; i < additionalAuthorizationCodeLength; i++) {
            code.append(randomChar(UNAMBIGUOUS_CHARS));
        }
        return code.toString();
    }

    public String generatePassword() {
        if (passwordLength < 3) {
            throw new InvalidInputException("Password length must be at least 3");
        }
        List<Character> password = new ArrayList<>();

        password.add(randomChar(UPPER));
        password.add(randomChar(DIGITS));
        password.add(randomChar(SPECIAL));

        for (int i = password.size(); i < passwordLength; i++) {
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