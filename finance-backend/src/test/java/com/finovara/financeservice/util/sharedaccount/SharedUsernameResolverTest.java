package com.finovara.financeservice.util.sharedaccount;

import com.finovara.contracts.user.authorization.dto.UserDataDto;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedUsernameResolverTest {

    @Mock
    private AuthBackendClient authBackendClient;

    @InjectMocks
    private SharedUsernameResolver resolver;

    @Test
    void shouldLoadDistinctUsernamesWithOneBatchRequest() {
        when(authBackendClient.getUserSessions(List.of(1L, 2L))).thenReturn(List.of(
                new UserDataDto(1L, "owner", "owner@example.com", null),
                new UserDataDto(2L, "member", "member@example.com", null)
        ));

        Map<Long, String> result = resolver.resolve(List.of(1L, 2L, 1L));

        assertEquals(Map.of(1L, "owner", 2L, "member"), result);
        verify(authBackendClient).getUserSessions(List.of(1L, 2L));
    }

    @Test
    void shouldNotCallAuthBackendForEmptyIdCollection() {
        assertEquals(Map.of(), resolver.resolve(Set.of()));

        verifyNoInteractions(authBackendClient);
    }
}
