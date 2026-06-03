package com.finovara.corebackend.usersetting.account.service.emailpolicy;

import com.finovara.contracts.clientdata.location.UserLocation;
import com.finovara.contracts.event.activity.secure.accountchange.activity.AccountChangesActivityEvent;
import com.finovara.contracts.event.notification.SendEmailEvent;
import com.finovara.corebackend.user.model.User;
import com.finovara.corebackend.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailUpdateServiceTest {

    @Mock
    private UserRepository userRepository;


    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private EmailUpdateService emailUpdateService;

    private User user;

    private static final Long USER_ID = 1L;
    private static final String OLD_EMAIL = "old@finovara.com";
    private static final String NEW_EMAIL = "new@finovara.com";
    private static final String IP_ADDRESS = "192.168.1.1";
    private static final String BROWSER = "Chrome";
    private static final String LOCATION = "Warsaw, Poland";

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(USER_ID)
                .email(OLD_EMAIL)
                .build();

        when(request.getRemoteAddr()).thenReturn(IP_ADDRESS);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/111.0");
    }

    @Test
    void shouldUpdateUserEmail() {
        emailUpdateService.updateEmail(user, NEW_EMAIL, request);

        assertThat(user.getEmail()).isEqualTo(NEW_EMAIL);
    }

    @Test
    void shouldSaveUser() {
        emailUpdateService.updateEmail(user, NEW_EMAIL, request);

        verify(userRepository).save(user);
    }

    @Test
    void shouldSendKafkaEventWithCorrectTopic() {
        emailUpdateService.updateEmail(user, NEW_EMAIL, request);

        verify(kafkaTemplate)
                .send(eq("activity.account-changes"), any(AccountChangesActivityEvent.class));
    }

    @Test
    void shouldSendKafkaEventWithCorrectPayload() {

        try (MockedStatic<UserLocation> mocked = mockStatic(UserLocation.class)) {

            mocked.when(() -> UserLocation.getLocationFromIp(IP_ADDRESS))
                    .thenReturn("Warsaw, Poland");

            ArgumentCaptor<AccountChangesActivityEvent> eventCaptor =
                    ArgumentCaptor.forClass(AccountChangesActivityEvent.class);

            emailUpdateService.updateEmail(user, NEW_EMAIL, request);

            verify(kafkaTemplate)
                    .send(eq("activity.account-changes"), eventCaptor.capture());

            AccountChangesActivityEvent event = eventCaptor.getValue();

            assertThat(event.location()).isEqualTo("Warsaw, Poland");
        }
    }

    @Test
    void shouldSendEmailNotification() {
        emailUpdateService.updateEmail(user, NEW_EMAIL, request);

        verify(kafkaTemplate).send(eq("notification.email.send"), any(SendEmailEvent.class));
    }

    @Test
    void shouldExecuteOperationsInCorrectOrder() {
        var inOrder = inOrder(userRepository, kafkaTemplate);

        emailUpdateService.updateEmail(user, NEW_EMAIL, request);

        inOrder.verify(userRepository).save(user);
        inOrder.verify(kafkaTemplate).send(eq("activity.account-changes"), any(AccountChangesActivityEvent.class));
        inOrder.verify(kafkaTemplate).send(eq("notification.email.send"), any(SendEmailEvent.class));
    }

    @Test
    void shouldResolveClientDataFromRequest() {
        emailUpdateService.updateEmail(user, NEW_EMAIL, request);

        verify(kafkaTemplate)
                .send(eq("activity.account-changes"), any(AccountChangesActivityEvent.class));
    }
}
