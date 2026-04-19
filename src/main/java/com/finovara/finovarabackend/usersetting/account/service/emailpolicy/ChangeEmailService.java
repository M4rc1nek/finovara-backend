package com.finovara.finovarabackend.usersetting.account.service;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.exception.serviceunavailable.ServiceUnavailableException;
import com.finovara.finovarabackend.exception.unprocessablecontent.MissingRequirementException;
import com.finovara.finovarabackend.user.exception.conflict.EmailAlreadyExistsException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ChangeEmailService {

    private static final String TEMPLATE_PATH = "email/change-email.html";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final JavaMailSender javaMailSender;
    private final AccountRepository accountRepository;
    private final UserManagerService userManagerService;
    private final PasswordValidator passwordValidator;

    @Value("${mail.recipient.address}")
    private String recipientAddress;

    @Async
    public void emailSend(Long userId, ChangeEmailDto changeEmailDto) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(changeEmailDto.email());
            helper.setFrom("Finovara <" + recipientAddress + ">");
            helper.setReplyTo(recipientAddress);
            helper.setSubject("Zmiana adresu e-mail");

            int code = generateSecureCode(userId, changeEmailDto.email());
            String html = loadTemplate(user.getUsername(), String.valueOf(code));

            helper.setText(html, true);

            javaMailSender.send(message);

        } catch (Exception exception) {
            throw new ServiceUnavailableException("Failed to send email", exception);
        }
    }

    public void verifyCode(Long userId, ChangeEmailDto changeEmailDto) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        AccountSettings accountSettings = user.getAccountSettings();

        Integer code = accountSettings.getEmailChangeCode();

        if (!code.equals(changeEmailDto.code())) {
            throw new InvalidInputException("Incorrect code");
        }
    }

    @Transactional
    public void changeEmailAddressWithCode(Long userId, ChangeEmailDto changeEmailDto) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        AccountSettings accountSettings = user.getAccountSettings();

        verifyCode(userId, changeEmailDto);

        String newEmail = accountSettings.getPendingEmail();

        accountSettings.setEmailChangeCode(null);
        accountSettings.setPendingEmail(null);

        user.setEmail(newEmail);

        accountRepository.save(accountSettings);
    }

    private int generateSecureCode(Long userId, String newEmail) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        AccountSettings accountSettings = user.getAccountSettings();

        int code = SECURE_RANDOM.nextInt(900000) + 100000;

        accountSettings.setEmailChangeCode(code);
        accountSettings.setPendingEmail(newEmail);

        accountRepository.save(accountSettings);

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
            throw new ServiceUnavailableException("Failed to load email template", e);
        }
    }

    private void validateEmail(User user, ChangeEmailDto changeEmailDto) {
        String newEmail = changeEmailDto.email();

        if (newEmail == null || newEmail.isBlank()) {
            throw new MissingRequirementException("Email cannot be empty");
        }

        if (newEmail.equalsIgnoreCase(user.getEmail())) {
            throw new InvalidInputException("New mail cannot be the same");
        }

        passwordValidator.validatePassword(user.getId(), new ConfirmPasswordDto(changeEmailDto.password()));

        if (userRepository.existsByEmail(newEmail)) {
            throw new EmailAlreadyExistsException("Email already in use");
        }
    }
}