package com.finovara.securitymonitoring.login.service;

import com.finovara.contracts.activity.event.secure.login.activity.LoginActivityEvent;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.model.activity.LoginActivityStatus;
import com.finovara.securitymonitoring.clientdata.model.ClientData;
import com.finovara.securitymonitoring.login.model.LoginProfile;
import com.finovara.securitymonitoring.login.repository.LoginProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginProfileUpdateServiceTest {

    @Mock
    private LoginProfileRepository loginProfileRepository;

    @InjectMocks
    private LoginProfileUpdateService loginProfileUpdateService;

    @Nested
    class HandleLoginEvent {

        private LoginProfile profile;
        private LoginActivityEvent event;
        private LocalDateTime occurredAt;

        @BeforeEach
        void setUp() {
            occurredAt = LocalDateTime.of(2026, 1, 15, 10, 30);

            profile = LoginProfile.builder()
                    .id(1L)
                    .userId(100L)
                    .loginCount(5L)
                    .clientData(new ArrayList<>())
                    .createdAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                    .updatedAt(LocalDateTime.of(2026, 1, 10, 10, 0))
                    .build();

            event = new LoginActivityEvent(
                    100L,
                    LoginActivityStatus.SUCCESSFUL,
                    "Chrome",
                    "192.168.1.10",
                    "Warsaw",
                    occurredAt
            );
        }

        @Test
        void shouldUpdateLoginProfileWhenSuccessfulLoginEventIsReceived() {
            when(loginProfileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));

            loginProfileUpdateService.handleLoginEvent(event);

            assertEquals("192.168.1.10", profile.getLastLoginIp());
            assertEquals("Warsaw", profile.getLastLoginLocation());
            assertEquals("Chrome", profile.getLastLoginBrowser());
            assertEquals(occurredAt, profile.getLastLoginAt());
            assertEquals(6L, profile.getLoginCount());
            assertNotNull(profile.getUpdatedAt());

            verify(loginProfileRepository).findByUserId(100L);
            verify(loginProfileRepository).save(profile);
            verifyNoMoreInteractions(loginProfileRepository);
        }

        @Test
        void shouldAddNewClientDataWhenLoginComesFromUnknownClient() {
            when(loginProfileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));

            loginProfileUpdateService.handleLoginEvent(event);

            assertEquals(1, profile.getClientData().size());

            ClientData clientData = profile.getClientData().get(0);

            assertEquals("192.168.1.10", clientData.getKnownIpAddress());
            assertEquals("Warsaw", clientData.getKnownLocation());
            assertEquals("Chrome", clientData.getKnownBrowser());
            assertEquals(profile, clientData.getLoginProfile());

            verify(loginProfileRepository).findByUserId(100L);
            verify(loginProfileRepository).save(profile);
            verifyNoMoreInteractions(loginProfileRepository);
        }

        @Test
        void shouldNotAddClientDataWhenLoginComesFromKnownClient() {
            ClientData knownClient = ClientData.builder()
                    .knownIpAddress("192.168.1.10")
                    .knownBrowser("Chrome")
                    .knownLocation("Warsaw")
                    .loginProfile(profile)
                    .build();

            profile.setClientData(new ArrayList<>(List.of(knownClient)));

            when(loginProfileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));

            loginProfileUpdateService.handleLoginEvent(event);

            assertEquals(1, profile.getClientData().size());
            assertEquals(knownClient, profile.getClientData().get(0));
            assertEquals(6L, profile.getLoginCount());

            verify(loginProfileRepository).findByUserId(100L);
            verify(loginProfileRepository).save(profile);
            verifyNoMoreInteractions(loginProfileRepository);
        }

        @Test
        void shouldAddNewClientDataWhenIpIsKnownButBrowserIsDifferent() {
            ClientData knownClient = ClientData.builder()
                    .knownIpAddress("192.168.1.10")
                    .knownBrowser("Firefox")
                    .knownLocation("Warsaw")
                    .loginProfile(profile)
                    .build();

            profile.setClientData(new ArrayList<>(List.of(knownClient)));

            when(loginProfileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));

            loginProfileUpdateService.handleLoginEvent(event);

            assertEquals(2, profile.getClientData().size());
            assertEquals("Chrome", profile.getClientData().get(1).getKnownBrowser());

            verify(loginProfileRepository).findByUserId(100L);
            verify(loginProfileRepository).save(profile);
            verifyNoMoreInteractions(loginProfileRepository);
        }

        @Test
        void shouldAddNewClientDataWhenBrowserIsKnownButIpIsDifferent() {
            ClientData knownClient = ClientData.builder()
                    .knownIpAddress("10.0.0.1")
                    .knownBrowser("Chrome")
                    .knownLocation("Warsaw")
                    .loginProfile(profile)
                    .build();

            profile.setClientData(new ArrayList<>(List.of(knownClient)));

            when(loginProfileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));

            loginProfileUpdateService.handleLoginEvent(event);

            assertEquals(2, profile.getClientData().size());
            assertEquals("192.168.1.10", profile.getClientData().get(1).getKnownIpAddress());

            verify(loginProfileRepository).findByUserId(100L);
            verify(loginProfileRepository).save(profile);
            verifyNoMoreInteractions(loginProfileRepository);
        }

        @Test
        void shouldIncrementLoginCountWhenSuccessfulLoginEventIsReceived() {
            profile.setLoginCount(0L);

            when(loginProfileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));

            loginProfileUpdateService.handleLoginEvent(event);

            assertEquals(1L, profile.getLoginCount());

            verify(loginProfileRepository).findByUserId(100L);
            verify(loginProfileRepository).save(profile);
        }

        @Test
        void shouldIncrementLoginCountWhenLoginCountIsAlreadyPositive() {
            profile.setLoginCount(99L);

            when(loginProfileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));

            loginProfileUpdateService.handleLoginEvent(event);

            assertEquals(100L, profile.getLoginCount());

            verify(loginProfileRepository).findByUserId(100L);
            verify(loginProfileRepository).save(profile);
        }

        @Test
        void shouldHandleLoginEventWhenClientDataContainsMultipleClients() {
            ClientData firstClient = ClientData.builder()
                    .knownIpAddress("10.0.0.1")
                    .knownBrowser("Firefox")
                    .knownLocation("Krakow")
                    .loginProfile(profile)
                    .build();

            ClientData secondClient = ClientData.builder()
                    .knownIpAddress("10.0.0.2")
                    .knownBrowser("Edge")
                    .knownLocation("Gdansk")
                    .loginProfile(profile)
                    .build();

            profile.setClientData(new ArrayList<>(List.of(firstClient, secondClient)));

            when(loginProfileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));

            loginProfileUpdateService.handleLoginEvent(event);

            assertEquals(3, profile.getClientData().size());
            assertEquals("192.168.1.10", profile.getClientData().get(2).getKnownIpAddress());
            assertEquals("Chrome", profile.getClientData().get(2).getKnownBrowser());

            verify(loginProfileRepository).findByUserId(100L);
            verify(loginProfileRepository).save(profile);
        }

        @Test
        void shouldNotUpdateProfileWhenLoginStatusIsNotSuccessful() {
            LoginActivityEvent unsuccessfulEvent = new LoginActivityEvent(
                    100L,
                    null,
                    "Chrome",
                    "192.168.1.10",
                    "Warsaw",
                    occurredAt
            );

            loginProfileUpdateService.handleLoginEvent(unsuccessfulEvent);

            verifyNoInteractions(loginProfileRepository);
        }

        @Test
        void shouldNotUpdateProfileWhenLoginStatusIsNull() {
            LoginActivityEvent eventWithNullStatus = new LoginActivityEvent(
                    100L,
                    null,
                    "Chrome",
                    "192.168.1.10",
                    "Warsaw",
                    occurredAt
            );

            loginProfileUpdateService.handleLoginEvent(eventWithNullStatus);

            verifyNoInteractions(loginProfileRepository);
        }

        @Test
        void shouldThrowExceptionWhenLoginProfileDoesNotExist() {
            when(loginProfileRepository.findByUserId(100L)).thenReturn(Optional.empty());

            assertThrows(
                    RequestedEntityNotFoundException.class,
                    () -> loginProfileUpdateService.handleLoginEvent(event)
            );

            verify(loginProfileRepository).findByUserId(100L);
            verify(loginProfileRepository, never()).save(profile);
            verifyNoMoreInteractions(loginProfileRepository);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFailsToFindProfile() {
            when(loginProfileRepository.findByUserId(100L))
                    .thenThrow(new IllegalStateException());

            assertThrows(
                    IllegalStateException.class,
                    () -> loginProfileUpdateService.handleLoginEvent(event)
            );

            verify(loginProfileRepository).findByUserId(100L);
            verify(loginProfileRepository, never()).save(profile);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFailsToSaveProfile() {
            when(loginProfileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
            doThrow(new IllegalStateException())
                    .when(loginProfileRepository)
                    .save(profile);

            assertThrows(
                    IllegalStateException.class,
                    () -> loginProfileUpdateService.handleLoginEvent(event)
            );

            verify(loginProfileRepository).findByUserId(100L);
            verify(loginProfileRepository).save(profile);
        }

        @Test
        void shouldThrowExceptionWhenLoginCountIsNull() {
            profile.setLoginCount(null);

            when(loginProfileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));

            assertThrows(
                    NullPointerException.class,
                    () -> loginProfileUpdateService.handleLoginEvent(event)
            );

            verify(loginProfileRepository).findByUserId(100L);
            verify(loginProfileRepository, never()).save(profile);
        }

        @Test
        void shouldThrowExceptionWhenClientDataIsNull() {
            profile.setClientData(null);

            when(loginProfileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));

            assertThrows(
                    NullPointerException.class,
                    () -> loginProfileUpdateService.handleLoginEvent(event)
            );

            verify(loginProfileRepository).findByUserId(100L);
            verify(loginProfileRepository, never()).save(profile);
        }

        @Test
        void shouldThrowExceptionWhenEventIsNull() {
            assertThrows(
                    NullPointerException.class,
                    () -> loginProfileUpdateService.handleLoginEvent(null)
            );

            verifyNoInteractions(loginProfileRepository);
        }

        @Test
        void shouldHandleNullLocationWhenSuccessfulLoginEventIsReceived() {
            LoginActivityEvent eventWithNullLocation = new LoginActivityEvent(
                    100L,
                    LoginActivityStatus.SUCCESSFUL,
                    "Chrome",
                    "192.168.1.10",
                    null,
                    occurredAt
            );

            when(loginProfileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));

            loginProfileUpdateService.handleLoginEvent(eventWithNullLocation);

            assertEquals(null, profile.getLastLoginLocation());
            assertEquals(1, profile.getClientData().size());
            assertEquals(null, profile.getClientData().get(0).getKnownLocation());

            verify(loginProfileRepository).findByUserId(100L);
            verify(loginProfileRepository).save(profile);
        }

        @Test
        void shouldHandleNullOccurredAtWhenSuccessfulLoginEventIsReceived() {
            LoginActivityEvent eventWithNullOccurredAt = new LoginActivityEvent(
                    100L,
                    LoginActivityStatus.SUCCESSFUL,
                    "Chrome",
                    "192.168.1.10",
                    "Warsaw",
                    null
            );

            when(loginProfileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));

            loginProfileUpdateService.handleLoginEvent(eventWithNullOccurredAt);

            assertEquals(null, profile.getLastLoginAt());
            assertEquals(6L, profile.getLoginCount());

            verify(loginProfileRepository).findByUserId(100L);
            verify(loginProfileRepository).save(profile);
        }

        @Test
        void shouldThrowExceptionWhenIpAddressIsNullAndKnownClientExists() {
            ClientData knownClient = ClientData.builder()
                    .knownIpAddress("192.168.1.10")
                    .knownBrowser("Chrome")
                    .knownLocation("Warsaw")
                    .loginProfile(profile)
                    .build();

            profile.setClientData(new ArrayList<>(List.of(knownClient)));

            LoginActivityEvent eventWithNullIp = new LoginActivityEvent(
                    100L,
                    LoginActivityStatus.SUCCESSFUL,
                    "Chrome",
                    null,
                    "Warsaw",
                    occurredAt
            );

            when(loginProfileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));

            assertThrows(
                    NullPointerException.class,
                    () -> loginProfileUpdateService.handleLoginEvent(eventWithNullIp)
            );

            verify(loginProfileRepository).findByUserId(100L);
            verify(loginProfileRepository, never()).save(profile);
        }
    }

    @Nested
    class DeleteByUserId {

        @BeforeEach
        void setUp() {
        }

        @Test
        void shouldDeleteLoginProfileWhenUserIdIsValid() {
            loginProfileUpdateService.deleteByUserId(100L);

            verify(loginProfileRepository).deleteByUserId(100L);
            verifyNoMoreInteractions(loginProfileRepository);
        }

        @Test
        void shouldDeleteLoginProfileWhenUserIdIsZero() {
            loginProfileUpdateService.deleteByUserId(0L);

            verify(loginProfileRepository).deleteByUserId(0L);
            verifyNoMoreInteractions(loginProfileRepository);
        }

        @Test
        void shouldDeleteLoginProfileWhenUserIdIsNull() {
            loginProfileUpdateService.deleteByUserId(null);

            verify(loginProfileRepository).deleteByUserId(null);
            verifyNoMoreInteractions(loginProfileRepository);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFailsToDeleteProfile() {
            doThrow(new IllegalStateException())
                    .when(loginProfileRepository)
                    .deleteByUserId(100L);

            assertThrows(
                    IllegalStateException.class,
                    () -> loginProfileUpdateService.deleteByUserId(100L)
            );

            verify(loginProfileRepository).deleteByUserId(100L);
        }
    }
}
