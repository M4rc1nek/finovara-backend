package com.finovara.finovarabackend.accountactivity.secure.login.activity;

import com.finovara.finovarabackend.accountactivity.secure.login.activity.dto.LoginActivityDto;
import com.finovara.finovarabackend.accountactivity.secure.login.activity.model.LoginActivity;
import com.finovara.finovarabackend.accountactivity.secure.login.activity.model.LoginActivityStatus;
import com.finovara.finovarabackend.accountactivity.secure.login.activity.repository.LoginActivityRepository;
import com.finovara.finovarabackend.accountactivity.secure.login.activity.service.LoginActivityService;
import com.finovara.finovarabackend.accountactivity.secure.login.archive.model.LoginActivityArchive;
import com.finovara.finovarabackend.accountactivity.secure.login.archive.service.LoginActivityArchiveService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.clientdata.metadata.ClientData;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import com.finovara.finovarabackend.util.confirmationpassword.service.PasswordConfirmationService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginActivityServiceTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private PasswordConfirmationService passwordConfirmationService;

    @Mock
    private ClientData clientData;

    @Mock
    private LoginActivityRepository loginActivityRepository;

    @Mock
    private LoginActivityArchiveService archiveService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private LoginActivityService loginActivityService;

    private User user;
    private int pageSize = 10;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("user@test.com")
                .build();

        ReflectionTestUtils.setField(loginActivityService, "pageSize", pageSize);
    }

    @Test
    void shouldCreateActivityAndNotArchiveWhenBelowThreshold() {
        when(userManagerService.getUserByEmailOrThrow("user@test.com")).thenReturn(user);
        when(clientData.getClientIp(request)).thenReturn("127.0.0.1");
        when(clientData.getUserBrowser(request)).thenReturn("Firefox");
        when(clientData.getUserLocation("127.0.0.1")).thenReturn("PL");

        when(loginActivityRepository.countActivityLoginByUserAssignedId(1L)).thenReturn((long) (pageSize - 1));

        loginActivityService.createLoginActivity("user@test.com", LoginActivityStatus.successful, request);

        verify(loginActivityRepository, times(1)).save(any(LoginActivity.class));
        verify(archiveService, never()).archive(any());
    }

    @Test
    void shouldArchiveWhenThresholdExceeded() {
        when(userManagerService.getUserByEmailOrThrow("user@test.com")).thenReturn(user);
        when(clientData.getClientIp(request)).thenReturn("127.0.0.1");
        when(clientData.getUserBrowser(request)).thenReturn("Firefox");
        when(clientData.getUserLocation("127.0.0.1")).thenReturn("PL");

        when(loginActivityRepository.countActivityLoginByUserAssignedId(1L)).thenReturn((long) (pageSize + 1));

        LoginActivity activity = LoginActivity.builder().id(1L).userAssigned(user).build();
        when(loginActivityRepository.findOldestByUserAssignedId(eq(1L), any(PageRequest.class))).thenReturn(List.of(activity));
        when(archiveService.mapToArchive(activity)).thenReturn(mock(LoginActivityArchive.class));

        loginActivityService.createLoginActivity("user@test.com", LoginActivityStatus.successful, request);

        verify(loginActivityRepository, times(1)).save(any(LoginActivity.class));
        verify(archiveService, times(1)).archive(anyList());
        verify(loginActivityRepository, times(1)).deleteAll(anyList());
    }

    @Test
    void shouldReturnLoginActivityDto() {
        List<LoginActivityDto> dtos = List.of(mock(LoginActivityDto.class));

        when(loginActivityRepository.findByUserAssignedEmailOrderByDesc("user@test.com")).thenReturn(dtos);

        List<LoginActivityDto> result = loginActivityService.getLoginActivity("user@test.com");

        assertEquals(1, result.size());
        verify(loginActivityRepository, times(1)).findByUserAssignedEmailOrderByDesc("user@test.com");
    }

    @Test
    void shouldConfirmPassword() {
        ConfirmPasswordDto dto = mock(ConfirmPasswordDto.class);
        loginActivityService.confirmPassword("user@test.com", dto);

        verify(passwordConfirmationService, times(1)).confirmPassword("user@test.com", dto);
    }
}