package com.finovara.finovarabackend.accountactivity.login.activities.service.create;

import com.finovara.finovarabackend.accountactivity.login.activities.model.LoginActivityStatus;
import com.finovara.finovarabackend.accountactivity.login.activities.repository.LoginActivityRepository;
import com.finovara.finovarabackend.accountactivity.login.activities.service.LoginActivityService;
import com.finovara.finovarabackend.config.TimeConfig;
import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.clientdata.metadata.ClientData;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateLoginActivityTest {

    @Mock
    private UserManagerService userManagerService;
    @Mock
    private LoginActivityRepository loginActivityRepository;
    @Mock
    private TimeConfig timeConfig;
    @Mock
    private ClientData clientData;

    @InjectMocks
    private LoginActivityService loginActivityService;

    private final String EMAIL = "test@mail.com";

    @Test
    void ShouldCreateLoginActivitySuccessfully() {
        ReflectionTestUtils.setField(loginActivityService, "pageSize", 10);

        User user = new User();
        user.setId(1L);

        HttpServletRequest request = mock(HttpServletRequest.class);

        Clock fixedClock = Clock.fixed(Instant.parse("2026-03-15T12:00:00Z"), ZoneId.of("UTC"));

        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);
        when(timeConfig.clock()).thenReturn(fixedClock);
        when(clientData.getClientIp(request)).thenReturn("127.0.0.1");
        when(clientData.getUserBrowser(request)).thenReturn("Chrome");
        when(clientData.getUserLocation("127.0.0.1")).thenReturn("Poland");

        when(loginActivityRepository.countActivityLoginByUserAssignedId(user.getId())).thenReturn(5L);

        loginActivityService.createLoginActivity(EMAIL, LoginActivityStatus.successful, request);

        verify(loginActivityRepository).save(argThat(activity ->
                activity.getUserAssigned().equals(user) &&
                        activity.getStatus() == LoginActivityStatus.successful &&
                        activity.getType().equals("Login") &&
                        activity.getIpAddress().equals("127.0.0.1") &&
                        activity.getBrowser().equals("Chrome") &&
                        activity.getLocation().equals("Poland")
        ));
    }

    @Test
    void ShouldThrowExceptionWhenUserDoesNotExist() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () -> loginActivityService.createLoginActivity(EMAIL, LoginActivityStatus.unsuccessful, request));

        verify(loginActivityRepository, never()).save(any());
    }
}