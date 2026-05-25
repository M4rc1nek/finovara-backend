package com.finovara.corebackend.usersetting.account.service.emailpolicy;

import com.finovara.activityservice.contracts.clientdata.browser.UserBrowser;
import com.finovara.activityservice.contracts.clientdata.ip.ClientIp;
import com.finovara.activityservice.contracts.clientdata.location.UserLocation;
import com.finovara.activityservice.contracts.event.secure.accountchange.activity.AccountChangesActivityEvent;
import com.finovara.activityservice.contracts.model.activity.AccountChangesActivityType;

import static com.finovara.activityservice.contracts.clientdata.browser.UserBrowser.getBrowser;
import static com.finovara.activityservice.contracts.clientdata.ip.ClientIp.getClientIpAddress;
import static com.finovara.activityservice.contracts.clientdata.location.UserLocation.getLocationFromIp;
import com.finovara.corebackend.user.model.User;
import com.finovara.corebackend.user.repository.UserRepository;
import com.finovara.corebackend.usersetting.notificationemail.action.emailchange.service.NotifyEmailChangeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmailUpdateService {
    private final UserRepository userRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final NotifyEmailChangeService notifyEmailChangeService;

    @Transactional
    public void updateEmail(User user, String email, HttpServletRequest request) {
        user.setEmail(email);
        userRepository.save(user);

        createActivity(user, request);
        notifyEmailChangeService.sendEmail(user);
    }

    private void createActivity(User user, HttpServletRequest request) {
        String ipAddress = getClientIpAddress(request);
        kafkaTemplate.send("activity.account-changes", new AccountChangesActivityEvent(user.getId(), AccountChangesActivityType.EMAIL_CHANGED, getBrowser(request), ipAddress, getLocationFromIp(ipAddress), LocalDateTime.now()));
    }
}
