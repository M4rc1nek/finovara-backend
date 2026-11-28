package com.finovara.authservice.settings.security.twofactor.service.code;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.exceptions.CodeGenerationException;
import dev.samstevens.totp.recovery.RecoveryCodeGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Arrays;
import java.util.stream.LongStream;

@Service
@RequiredArgsConstructor
public class AuthenticatorCredentialService {

    private static final int TIME_STEP_SECONDS = 30;

    private final DefaultSecretGenerator secretGenerator = new DefaultSecretGenerator(32);
    private final CodeGenerator codeGenerator = new DefaultCodeGenerator();
    private final DefaultCodeVerifier codeVerifier = new DefaultCodeVerifier(codeGenerator, new SystemTimeProvider());
    private final RecoveryCodeGenerator recoveryCodeGenerator = new RecoveryCodeGenerator();
    private final TwoFactorSecretEncryption secretEncryption;
    private final PasswordEncoder backupCodeEncoder;

    public String generateAuthenticatorSecret() {
        return secretGenerator.generate();
    }

    public String createAuthenticatorSetupUri(String secret, String email) {
        String issuer = "Finovara";
        String label = URLEncoder.encode(issuer + ":" + email, StandardCharsets.UTF_8).replace("+", "%20");
        return "otpauth://totp/" + label
                + "?secret=" + secret
                + "&issuer=" + URLEncoder.encode(issuer, StandardCharsets.UTF_8)
                + "&algorithm=SHA1&digits=6&period=" + TIME_STEP_SECONDS;
    }

    public Long findCodeTimeStep(String secret, String code, long unixTimeSeconds) {
        if (code == null || !code.matches("\\d{6}") || !codeVerifier.isValidCode(secret, code)) {
            return null;
        }
        long currentStep = Math.floorDiv(unixTimeSeconds, TIME_STEP_SECONDS);
        return LongStream.rangeClosed(currentStep - 1, currentStep + 1)
                .filter(step -> isCodeValidAtTimeStep(secret, code, step))
                .boxed()
                .findFirst()
                .orElse(null);
    }

    private boolean isCodeValidAtTimeStep(String secret, String code, long timeStep) {
        byte[] expected = generateCodeForTimeStep(secret, timeStep).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, code.getBytes(StandardCharsets.US_ASCII));
    }

    private String generateCodeForTimeStep(String secret, long timeStep) {
        try {
            return codeGenerator.generate(secret, timeStep);
        } catch (CodeGenerationException exception) {
            throw new IllegalStateException("Could not validate two-factor code", exception);
        }
    }

    public String encryptAuthenticatorSecret(String plaintext) {
        return secretEncryption.encrypt(plaintext);
    }

    public String decryptAuthenticatorSecret(String ciphertext) {
        return secretEncryption.decrypt(ciphertext);
    }

    public List<String> generateRecoveryCodes() {
        return Arrays.asList(recoveryCodeGenerator.generateCodes(10));
    }

    public String hashRecoveryCode(String code) {
        return backupCodeEncoder.encode(normalizeRecoveryCode(code));
    }

    public String normalizeRecoveryCode(String code) {
        return code.replaceAll("[\\s-]", "").toLowerCase(java.util.Locale.ROOT);
    }

    public boolean matchesRecoveryCode(String code, String encodedHash) {
        return backupCodeEncoder.matches(normalizeRecoveryCode(code), encodedHash);
    }
}
