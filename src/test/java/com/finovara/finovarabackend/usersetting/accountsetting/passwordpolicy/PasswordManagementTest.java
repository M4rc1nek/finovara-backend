package com.finovara.finovarabackend.usersetting.accountsetting.passwordpolicy;

import com.finovara.finovarabackend.accountactivity.secure.accountchange.activity.model.AccountChangesActivityType;
import com.finovara.finovarabackend.accountactivity.secure.accountchange.activity.service.AccountChangesActivityService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.usersetting.account.service.passwordpolicy.PasswordManagementService;
import com.finovara.finovarabackend.usersetting.notificationemail.passwordchange.service.NotifyPasswordChangeService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordManagementTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AccountChangesActivityService accountChangesActivityService;
    @Mock
    private NotifyPasswordChangeService notifyPasswordChangeService;

    @InjectMocks
    private PasswordManagementService passwordManagementService;

    private HttpServletRequest request;
    private User user;

    @BeforeEach
    void setUp(){
        user = new User();
        user.setEmail("activity@test.com");
    }
    @Test
    void shouldUpdatePasswordAndTriggerAllActions() {
        String rawPassword = "newPass";
        String encodedPassword = "encodedPass";

        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);

        passwordManagementService.updatePassword(user, rawPassword, request);

        assertEquals(encodedPassword, user.getPassword());

        verify(userRepository).save(user);

        verify(accountChangesActivityService).createAccountChangesActivity("activity@test.com", AccountChangesActivityType.PASSWORD_CHANGED, request);

        verify(notifyPasswordChangeService).sendEmail(user);
    }

    @Test
    void shouldEncodePasswordWithCorrectValue() {
        String rawPassword = "myPassword";

        passwordManagementService.updatePassword(user, rawPassword, request);

        verify(passwordEncoder).encode("myPassword");
    }

    @Test
    void shouldSaveUserWithUpdatedPassword() {
        when(passwordEncoder.encode(any())).thenReturn("encodedPass");

        passwordManagementService.updatePassword(user, "newPass", request);

        verify(userRepository).save(user);
        assertEquals("encodedPass", user.getPassword());
    }

    @Test
    void shouldCreateActivityWithCorrectData() {
        when(passwordEncoder.encode(any())).thenReturn("encodedPass");

        passwordManagementService.updatePassword(user, "newPass", request);

        verify(accountChangesActivityService).createAccountChangesActivity("activity@test.com", AccountChangesActivityType.PASSWORD_CHANGED, request);
    }

    @Test
    void shouldSendNotificationEmail() {
        when(passwordEncoder.encode(any())).thenReturn("encodedPass");

        passwordManagementService.updatePassword(user, "newPass", request);

        verify(notifyPasswordChangeService).sendEmail(user);
    }
}