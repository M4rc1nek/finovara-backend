package com.finovara.authservice.settings.account.service.passwordpolicy.change;

import com.finovara.contracts.activity.event.secure.accountchange.activity.AccountChangesActivityEvent;
import com.finovara.contracts.notification.event.SendEmailEvent;
import com.finovara.contracts.model.activity.AccountChangesActivityType;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordUpdateServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OutboxService outboxService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private PasswordUpdateService passwordUpdateService;

    private User user;

    private static final Long USER_ID = 1L;
    private static final String USERNAME = "jankowalski";
    private static final String EMAIL = "jan@example.com";
    private static final String NEW_PASSWORD = "newPassword";
    private static final String ENCODED_PASSWORD = "encodedPassword";

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(USER_ID)
                .username(USERNAME)
                .email(EMAIL)
                .password("oldPassword")
                .build();

        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void shouldEncodeAndSetNewPassword() {
        passwordUpdateService.updatePassword(user, NEW_PASSWORD, request);

        verify(passwordEncoder).encode(NEW_PASSWORD);
        assertThat(user.getPassword()).isEqualTo(ENCODED_PASSWORD);
    }

    @Test
    void shouldPersistUser() {
        passwordUpdateService.updatePassword(user, NEW_PASSWORD, request);

        verify(userRepository).save(user);
    }

    @Test
    void shouldSaveActivityEventToOutbox() {
        passwordUpdateService.updatePassword(user, NEW_PASSWORD, request);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(outboxService).save(
                eq("User"),
                eq(USER_ID.toString()),
                eq("activity.account-changes"),
                payloadCaptor.capture()
        );

        AccountChangesActivityEvent event = (AccountChangesActivityEvent) payloadCaptor.getValue();
        assertThat(event.userId()).isEqualTo(USER_ID);
        assertThat(event.type()).isEqualTo(AccountChangesActivityType.PASSWORD_CHANGED);
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    void shouldSaveEmailNotificationToOutbox() {
        passwordUpdateService.updatePassword(user, NEW_PASSWORD, request);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(outboxService).save(
                eq("User"),
                eq(USER_ID.toString()),
                eq("notification.email.send"),
                payloadCaptor.capture()
        );

        SendEmailEvent event = (SendEmailEvent) payloadCaptor.getValue();
        assertThat(event.userId()).isEqualTo(USER_ID);
        assertThat(event.username()).isEqualTo(USERNAME);
        assertThat(event.email()).isEqualTo(EMAIL);
    }

    @Test
    void shouldSaveBothOutboxEventsExactlyOnce() {
        passwordUpdateService.updatePassword(user, NEW_PASSWORD, request);

        verify(outboxService, times(2)).save(any(), any(), any(), any());
    }

    @Test
    void shouldExecuteOperationsInCorrectOrder() {
        var inOrder = inOrder(passwordEncoder, userRepository, outboxService);

        passwordUpdateService.updatePassword(user, NEW_PASSWORD, request);

        inOrder.verify(passwordEncoder).encode(NEW_PASSWORD);
        inOrder.verify(userRepository).save(user);
        inOrder.verify(outboxService).save(eq("User"), any(), eq("activity.account-changes"), any());
        inOrder.verify(outboxService).save(eq("User"), any(), eq("notification.email.send"), any());
    }
}