package com.finovara.finovarabackend.usersetting.account.service.passwordpolicy;

import com.finovara.finovarabackend.accountactivity.secure.accountchange.activity.model.AccountChangesActivityType;
import com.finovara.finovarabackend.accountactivity.secure.accountchange.activity.service.AccountChangesActivityService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.usersetting.notificationemail.passwordchange.service.NotifyPasswordChangeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordUpdateService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountChangesActivityService accountChangesActivityService;
    private final NotifyPasswordChangeService notifyPasswordChangeService;

    public void updatePassword(User user, String newPassword, HttpServletRequest request) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        createActivity(user, request);

        notifyPasswordChangeService.sendEmail(user);
    }

    private void createActivity(User user, HttpServletRequest request) {
        accountChangesActivityService.createAccountChangesActivity(user.getId(), AccountChangesActivityType.PASSWORD_CHANGED, request);
    }
}
