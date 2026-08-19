package com.finovara.authservice.settings.account.service.emailpolicy.change;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailUpdateServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OutboxService outboxService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private EmailUpdateService emailUpdateService;

    private User user;

    private static final Long USER_ID = 1L;
    private static final String USERNAME = "jankowalski";
    private static final String OLD_EMAIL = "old@example.com";
    private static final String NEW_EMAIL = "new@example.com";

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(USER_ID);
        user.setUsername(USERNAME);
        user.setEmail(OLD_EMAIL);

        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void shouldUpdateUserEmailAndPersist() {
        emailUpdateService.updateEmail(user, NEW_EMAIL, request);

        assertThat(user.getEmail()).isEqualTo(NEW_EMAIL);
        verify(userRepository).save(user);
    }

    @Test
    void shouldSaveEmailNotificationToOutbox() {
        emailUpdateService.updateEmail(user, NEW_EMAIL, request);

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
        assertThat(event.email()).isEqualTo(NEW_EMAIL);
    }

    @Test
    void shouldSaveActivityEventToOutbox() {
        emailUpdateService.updateEmail(user, NEW_EMAIL, request);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(outboxService).save(
                eq("User"),
                eq(USER_ID.toString()),
                eq("activity.account-changes"),
                payloadCaptor.capture()
        );

        AccountChangesActivityEvent event = (AccountChangesActivityEvent) payloadCaptor.getValue();
        assertThat(event.userId()).isEqualTo(USER_ID);
        assertThat(event.type()).isEqualTo(AccountChangesActivityType.EMAIL_CHANGED);
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    void shouldSaveBothOutboxEventsExactlyOnce() {
        emailUpdateService.updateEmail(user, NEW_EMAIL, request);

        verify(outboxService, times(2)).save(any(), any(), any(), any());
    }
}