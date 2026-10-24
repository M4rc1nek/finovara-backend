package com.finovara.securitymonitoring.login.service;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.mainaccount.activity.event.secure.login.activity.LoginActivityEvent;
import com.finovara.contracts.mainaccount.activity.model.LoginActivityStatus;
import com.finovara.securitymonitoring.clientdata.model.ClientData;
import com.finovara.securitymonitoring.login.model.LoginProfile;
import com.finovara.securitymonitoring.login.repository.LoginProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginProfileUpdateServiceTest {

    private static final Long USER_ID = 1L;
    private static final String IP_ADDRESS = "192.168.0.1";
    private static final String OTHER_IP_ADDRESS = "10.0.0.5";
    private static final String BROWSER = "Chrome";
    private static final String OTHER_BROWSER = "Firefox";
    private static final String LOCATION = "Warsaw";
    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2025, 1, 15, 10, 30);

    @Mock
    private LoginProfileRepository loginProfileRepository;

    @Mock
    private LoginActivityEvent event;

    @InjectMocks
    private LoginProfileUpdateService service;

    private LoginProfile profile;

    @BeforeEach
    void setUpProfile() {
        profile = new LoginProfile();
        profile.setClientData(new ArrayList<>());
        profile.setLoginCount(0L);
    }

    private ClientData createClient(String ip, String browser) {
        return ClientData.builder()
                .knownIpAddress(ip)
                .knownBrowser(browser)
                .build();
    }

    @Nested
    class HandleLoginEvent {

        @Nested
        class IgnoredEvents {

            @ParameterizedTest
            @EnumSource(value = LoginActivityStatus.class, mode = EXCLUDE, names = "SUCCESSFUL")
            void shouldIgnoreEventWhenStatusIsNotSuccessful(LoginActivityStatus status) {
                when(event.status()).thenReturn(status);

                service.handleLoginEvent(event);

                verifyNoInteractions(loginProfileRepository);
            }
        }

        @Nested
        class ProfileNotFound {

            @BeforeEach
            void setUp() {
                when(event.status()).thenReturn(LoginActivityStatus.SUCCESSFUL);
                when(event.userId()).thenReturn(USER_ID);
                when(loginProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.empty());
            }

            @Test
            void shouldThrowExceptionWhenProfileNotFound() {
                assertThrows(RequestedEntityNotFoundException.class, () -> service.handleLoginEvent(event));
            }

            @Test
            void shouldThrowExceptionWithUserIdInMessageWhenProfileNotFound() {
                RequestedEntityNotFoundException exception = assertThrows(
                        RequestedEntityNotFoundException.class, () -> service.handleLoginEvent(event));

                assertEquals("Login profile not found for userId=" + USER_ID, exception.getMessage());
            }

            @Test
            void shouldNotSaveProfileWhenProfileNotFound() {
                assertThrows(RequestedEntityNotFoundException.class, () -> service.handleLoginEvent(event));

                verify(loginProfileRepository, never()).save(any(LoginProfile.class));
            }
        }

        @Nested
        class ProfileFound {

            @BeforeEach
            void setUp() {
                when(event.status()).thenReturn(LoginActivityStatus.SUCCESSFUL);
                when(event.userId()).thenReturn(USER_ID);
                when(event.ipAddress()).thenReturn(IP_ADDRESS);
                when(event.browser()).thenReturn(BROWSER);
                when(event.location()).thenReturn(LOCATION);
                when(event.occurredAt()).thenReturn(OCCURRED_AT);
                when(loginProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));
            }

            @Test
            void shouldAddNewClientWhenClientDataIsEmpty() {
                service.handleLoginEvent(event);

                assertEquals(1, profile.getClientData().size());
            }

            @Test
            void shouldAddNewClientWithEventDataWhenClientIsUnknown() {
                service.handleLoginEvent(event);

                ClientData addedClient = profile.getClientData().get(0);
                assertEquals(IP_ADDRESS, addedClient.getKnownIpAddress());
                assertEquals(LOCATION, addedClient.getKnownLocation());
                assertEquals(BROWSER, addedClient.getKnownBrowser());
                assertSame(profile, addedClient.getLoginProfile());
            }

            @Test
            void shouldAddNewClientWhenIpDiffersAndBrowserIsSame() {
                profile.getClientData().add(createClient(OTHER_IP_ADDRESS, BROWSER));

                service.handleLoginEvent(event);

                assertEquals(2, profile.getClientData().size());
            }

            @Test
            void shouldAddNewClientWhenBrowserDiffersAndIpIsSame() {
                profile.getClientData().add(createClient(IP_ADDRESS, OTHER_BROWSER));

                service.handleLoginEvent(event);

                assertEquals(2, profile.getClientData().size());
            }

            @Test
            void shouldAddNewClientWhenBothIpAndBrowserDiffer() {
                profile.getClientData().add(createClient(OTHER_IP_ADDRESS, OTHER_BROWSER));

                service.handleLoginEvent(event);

                assertEquals(2, profile.getClientData().size());
            }

            @Test
            void shouldKeepExistingClientsWhenNewClientAdded() {
                ClientData existingClient = createClient(OTHER_IP_ADDRESS, OTHER_BROWSER);
                profile.getClientData().add(existingClient);

                service.handleLoginEvent(event);

                assertSame(existingClient, profile.getClientData().get(0));
            }

            @Test
            void shouldNotAddClientWhenClientIsKnown() {
                profile.getClientData().add(createClient(IP_ADDRESS, BROWSER));

                service.handleLoginEvent(event);

                assertEquals(1, profile.getClientData().size());
            }

            @Test
            void shouldNotAddClientWhenKnownClientIsAmongMany() {
                profile.getClientData().add(createClient(OTHER_IP_ADDRESS, OTHER_BROWSER));
                profile.getClientData().add(createClient(IP_ADDRESS, BROWSER));
                profile.getClientData().add(createClient(OTHER_IP_ADDRESS, BROWSER));

                service.handleLoginEvent(event);

                assertEquals(3, profile.getClientData().size());
            }

            @Test
            void shouldUpdateLastLoginFieldsWhenClientIsUnknown() {
                service.handleLoginEvent(event);

                assertEquals(IP_ADDRESS, profile.getLastLoginIp());
                assertEquals(LOCATION, profile.getLastLoginLocation());
                assertEquals(BROWSER, profile.getLastLoginBrowser());
                assertEquals(OCCURRED_AT, profile.getLastLoginAt());
            }

            @Test
            void shouldUpdateLastLoginFieldsWhenClientIsKnown() {
                profile.getClientData().add(createClient(IP_ADDRESS, BROWSER));

                service.handleLoginEvent(event);

                assertEquals(IP_ADDRESS, profile.getLastLoginIp());
                assertEquals(LOCATION, profile.getLastLoginLocation());
                assertEquals(BROWSER, profile.getLastLoginBrowser());
                assertEquals(OCCURRED_AT, profile.getLastLoginAt());
            }

            @Test
            void shouldOverwritePreviousLastLoginFieldsWhenProfileHadPreviousLogin() {
                profile.setLastLoginIp(OTHER_IP_ADDRESS);
                profile.setLastLoginBrowser(OTHER_BROWSER);
                profile.setLastLoginLocation("Krakow");
                profile.setLastLoginAt(OCCURRED_AT.minusDays(1));

                service.handleLoginEvent(event);

                assertEquals(IP_ADDRESS, profile.getLastLoginIp());
                assertEquals(BROWSER, profile.getLastLoginBrowser());
                assertEquals(LOCATION, profile.getLastLoginLocation());
                assertEquals(OCCURRED_AT, profile.getLastLoginAt());
            }

            @Test
            void shouldIncrementLoginCountWhenCountIsZero() {
                service.handleLoginEvent(event);

                assertEquals(1L, profile.getLoginCount());
            }

            @Test
            void shouldIncrementLoginCountWhenCountIsPositive() {
                profile.setLoginCount(5L);

                service.handleLoginEvent(event);

                assertEquals(6L, profile.getLoginCount());
            }

            @Test
            void shouldIncrementLoginCountWhenClientIsKnown() {
                profile.getClientData().add(createClient(IP_ADDRESS, BROWSER));
                profile.setLoginCount(2L);

                service.handleLoginEvent(event);

                assertEquals(3L, profile.getLoginCount());
            }

            @Test
            void shouldSetUpdatedAtWhenLoginHandled() {
                LocalDateTime before = LocalDateTime.now();

                service.handleLoginEvent(event);

                assertNotNull(profile.getUpdatedAt());
                assertFalse(profile.getUpdatedAt().isBefore(before));
            }

            @Test
            void shouldSaveProfileWhenLoginHandled() {
                service.handleLoginEvent(event);

                verify(loginProfileRepository).findByUserIdForUpdate(USER_ID);
                verify(loginProfileRepository).save(profile);
                verifyNoMoreInteractions(loginProfileRepository);
            }

            @Test
            void shouldSaveProfileWhenClientIsKnown() {
                profile.getClientData().add(createClient(IP_ADDRESS, BROWSER));

                service.handleLoginEvent(event);

                verify(loginProfileRepository).save(profile);
            }
        }

        @Nested
        class NullClientValues {

            @BeforeEach
            void setUp() {
                when(event.status()).thenReturn(LoginActivityStatus.SUCCESSFUL);
                when(event.userId()).thenReturn(USER_ID);
                when(loginProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));
            }

            @Test
            void shouldAddClientWhenIpIsNullAndClientDataIsEmpty() {
                when(event.ipAddress()).thenReturn(null);
                when(event.browser()).thenReturn(BROWSER);
                when(event.location()).thenReturn(LOCATION);
                when(event.occurredAt()).thenReturn(OCCURRED_AT);

                service.handleLoginEvent(event);

                assertEquals(1, profile.getClientData().size());
                assertNull(profile.getClientData().get(0).getKnownIpAddress());
                assertNull(profile.getLastLoginIp());
            }

            @Test
            void shouldAddClientWhenBrowserIsNullAndClientDataIsEmpty() {
                when(event.ipAddress()).thenReturn(IP_ADDRESS);
                when(event.browser()).thenReturn(null);
                when(event.location()).thenReturn(LOCATION);
                when(event.occurredAt()).thenReturn(OCCURRED_AT);

                service.handleLoginEvent(event);

                assertEquals(1, profile.getClientData().size());
                assertNull(profile.getClientData().get(0).getKnownBrowser());
                assertNull(profile.getLastLoginBrowser());
            }

            @Test
            void shouldAddClientWhenBrowserIsNullAndIpDiffers() {
                profile.getClientData().add(createClient(OTHER_IP_ADDRESS, BROWSER));
                when(event.ipAddress()).thenReturn(IP_ADDRESS);
                when(event.browser()).thenReturn(null);
                when(event.location()).thenReturn(LOCATION);
                when(event.occurredAt()).thenReturn(OCCURRED_AT);

                service.handleLoginEvent(event);

                assertEquals(2, profile.getClientData().size());
            }

            @Test
            void shouldThrowExceptionWhenIpIsNullAndClientDataIsNotEmpty() {
                profile.getClientData().add(createClient(IP_ADDRESS, BROWSER));
                when(event.ipAddress()).thenReturn(null);
                when(event.browser()).thenReturn(BROWSER);

                assertThrows(NullPointerException.class, () -> service.handleLoginEvent(event));
            }

            @Test
            void shouldThrowExceptionWhenBrowserIsNullAndIpMatchesKnownClient() {
                profile.getClientData().add(createClient(IP_ADDRESS, BROWSER));
                when(event.ipAddress()).thenReturn(IP_ADDRESS);
                when(event.browser()).thenReturn(null);

                assertThrows(NullPointerException.class, () -> service.handleLoginEvent(event));
            }

            @Test
            void shouldNotSaveProfileWhenNullPointerExceptionThrown() {
                profile.getClientData().add(createClient(IP_ADDRESS, BROWSER));
                when(event.ipAddress()).thenReturn(null);
                when(event.browser()).thenReturn(BROWSER);

                assertThrows(NullPointerException.class, () -> service.handleLoginEvent(event));

                verify(loginProfileRepository, never()).save(any(LoginProfile.class));
            }
        }
    }

    @Nested
    class DeleteByUserId {

        @Test
        void shouldDeleteProfileWhenUserIdProvided() {
            service.deleteByUserId(USER_ID);

            verify(loginProfileRepository).deleteByUserId(USER_ID);
            verifyNoMoreInteractions(loginProfileRepository);
        }

        @Test
        void shouldDeleteProfileWhenUserIdIsNull() {
            service.deleteByUserId(null);

            verify(loginProfileRepository).deleteByUserId(null);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFailsOnDelete() {
            doThrow(new IllegalStateException("db error")).when(loginProfileRepository).deleteByUserId(USER_ID);

            assertThrows(IllegalStateException.class, () -> service.deleteByUserId(USER_ID));
        }
    }
}