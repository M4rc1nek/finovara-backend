package com.finovara.finovarabackend.usersetting.accountsetting.account.service.getaccount;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.account.dto.AccountSettingsDto;
import com.finovara.finovarabackend.usersetting.account.service.AccountService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountSettingsGetTest {

    @Mock
    private UserManagerService userManagerService;

    @InjectMocks
    private AccountService accountService;

    @Test
    void shouldReturnAccountSettings() {

        String email = "test@test.com";

        User user = new User();
        user.setUsername("john123");
        user.setEmail(email);
        user.setCreatedAt(LocalDateTime.of(2024, 1, 10, 12, 0));
        user.setProfileImagePath("avatar.png");

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);

        AccountSettingsDto result = accountService.getAccountSettings(email);

        assertThat(result.username()).isEqualTo("john123");
        assertThat(result.email()).isEqualTo(email);
        assertThat(result.createdAt()).isEqualTo(user.getCreatedAt());
    }

    @Test
    void shouldReturnNullProfileImageWhenPathIsNull() {

        String email = "test@test.com";

        User user = new User();
        user.setUsername("john123");
        user.setEmail(email);
        user.setCreatedAt(LocalDateTime.of(2026, 3, 1, 12, 0));
        user.setProfileImagePath(null);

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);

        AccountSettingsDto result = accountService.getAccountSettings(email);

        assertThat(result.profileImageUrl()).isNull();
    }
}