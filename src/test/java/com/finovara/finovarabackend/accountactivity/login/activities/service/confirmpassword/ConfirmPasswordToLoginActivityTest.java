package com.finovara.finovarabackend.accountactivity.login.activities.service.confirmpassword;

import com.finovara.finovarabackend.accountactivity.login.activities.service.LoginActivityService;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import com.finovara.finovarabackend.util.confirmationpassword.service.PasswordConfirmationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmPasswordToLoginActivityTest {

    @Mock
    private PasswordConfirmationService passwordConfirmationService;

    @InjectMocks
    private LoginActivityService loginActivityService;

    @Test
    void shouldConfirmPasswordForLoginActivity() {
        String email = "test@example.com";
        ConfirmPasswordDto confirmPasswordDto = new ConfirmPasswordDto("password123");

        loginActivityService.confirmPasswordToLoginActivity(email, confirmPasswordDto);

        verify(passwordConfirmationService, times(1)).confirmPassword(email, confirmPasswordDto);
    }
}