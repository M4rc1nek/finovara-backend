package com.finovara.authservice.security.oauth2;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

@Component
public class PendingLoginEncryptor {

    @Value("${oauth2.pending-login.secret}")
    private String secret;

    private TextEncryptor encryptor;

    @PostConstruct
    void initialize() {
        encryptor = Encryptors.delux(secret, "5c0744940b5c369b");
    }

    public String encrypt(String plaintext) {
        return encryptor.encrypt(plaintext);
    }

    public String decrypt(String ciphertext) {
        return encryptor.decrypt(ciphertext);
    }
}
