package com.finovara.corebackend.usersetting.account.service.passwordpolicy.change;

import com.finovara.contracts.event.secure.accountchange.activity.AccountChangesActivityEvent;
import com.finovara.contracts.model.activity.AccountChangesActivityType;

import static com.finovara.contracts.clientdata.browser.UserBrowser.getBrowser;
import static com.finovara.contracts.clientdata.ip.ClientIp.getClientIpAddress;
import static com.finovara.contracts.clientdata.location.UserLocation.getLocationFromIp;
import com.finovara.corebackend.user.model.User;
import com.finovara.corebackend.user.repository.UserRepository;
import com.finovara.corebackend.usersetting.notificationemail.action.passwordchange.service.NotifyPasswordChangeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PasswordUpdateService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final NotifyPasswordChangeService notifyPasswordChangeService;

    @Transactional
    public void updatePassword(User user, String newPassword, HttpServletRequest request) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        createActivity(user, request);

        notifyPasswordChangeService.sendEmail(user);
    }

    private void createActivity(User user, HttpServletRequest request) {
        String ipAddress = getClientIpAddress(request);
        kafkaTemplate.send("activity.account-changes", new AccountChangesActivityEvent(user.getId(), AccountChangesActivityType.PASSWORD_CHANGED, getBrowser(request), ipAddress, getLocationFromIp(ipAddress), LocalDateTime.now()));
    }
}
