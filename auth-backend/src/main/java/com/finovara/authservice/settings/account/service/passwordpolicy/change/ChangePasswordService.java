package com.finovara.authservice.settings.account.service.passwordpolicy.change;

import com.finovara.authservice.user.model.User;
import com.finovara.authservice.settings.account.dto.passwordpolicy.ChangePasswordDto;
import com.finovara.authservice.settings.account.service.verification.CredentialValidationService;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.authservice.settings.security.operationauthorization.service.AdditionalAuthorizationService;
import com.finovara.contracts.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChangePasswordService {

    private final UserManagerService userManagerService;
    private final CredentialValidationService credentialValidationService;
    private final PasswordUpdateService passwordUpdateService;
    private final AdditionalAuthorizationService additionalAuthorizationService;
    private final AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;

    public void changePassword(Long userId, ChangePasswordDto changePasswordDto, HttpServletRequest request) {
        additionalAuthorizationService.confirmAdditionalAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(changePasswordDto.authorizationCode()));
        
        User user = userManagerService.getUserByIdOrThrow(userId);
        String newPassword = changePasswordDto.newPassword();

        credentialValidationService.validateNewPassword(newPassword, changePasswordDto.confirmNewPassword(), user.getPassword());

        passwordUpdateService.updatePassword(user, newPassword, request);
    }
}