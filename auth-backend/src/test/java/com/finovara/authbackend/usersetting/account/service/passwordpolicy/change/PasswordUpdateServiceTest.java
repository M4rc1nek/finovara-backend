package com.finovara.authbackend.usersetting.account.service.passwordpolicy.change;

import com.finovara.contracts.clientdata.location.UserLocation;
import com.finovara.contracts.event.activity.secure.accountchange.activity.AccountChangesActivityEvent;
import com.finovara.contracts.event.notification.SendEmailEvent;
import com.finovara.authbackend.user.model.User;
import com.finovara.authbackend.user.repository.UserRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordUpdateServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private PasswordUpdateService passwordUpdateService;

    private User user;

    private static final Long USER_ID = 1L;
    private static final String OLD_PASSWORD = "oldPassword";
    private static final String NEW_PASSWORD = "newPassword";
    private static final String ENCODED_PASSWORD = "encodedPassword";
    private static final String IP_ADDRESS = "192.168.1.1";
    private static final String BROWSER = "Chrome";
    private static final String LOCATION = "Warsaw, Poland";

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(USER_ID)
                .password(OLD_PASSWORD)
                .build();

        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(request.getRemoteAddr()).thenReturn(IP_ADDRESS);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/111.0");
    }

    @Test
    void shouldUpdateUserPassword() {
        passwordUpdateService.updatePassword(user, NEW_PASSWORD, request);

        assertThat(user.getPassword()).isEqualTo(ENCODED_PASSWORD);
    }

    @Test
    void shouldEncodePassword() {
        passwordUpdateService.updatePassword(user, NEW_PASSWORD, request);

        verify(passwordEncoder).encode(NEW_PASSWORD);
    }

    @Test
    void shouldSaveUser() {
        passwordUpdateService.updatePassword(user, NEW_PASSWORD, request);

        verify(userRepository).save(user);
    }

    @Test
    void shouldSendKafkaEventWithCorrectTopic() {
        passwordUpdateService.updatePassword(user, NEW_PASSWORD, request);

        verify(kafkaTemplate)
                .send(eq("activity.account-changes"), any(AccountChangesActivityEvent.class));
    }

    @Test
    void shouldSendKafkaEventWithCorrectPayload() {
        try (MockedStatic<UserLocation> mocked =
                     mockStatic(UserLocation.class)) {

            mocked.when(() -> UserLocation.getLocationFromIp(IP_ADDRESS))
                    .thenReturn("Warsaw, Poland");

            passwordUpdateService.updatePassword(user, NEW_PASSWORD, request);

            ArgumentCaptor<AccountChangesActivityEvent> eventCaptor =
                    ArgumentCaptor.forClass(AccountChangesActivityEvent.class);

            verify(kafkaTemplate).send(eq("activity.account-changes"), eventCaptor.capture());

            AccountChangesActivityEvent event = eventCaptor.getValue();

            assertThat(event.location()).isEqualTo("Warsaw, Poland");
        }
    }

    @Test
    void shouldSendPasswordChangeNotification() {
        passwordUpdateService.updatePassword(user, NEW_PASSWORD, request);

        verify(kafkaTemplate).send(eq("notification.email.send"), any(SendEmailEvent.class));
    }

    @Test
    void shouldExecuteOperationsInCorrectOrder() {
        var inOrder = inOrder(
                passwordEncoder,
                userRepository,
                kafkaTemplate
        );

        passwordUpdateService.updatePassword(user, NEW_PASSWORD, request);

        inOrder.verify(passwordEncoder).encode(NEW_PASSWORD);
        inOrder.verify(userRepository).save(user);
        inOrder.verify(kafkaTemplate).send(eq("activity.account-changes"), any(AccountChangesActivityEvent.class));
        inOrder.verify(kafkaTemplate).send(eq("notification.email.send"), any(SendEmailEvent.class));
    }

    @Test
    void shouldResolveClientDataFromRequest() {
        passwordUpdateService.updatePassword(user, NEW_PASSWORD, request);

        verify(kafkaTemplate)
                .send(eq("activity.account-changes"), any(AccountChangesActivityEvent.class));
    }
}
