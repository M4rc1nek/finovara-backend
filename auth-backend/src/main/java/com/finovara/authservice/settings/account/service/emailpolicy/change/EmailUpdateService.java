package com.finovara.authservice.settings.account.service.emailpolicy.change;

import com.finovara.contracts.event.activity.secure.accountchange.activity.AccountChangesActivityEvent;
import com.finovara.contracts.event.notification.SendEmailEvent;
import com.finovara.contracts.model.activity.AccountChangesActivityType;
import com.finovara.contracts.outbox.OutboxService;

import static com.finovara.contracts.clientdata.browser.UserBrowser.getBrowser;
import static com.finovara.contracts.clientdata.ip.ClientIp.getClientIpAddress;
import static com.finovara.contracts.clientdata.location.UserLocation.getLocationFromIp;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmailUpdateService {
    private final UserRepository userRepository;
    private final OutboxService outboxService;

    @Transactional
    public void updateEmail(User user, String email, HttpServletRequest request) {
        user.setEmail(email);
        userRepository.save(user);

        createActivity(user, request);
        outboxService.save("User", user.getId().toString(), "notification.email.send",
                new SendEmailEvent(user.getId(), user.getUsername(), user.getEmail(), "Finovara - Zmiana adresu e-mail", "email/email-changed.html"));
    }

    private void createActivity(User user, HttpServletRequest request) {
        String ipAddress = getClientIpAddress(request);
        outboxService.save("User", user.getId().toString(), "activity.account-changes",
                new AccountChangesActivityEvent(user.getId(), AccountChangesActivityType.EMAIL_CHANGED, getBrowser(request), ipAddress, getLocationFromIp(ipAddress), LocalDateTime.now()));
    }
}