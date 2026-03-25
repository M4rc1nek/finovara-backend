package com.finovara.finovarabackend.usersetting.account.service.passwordpolicy;

import com.finovara.finovarabackend.accountactivity.accountchange.activities.model.AccountChangesActivityType;
import com.finovara.finovarabackend.accountactivity.accountchange.activities.service.AccountChangesActivityService;
import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.exception.serviceunavailable.ServiceUnavailableException;
import com.finovara.finovarabackend.exception.unprocessablecontent.MissingRequirementException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.usersetting.account.dto.passwordpolicy.ForgotPasswordDto;
import com.finovara.finovarabackend.usersetting.account.dto.passwordpolicy.PasswordRequestDto;
import com.finovara.finovarabackend.usersetting.account.model.AccountSettings;
import com.finovara.finovarabackend.usersetting.account.repository.AccountRepository;
import com.finovara.finovarabackend.usersetting.notification.model.NotificationSettings;
import com.finovara.finovarabackend.util.service.user.accountmanagment.passwordpolicy.PasswordChangeEmailService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForgotPasswordService {
    private static final String TEMPLATE_PATH = "email/reset-password.html";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${mail.recipient.address}")
    private String recipientAddress;

    private final UserManagerService userManagerService;
    private final PasswordEncoder passwordEncoder;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final JavaMailSender javaMailSender;
    private final PasswordChangeEmailService passwordChangeEmailService;
    private final AccountChangesActivityService accountChangesActivityService;

    public void validateEmailExists(String email) {
        if (!userRepository.existsByEmail(email)) {
            throw new InvalidInputException("No account found with the given email address");
        }
    }

    @Async
    public void emailSend(String email, ForgotPasswordDto forgotPasswordDto) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(forgotPasswordDto.email());
            helper.setFrom("Finovara <" + recipientAddress + ">");
            helper.setReplyTo(recipientAddress);
            helper.setSubject("Przypomnienie Hasła");

            int code = generateSecureCode(email);
            String html = loadTemplate(String.valueOf(code), String.valueOf(email));

            helper.setText(html, true);

            javaMailSender.send(message);

        } catch (Exception exception) {
            throw new ServiceUnavailableException("Failed to send email", exception);
        }
    }

    public void verifyCode(String email, ForgotPasswordDto forgotPasswordDto) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        AccountSettings accountSettings = user.getAccountSettings();

        if (accountSettings.getForgotPasswordCode() == null) {
            throw new InvalidInputException("No code generated");
        }

        if (accountSettings.getForgotPasswordCodeExpiresAt() == null ||
                accountSettings.getForgotPasswordCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidInputException("Code expired");
        }

        if (!accountSettings.getForgotPasswordCode().equals(forgotPasswordDto.code())) {
            throw new InvalidInputException("Incorrect code");
        }
    }

    @Transactional
    public void changePasswordWithCode(String email, PasswordRequestDto passwordRequestDto, HttpServletRequest request) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        AccountSettings accountSettings = user.getAccountSettings();
        NotificationSettings notificationSettings = user.getNotificationSettings();

        verifyCode(email, passwordRequestDto.forgotPasswordDto());

        if (!passwordRequestDto.changePasswordDto().newPassword()
                .equals(passwordRequestDto.changePasswordDto().confirmNewPassword())) {
            throw new MissingRequirementException("New passwords have to be the same");
        }

        if (passwordEncoder.matches(passwordRequestDto.changePasswordDto().newPassword(), user.getPassword())) {
            throw new MissingRequirementException("This password is already set");
        }

        if (passwordRequestDto.changePasswordDto().newPassword().isEmpty()) {
            throw new MissingRequirementException("The new password cannot be empty");
        }

        user.setPassword(passwordEncoder.encode(passwordRequestDto.changePasswordDto().newPassword()));
        userRepository.save(user);
        accountChangesActivityService.createAccountChangesActivity(email, AccountChangesActivityType.PASSWORD_CHANGED, request);
        accountSettings.setForgotPasswordCode(null);
        accountSettings.setForgotPasswordCodeExpiresAt(null);
        accountRepository.save(accountSettings);

        if (notificationSettings.isNotifyOnPasswordChange()) {
            passwordChangeEmailService.sendEmail(user);
        }
    }

    private int generateSecureCode(String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        AccountSettings accountSettings = user.getAccountSettings();

        LocalDateTime startCodeExpiration = LocalDateTime.now();

        int code = SECURE_RANDOM.nextInt(900000) + 100000;

        if (accountSettings == null) {
            accountSettings = new AccountSettings();
            accountSettings.setUserAssigned(user);
            user.setAccountSettings(accountSettings);
        }
        accountSettings.setForgotPasswordCode(code);
        accountSettings.setForgotPasswordCodeExpiresAt(startCodeExpiration.plusMinutes(15));

        accountRepository.save(accountSettings);
        return code;
    }

    private String loadTemplate(String code, String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        try {
            ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH);

            try (InputStream inputStream = resource.getInputStream()) {
                String html = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                return html
                        .replace("{{CODE}}", code)
                        .replace("{{USERNAME}}", user.getUsername());
            }

        } catch (Exception e) {
            throw new ServiceUnavailableException("Failed to load email template", e);
        }
    }

}
