package com.finovara.finovarabackend.usersetting.account.service.emailpolicy;

import com.finovara.finovarabackend.exception.serviceunavailable.ServiceUnavailableException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.account.dto.ChangeEmailDto;
import com.finovara.finovarabackend.usersetting.account.model.AccountSettings;
import com.finovara.finovarabackend.usersetting.account.repository.AccountRepository;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import com.finovara.finovarabackend.util.confirmationpassword.service.PasswordValidator;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChangeEmailService {

    private static final String TEMPLATE_PATH = "email/change-email.html";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JavaMailSender javaMailSender;
    private final AccountRepository accountRepository;
    private final UserManagerService userManagerService;
    private final EmailValidator emailValidator;
    private final PasswordValidator passwordValidator;
    private final CodeValidator codeValidator;

    @Value("${mail.recipient.address}")
    private String recipientAddress;

    @Async
    public void sendEmailAsync(User user, String email, int code) {
        log.info("Email change requested: userId={}, email={}", user.getId(), email);

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(email);
            helper.setFrom("Finovara <" + recipientAddress + ">");
            helper.setReplyTo(recipientAddress);
            helper.setSubject("Zmiana adresu e-mail");

            String html = loadTemplate(user.getUsername(), String.valueOf(code));
            helper.setText(html, true);

            javaMailSender.send(message);

        } catch (Exception e) {
            throw new ServiceUnavailableException("Email sending failed", e);
        }
    }

    public void requestEmailChange(Long userId, ChangeEmailDto dto) {
        User user = userManagerService.getUserByIdOrThrow(userId);

        emailValidator.validateEmail(user, dto);
        passwordValidator.validatePassword(user.getId(), new ConfirmPasswordDto(dto.password()));

        int code = generateSecureCode(user.getId(), dto.email());
        sendEmailAsync(user, dto.email(), code);
    }

    @Transactional
    public void changeEmailAddressWithCode(Long userId, ChangeEmailDto dto) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        AccountSettings settings = user.getAccountSettings();

        codeValidator.verifyCode(settings, dto);

        String newEmail = settings.getPendingEmail();

        settings.setEmailChangeCode(null);
        settings.setEmailChangeCodeExpiresAt(null);
        settings.setPendingEmail(null);

        user.setEmail(newEmail);

        accountRepository.save(settings);
    }

    private int generateSecureCode(Long userId, String newEmail) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        AccountSettings settings = user.getAccountSettings();

        int code = SECURE_RANDOM.nextInt(900000) + 100000;

        settings.setEmailChangeCode(code);
        settings.setEmailChangeCodeExpiresAt(LocalDateTime.now().plusMinutes(15));
        settings.setPendingEmail(newEmail);

        accountRepository.save(settings);
        return code;
    }

    private String loadTemplate(String username, String code) {
        try {
            ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH);

            try (InputStream inputStream = resource.getInputStream()) {
                String html = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

                return html
                        .replace("{{CODE}}", code)
                        .replace("{{USERNAME}}", username);
            }

        } catch (Exception e) {
            throw new ServiceUnavailableException("Failed to load template", e);
        }
    }
}