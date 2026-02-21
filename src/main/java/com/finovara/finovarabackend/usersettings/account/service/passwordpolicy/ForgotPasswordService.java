package com.finovara.finovarabackend.usersettings.account.service.passwordpolicy;

import com.finovara.finovarabackend.accountactivity.accountchanges.activities.model.AccountChangesActivityType;
import com.finovara.finovarabackend.accountactivity.accountchanges.activities.service.AccountChangesActivityService;
import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.exception.unprocessablecontent.MissingRequirementException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.usersettings.account.dto.passwordpolicy.ForgotPasswordDto;
import com.finovara.finovarabackend.usersettings.account.dto.passwordpolicy.PasswordRequestDto;
import com.finovara.finovarabackend.usersettings.account.model.AccountSettings;
import com.finovara.finovarabackend.usersettings.account.repository.AccountRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ForgotPasswordService {

    @Value("${mail.recipient.address}")
    String recipientAddress;

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
            String html = loadTemplate(String.valueOf(code));

            helper.setText(html, true);

            javaMailSender.send(message);

        } catch (Exception exception) {
            throw new RuntimeException("Failed to send email", exception);
        }
    }

    public void verifyCode(String email, ForgotPasswordDto forgotPasswordDto) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        AccountSettings accountSettings = user.getAccountSettings();

        if (!accountSettings.getForgotPasswordCode().equals(forgotPasswordDto.code())) {
            throw new InvalidInputException("Incorrect code");
        }
    }

    @Transactional
    public void changePasswordWithCode(String email, PasswordRequestDto passwordRequestDto, HttpServletRequest request) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        AccountSettings accountSettings = user.getAccountSettings();

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
        accountRepository.save(accountSettings);

        passwordChangeEmailService.sendEmail(user);
    }

    @Transactional
    private int generateSecureCode(String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        AccountSettings accountSettings = user.getAccountSettings();

        SecureRandom random = new SecureRandom();
        int code = random.nextInt(900000) + 100000;

        if (accountSettings == null) {
            accountSettings = new AccountSettings();
            accountSettings.setUserAssigned(user);
            user.setAccountSettings(accountSettings);
        }
        accountSettings.setForgotPasswordCode(code);
        accountRepository.save(accountSettings);
        return code;
    }

    private String loadTemplate(String code) {
        try {
            ClassPathResource resource = new ClassPathResource("email/reset-password.html");

            try (InputStream inputStream = resource.getInputStream()) {
                String html = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                return html.replace("{{CODE}}", code);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to load email template", e);
        }
    }

}
