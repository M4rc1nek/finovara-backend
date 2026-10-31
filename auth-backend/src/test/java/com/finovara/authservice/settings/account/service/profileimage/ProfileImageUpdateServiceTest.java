package com.finovara.authservice.settings.account.service.profileimage;

import com.finovara.authservice.riskverification.service.RiskGuardService;
import com.finovara.authservice.settings.security.operationauthorization.service.AdditionalAuthorizationService;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.securitymonitoring.model.RiskTriggerType;
import com.finovara.contracts.user.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileImageUpdateServiceTest {

    @Mock
    private UserManagerService userManagerService;
    @Mock
    private OutboxService outboxService;
    @Mock
    private AdditionalAuthorizationService authorizationService;
    @Mock
    private AdditionalAuthorizationCodeResolver codeResolver;
    @Mock
    private RiskGuardService riskGuardService;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private ProfileImageUpdateService service;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "profileImagesDefaultDirectory", "images");
        user = User.builder().id(1L).email("user@example.com").profileImagePath("old.png").build();
    }

    @Nested
    class ReplaceProfileImage {
        @Test
        void shouldReplaceImageAndReturnPreviousPathWhenAuthorized() {
            when(userManagerService.getUserByIdOrThrow(1L)).thenReturn(user);

            String result = service.replaceProfileImage(1L, "new.png", request, "code", "event");

            assertEquals("old.png", result);
            assertEquals("new.png", user.getProfileImagePath());
            verify(riskGuardService).guard(1L, RiskTriggerType.PROFILE_IMAGE_CHANGED, "user@example.com", "event", request);
            verify(outboxService).save(any(), any(), any(), any());
        }
    }

    @Nested
    class ResetProfileImage {
        @Test
        void shouldResetImageAndReturnPreviousPathWhenCustomImageExists() {
            when(userManagerService.getUserByIdOrThrow(1L)).thenReturn(user);

            String result = service.resetProfileImage(1L, request, "code", "event");

            assertEquals("old.png", result);
            assertEquals("images\\UserProf.png", user.getProfileImagePath());
            verify(outboxService).save(any(), any(), any(), any());
        }

        @Test
        void shouldThrowExceptionWhenProfileImageIsAlreadyDefault() {
            user.setProfileImagePath("images/UserProf.png");
            when(userManagerService.getUserByIdOrThrow(1L)).thenReturn(user);

            assertThrows(IllegalArgumentException.class, () -> service.resetProfileImage(1L, request, "code", "event"));
            verify(riskGuardService, never()).guard(any(), any(), any(), any(), any());
        }
    }
}
