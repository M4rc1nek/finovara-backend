package com.finovara.authservice.twofactor.service.code;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticatorCredentialServiceTest {

    private final TwoFactorSecretEncryption secretEncryption = createSecretEncryption();
    private final AuthenticatorCredentialService credentialService =
            new AuthenticatorCredentialService(secretEncryption, new BCryptPasswordEncoder());

    private static TwoFactorSecretEncryption createSecretEncryption() {
        TwoFactorSecretEncryption encryption = new TwoFactorSecretEncryption();
        ReflectionTestUtils.setField(encryption, "encryptionSecret", "unit-test-encryption-secret");
        ReflectionTestUtils.invokeMethod(encryption, "initialize");
        return encryption;
    }

    @Test
    void verifiesAuthenticatorCodeUsingLibrary() throws Exception {
        String secret = credentialService.generateAuthenticatorSecret();
        long timestamp = System.currentTimeMillis() / 1000;
        long timeStep = timestamp / 30;
        String code = new DefaultCodeGenerator().generate(secret, timeStep);

        assertThat(credentialService.findCodeTimeStep(secret, code, timestamp)).isEqualTo(timeStep);
        assertThat(credentialService.findCodeTimeStep(secret, "invalid", timestamp)).isNull();
    }

    @Test
    void encryptsAndDecryptsSecretsUsingDifferentNonces() {
        String secret = credentialService.generateAuthenticatorSecret();

        String firstCiphertext = credentialService.encryptAuthenticatorSecret(secret);
        String secondCiphertext = credentialService.encryptAuthenticatorSecret(secret);

        assertThat(firstCiphertext).isNotEqualTo(secondCiphertext);
        assertThat(credentialService.decryptAuthenticatorSecret(firstCiphertext)).isEqualTo(secret);
        assertThat(credentialService.decryptAuthenticatorSecret(secondCiphertext)).isEqualTo(secret);
    }

    @Test
    void createsTenUniqueHighEntropyBackupCodesAndNormalizesFormatting() {
        var codes = credentialService.generateRecoveryCodes();

        assertThat(codes).hasSize(10).doesNotHaveDuplicates();
        assertThat(codes).allSatisfy(code -> assertThat(code).hasSize(19));
        String hash = credentialService.hashRecoveryCode(codes.getFirst());
        assertThat(credentialService.matchesRecoveryCode(" " + codes.getFirst().toUpperCase(), hash)).isTrue();
        assertThat(credentialService.matchesRecoveryCode(codes.get(1), hash)).isFalse();
    }
}
