package com.finovara.finovarabackend.usersetting.account.service.emailpolicy;

import com.finovara.finovarabackend.accountactivity.secure.accountchange.activity.model.AccountChangesActivityType;
import com.finovara.finovarabackend.accountactivity.secure.accountchange.activity.service.AccountChangesActivityService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.usersetting.notificationemail.action.emailchange.service.NotifyEmailChangeService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailUpdateServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountChangesActivityService accountChangesActivityService;

    @Mock
    private NotifyEmailChangeService notifyEmailChangeService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private EmailUpdateService emailUpdateService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("old@mail.com");
    }

    @Test
    void shouldUpdateEmailAndPersistUser() {
        String newEmail = "new@mail.com";

        emailUpdateService.updateEmail(user, newEmail, request);

        assertEquals(newEmail, user.getEmail());
        verify(userRepository).save(user);
    }

    @Test
    void shouldCreateAccountChangeActivity() {
        String newEmail = "new@mail.com";

        emailUpdateService.updateEmail(user, newEmail, request);

        verify(accountChangesActivityService).createAccountChangesActivity(user.getId(), AccountChangesActivityType.EMAIL_CHANGED, request);
    }

    @Test
    void shouldSendNotificationEmail() {
        String newEmail = "new@mail.com";

        emailUpdateService.updateEmail(user, newEmail, request);

        verify(notifyEmailChangeService).sendEmail(user);
    }

    @Test
    void shouldExecuteAllSideEffectsInOrder() {
        String newEmail = "new@mail.com";

        emailUpdateService.updateEmail(user, newEmail, request);

        InOrder inOrder = inOrder(userRepository, accountChangesActivityService, notifyEmailChangeService);

        inOrder.verify(userRepository).save(user);
        inOrder.verify(accountChangesActivityService).createAccountChangesActivity(user.getId(), AccountChangesActivityType.EMAIL_CHANGED, request);
        inOrder.verify(notifyEmailChangeService).sendEmail(user);
    }
}