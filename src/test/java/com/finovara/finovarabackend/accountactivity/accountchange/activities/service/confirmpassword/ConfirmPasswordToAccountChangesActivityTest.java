package com.finovara.finovarabackend.accountactivity.accountchange.activities.service.confirmpassword;

import com.finovara.finovarabackend.accountactivity.accountchange.activities.service.AccountChangesActivityService;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import com.finovara.finovarabackend.util.confirmationpassword.service.PasswordConfirmationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ConfirmPasswordToAccountChangesActivityTest {

    @Mock
    private PasswordConfirmationService passwordConfirmationService;

    @InjectMocks
    private AccountChangesActivityService accountChangesActivityService;

    @Test
    void shouldCallPasswordConfirmationService() {
        ConfirmPasswordDto dto = new ConfirmPasswordDto("password");
        String email = "test@mail.com";

        accountChangesActivityService.confirmPasswordToAccountChangesActivity(email, dto);

        verify(passwordConfirmationService, times(1)).confirmPassword(email, dto);
    }
}
