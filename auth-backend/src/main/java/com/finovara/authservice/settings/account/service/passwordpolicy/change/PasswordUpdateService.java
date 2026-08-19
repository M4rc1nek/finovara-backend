package com.finovara.authservice.settings.account.service.passwordpolicy.change;

import com.finovara.contracts.activity.event.secure.accountchange.activity.AccountChangesActivityEvent;
import com.finovara.contracts.notification.email.ActionEmailEventType;
import com.finovara.contracts.notification.event.SendEmailEvent;
import com.finovara.contracts.model.activity.AccountChangesActivityType;
import com.finovara.contracts.outbox.OutboxService;

import static com.finovara.contracts.clientdata.browser.UserBrowser.getBrowser;
import static com.finovara.contracts.clientdata.ip.ClientIp.getClientIpAddress;
import static com.finovara.contracts.clientdata.location.UserLocation.getLocationFromIp;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PasswordUpdateService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OutboxService outboxService;

    @Transactional
    public void updatePassword(User user, String newPassword, HttpServletRequest request) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        createActivity(user, request);
        outboxService.save("User", user.getId().toString(), "notification.email.send",
                new SendEmailEvent(user.getId(), user.getUsername(), user.getEmail(),
                        ActionEmailEventType.PASSWORD_CHANGED, Map.of()));
    }

    private void createActivity(User user, HttpServletRequest request) {
        String ipAddress = getClientIpAddress(request);
        outboxService.save("User", user.getId().toString(), "activity.account-changes",
                new AccountChangesActivityEvent(user.getId(), AccountChangesActivityType.PASSWORD_CHANGED, getBrowser(request), ipAddress, getLocationFromIp(ipAddress), LocalDateTime.now()));
    }
}