package com.finovara.finovarabackend.usersetting.account.service.passwordpolicy.change;

import com.finovara.finovarabackend.accountactivity.secure.accountchange.activity.model.AccountChangesActivityType;
import com.finovara.finovarabackend.accountactivity.secure.accountchange.activity.service.AccountChangesActivityService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.usersetting.account.service.passwordpolicy.change.PasswordUpdateService;
import com.finovara.finovarabackend.usersetting.notificationemail.action.passwordchange.service.NotifyPasswordChangeService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordUpdateServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountChangesActivityService accountChangesActivityService;

    @Mock
    private NotifyPasswordChangeService notifyPasswordChangeService;

    @Mock
    private HttpServletRequest request;

    private PasswordEncoder passwordEncoder;

    private PasswordUpdateService passwordUpdateService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        passwordUpdateService = new PasswordUpdateService(
                userRepository,
                passwordEncoder,
                accountChangesActivityService,
                notifyPasswordChangeService
        );
    }

    @Test
    void shouldEncodePasswordAndSaveUser() {
        User user = new User();
        user.setId(1L);
        user.setPassword("oldPassword");

        String newPassword = "newPassword123";

        passwordUpdateService.updatePassword(user, newPassword, request);

        assertNotEquals("newPassword123", user.getPassword());
        assertTrue(passwordEncoder.matches(newPassword, user.getPassword()));

        verify(userRepository).save(user);
    }

    @Test
    void shouldCreateAccountChangeActivity() {
        User user = new User();
        user.setId(1L);

        passwordUpdateService.updatePassword(user, "pass", request);

        verify(accountChangesActivityService).createAccountChangesActivity(eq(1L), eq(AccountChangesActivityType.PASSWORD_CHANGED), eq(request));
    }

    @Test
    void shouldSendNotificationEmail() {
        User user = new User();
        user.setId(1L);

        passwordUpdateService.updatePassword(user, "pass", request);

        verify(notifyPasswordChangeService).sendEmail(user);
    }

    @Test
    void shouldExecuteAllStepsInOrder() {
        User user = new User();
        user.setId(1L);

        passwordUpdateService.updatePassword(user, "pass", request);

        InOrder inOrder = inOrder(userRepository, accountChangesActivityService, notifyPasswordChangeService);

        inOrder.verify(userRepository).save(user);
        inOrder.verify(accountChangesActivityService).createAccountChangesActivity(anyLong(), any(), any());
        inOrder.verify(notifyPasswordChangeService).sendEmail(user);
    }
}