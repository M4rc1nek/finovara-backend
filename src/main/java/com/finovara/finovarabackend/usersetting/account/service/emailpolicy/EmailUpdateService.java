package com.finovara.finovarabackend.usersetting.account.service.emailpolicy;

import com.finovara.finovarabackend.accountactivity.secure.accountchange.activity.model.AccountChangesActivityType;
import com.finovara.finovarabackend.accountactivity.secure.accountchange.activity.service.AccountChangesActivityService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.usersetting.notificationemail.util.emailsender.EmailChangeNotifier;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailUpdateService {
    private final UserRepository userRepository;
    private final AccountChangesActivityService accountChangesActivityService;
    private final EmailChangeNotifier emailChangeNotifier;

    @Transactional
    public void updateEmail(User user, String email, HttpServletRequest request) {
        user.setEmail(email);
        userRepository.save(user);

        createActivity(user, request);
        emailChangeNotifier.sendEmail(user);
    }

    private void createActivity(User user, HttpServletRequest request) {
        accountChangesActivityService.createAccountChangesActivity(user.getId(), AccountChangesActivityType.EMAIL_CHANGED, request);
    }
}
