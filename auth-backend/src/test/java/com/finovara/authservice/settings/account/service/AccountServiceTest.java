package com.finovara.authservice.settings.account.service;

import com.finovara.authservice.riskverification.service.RiskGuardService;
import com.finovara.authservice.settings.account.dto.AccountSettingsDto;
import com.finovara.authservice.settings.security.operationauthorization.service.AdditionalAuthorizationService;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.user.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserManagerService userManagerService;
    @Mock private OutboxService outboxService;
    @Mock private AdditionalAuthorizationService authorizationService;
    @Mock private AdditionalAuthorizationCodeResolver codeResolver;
    @Mock private RiskGuardService riskGuardService;
    @Mock private HttpServletRequest request;
    @InjectMocks private AccountService service;
    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("anna").email("a@x.com").createdAt(LocalDateTime.of(2026, 1, 1, 0, 0)).build();
    }

    @Nested
    class GetAccountSettings {
        @Test
        void shouldReturnCurrentSettingsWhenUserExists() {
            when(userManagerService.getUserByIdOrThrow(1L)).thenReturn(user);

            AccountSettingsDto result = service.getAccountSettings(1L);

            assertEquals("anna", result.username());
            assertEquals("a@x.com", result.email());
            assertEquals(user.getCreatedAt(), result.createdAt());
            assertEquals(null, result.authorizationCode());
        }
    }

    @Nested
    class UpdateUsername {
        @Test
        void shouldThrowExceptionWhenUsernameAlreadyExistsBeforeRiskCheck() {
            AccountSettingsDto dto = new AccountSettingsDto("bob", null, null, null, "code", "event");
            when(userManagerService.getUserByIdOrThrow(1L)).thenReturn(user);
            when(userRepository.existsByUsername("bob")).thenReturn(true);

            assertThrows(EntityAlreadyExistsException.class, () -> service.updateUsername(dto, 1L, request));
            verifyNoInteractions(riskGuardService, outboxService);
            verify(userRepository, never()).saveAndFlush(user);
        }

        @Test
        void shouldTranslateUniqueConstraintViolationToDomainException() {
            AccountSettingsDto dto = new AccountSettingsDto("bob", null, null, null, "code", "event");
            when(userManagerService.getUserByIdOrThrow(1L)).thenReturn(user);
            when(userRepository.existsByUsername("bob")).thenReturn(false);
            when(userRepository.saveAndFlush(user)).thenThrow(new DataIntegrityViolationException("duplicate"));

            assertThrows(EntityAlreadyExistsException.class, () -> service.updateUsername(dto, 1L, request));
            assertEquals("bob", user.getUsername());
            verifyNoInteractions(outboxService);
        }
    }
}
