package com.finovara.finovarabackend.accountactivity.accountchange.activities.service.get;

import com.finovara.finovarabackend.accountactivity.accountchange.activities.dto.AccountChangesActivityDto;
import com.finovara.finovarabackend.accountactivity.accountchange.activities.model.AccountChangesActivityType;
import com.finovara.finovarabackend.accountactivity.accountchange.activities.repository.AccountChangesActivityRepository;
import com.finovara.finovarabackend.accountactivity.accountchange.activities.service.AccountChangesActivityService;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import com.finovara.finovarabackend.util.confirmationpassword.service.PasswordConfirmationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAccountChangesActivityTest {

    @Mock
    private AccountChangesActivityRepository accountChangesActivityRepository;
    @Mock
    private PasswordConfirmationService passwordConfirmationService;

    @InjectMocks
    private AccountChangesActivityService accountChangesActivityService;

    private final String EMAIL = "test@mail.com";

    @Test
    void shouldReturnAccountChangesActivitiesForUser() {
        AccountChangesActivityDto accountChangesActivity = new AccountChangesActivityDto(
                AccountChangesActivityType.USERNAME_CHANGED,
                LocalDateTime.now(),
                "Firefox",
                "127.0.0.1",
                "Warsaw"
        );

        AccountChangesActivityDto accountChangesActivity2 = new AccountChangesActivityDto(
                AccountChangesActivityType.USERNAME_CHANGED,
                LocalDateTime.now(),
                "Chrome",
                "127.0.0.1",
                "France"
        );

        when(accountChangesActivityRepository.findByUserAssignedEmailOrderByIdDesc(EMAIL)).thenReturn(List.of(accountChangesActivity, accountChangesActivity2));
        List<AccountChangesActivityDto> result = accountChangesActivityService.getAccountChangesActivity(EMAIL);

        assertThat(result).hasSize(2);
        verify(accountChangesActivityRepository).findByUserAssignedEmailOrderByIdDesc(EMAIL);
    }

    @Test
    void shouldCallPasswordConfirmationService() {
        ConfirmPasswordDto dto = new ConfirmPasswordDto("password");

        accountChangesActivityService.confirmPasswordToAccountChangesActivity(EMAIL, dto);

        verify(passwordConfirmationService).confirmPassword(EMAIL, dto);
    }
}