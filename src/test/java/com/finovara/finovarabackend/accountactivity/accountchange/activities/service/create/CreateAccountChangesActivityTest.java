package com.finovara.finovarabackend.accountactivity.accountchange.activities.service.create;

import com.finovara.finovarabackend.accountactivity.security.accountchange.activities.model.AccountChangesActivity;
import com.finovara.finovarabackend.accountactivity.security.accountchange.activities.model.AccountChangesActivityType;
import com.finovara.finovarabackend.accountactivity.security.accountchange.activities.repository.AccountChangesActivityRepository;
import com.finovara.finovarabackend.accountactivity.security.accountchange.activities.service.AccountChangesActivityService;
import com.finovara.finovarabackend.accountactivity.security.accountchange.archive.model.AccountChangeArchive;
import com.finovara.finovarabackend.accountactivity.security.accountchange.archive.service.AccountChangeArchiveService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.clientdata.metadata.ClientData;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateAccountChangesActivityTest {

    @Mock
    private UserManagerService userManagerService;
    @Mock
    private AccountChangesActivityRepository accountChangesActivityRepository;
    @Mock
    private ClientData clientData;
    @Mock
    private AccountChangeArchiveService accountChangeArchiveService;

    @InjectMocks
    private AccountChangesActivityService accountChangesActivityService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(accountChangesActivityService, "pageSize", 1);
    }

    @Test
    void shouldCreateAccountChangesActivityAndMoveToArchive() {

        String email = "test@example.com";
        HttpServletRequest request = mock(HttpServletRequest.class);

        User user = new User();
        user.setId(1L);
        user.setEmail(email);

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);

        when(clientData.getClientIp(request)).thenReturn("127.0.0.1");
        when(clientData.getUserBrowser(request)).thenReturn("Chrome");
        when(clientData.getUserLocation("127.0.0.1")).thenReturn("TestCity");

        when(accountChangesActivityRepository.countAccountChangesByUserAssignedId(1L)).thenReturn(2L);

        AccountChangesActivity activity = AccountChangesActivity.builder()
                .userAssigned(user)
                .type(AccountChangesActivityType.PASSWORD_CHANGED)
                .build();

        when(accountChangesActivityRepository.findFewByUserAssignedId(eq(1L), any())).thenReturn(List.of(activity));

        AccountChangeArchive archive = new AccountChangeArchive();

        when(accountChangeArchiveService.mapToArchive(any())).thenReturn(archive);

        LocalDateTime now = LocalDateTime.now();

        accountChangesActivityService.createAccountChangesActivity(email, AccountChangesActivityType.PASSWORD_CHANGED, request);

        verify(accountChangesActivityRepository).save(argThat(saved ->
                saved.getUserAssigned().equals(user) &&
                        saved.getType() == AccountChangesActivityType.PASSWORD_CHANGED &&
                        saved.getIpAddress().equals("127.0.0.1") &&
                        saved.getBrowser().equals("Chrome") &&
                        saved.getLocation().equals("TestCity") &&
                        !saved.getDate().isBefore(now)
        ));

        verify(userManagerService, times(2)).getUserByEmailOrThrow(email);
        verify(accountChangeArchiveService).archive(any());
        verify(accountChangesActivityRepository).deleteAll(any());
    }
}