package com.finovara.finovarabackend.accountactivity.secure.accountachange.activity;

import com.finovara.finovarabackend.accountactivity.secure.accountchange.activity.dto.AccountChangesActivityDto;
import com.finovara.finovarabackend.accountactivity.secure.accountchange.activity.model.AccountChangesActivity;
import com.finovara.finovarabackend.accountactivity.secure.accountchange.activity.model.AccountChangesActivityType;
import com.finovara.finovarabackend.accountactivity.secure.accountchange.activity.repository.AccountChangesActivityRepository;
import com.finovara.finovarabackend.accountactivity.secure.accountchange.activity.service.AccountChangesActivityService;
import com.finovara.finovarabackend.accountactivity.secure.accountchange.archive.model.AccountChangeArchive;
import com.finovara.finovarabackend.accountactivity.secure.accountchange.archive.service.AccountChangeArchiveService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.clientdata.metadata.ClientData;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import com.finovara.finovarabackend.util.confirmationpassword.service.PasswordValidator;
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
class AccountChangesActivityServiceTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private PasswordValidator passwordValidator;

    @Mock
    private ClientData clientData;

    @Mock
    private AccountChangesActivityRepository accountChangesActivityRepository;

    @Mock
    private AccountChangeArchiveService archiveService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AccountChangesActivityService accountChangesActivityService;

    private User user;
    private int pageSize = 10;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@test.com")
                .build();

        ReflectionTestUtils.setField(accountChangesActivityService, "pageSize", pageSize);
    }

    @Test
    void shouldCreateActivityAndNotArchiveWhenBelowThreshold() {
        when(userManagerService.getUserByIdOrThrow(1L)).thenReturn(user);
        when(clientData.getClientIp(request)).thenReturn("127.0.0.1");
        when(clientData.getUserBrowser(request)).thenReturn("Chrome");
        when(clientData.getUserLocation("127.0.0.1")).thenReturn("PL");

        when(accountChangesActivityRepository.countAccountChangesByUserAssignedId(1L)).thenReturn((long) (pageSize - 1));

        accountChangesActivityService.createAccountChangesActivity(1L, AccountChangesActivityType.PASSWORD_CHANGED, request);

        verify(accountChangesActivityRepository, times(1)).save(any(AccountChangesActivity.class));
        verify(archiveService, never()).archive(any());
    }

    @Test
    void shouldArchiveWhenThresholdExceeded() {
        when(userManagerService.getUserByIdOrThrow(1L)).thenReturn(user);
        when(clientData.getClientIp(request)).thenReturn("127.0.0.1");
        when(clientData.getUserBrowser(request)).thenReturn("Chrome");
        when(clientData.getUserLocation("127.0.0.1")).thenReturn("PL");

        when(accountChangesActivityRepository.countAccountChangesByUserAssignedId(1L)).thenReturn((long) (pageSize + 1));

        AccountChangesActivity activity = AccountChangesActivity.builder().id(1L).userAssigned(user).build();
        when(accountChangesActivityRepository.findFewByUserAssignedId(eq(1L), any(PageRequest.class))).thenReturn(List.of(activity));
        when(archiveService.mapToArchive(activity)).thenReturn(mock(AccountChangeArchive.class));

        accountChangesActivityService.createAccountChangesActivity(1L, AccountChangesActivityType.PASSWORD_CHANGED, request);

        verify(accountChangesActivityRepository, times(1)).save(any(AccountChangesActivity.class));
        verify(archiveService, times(1)).archive(anyList());
        verify(accountChangesActivityRepository, times(1)).deleteAll(anyList());
    }

    @Test
    void shouldReturnActivitiesDto() {
        Long userId = 1L;
        List<AccountChangesActivityDto> dtos = List.of(mock(AccountChangesActivityDto.class));

        when(accountChangesActivityRepository.findByUserAssignedIdOrderByIdDesc(userId)).thenReturn(dtos);

        List<AccountChangesActivityDto> result = accountChangesActivityService.getAccountChangesActivity(userId);

        assertEquals(1, result.size());
        verify(accountChangesActivityRepository, times(1)).findByUserAssignedIdOrderByIdDesc(userId);
    }

    @Test
    void shouldConfirmPassword() {
        Long userId = 1L;
        ConfirmPasswordDto dto = mock(ConfirmPasswordDto.class);
        accountChangesActivityService.confirmPasswordToAccountChangesActivity(userId, dto);

        verify(passwordValidator, times(1)).validatePassword(userId, dto);
    }
}