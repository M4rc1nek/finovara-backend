package com.finovara.securitymonitoring.accountchange.service;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.mainaccount.activity.event.secure.accountchange.activity.AccountChangeActivityEvent;
import com.finovara.contracts.mainaccount.activity.model.AccountChangeActivityType;
import com.finovara.securitymonitoring.accountchange.model.AccountChangeProfile;
import com.finovara.securitymonitoring.accountchange.repository.AccountChangeProfileRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountChangeProfileUpdateServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2025, 1, 15, 10, 30);

    @Mock
    private AccountChangeProfileRepository accountChangeProfileRepository;

    @Mock
    private AccountChangeActivityEvent event;

    @InjectMocks
    private AccountChangeProfileUpdateService service;

    private AccountChangeProfile profile;

    @BeforeEach
    void setUpProfile() {
        profile = new AccountChangeProfile();
        profile.setPasswordChangeCount(0L);
        profile.setEmailChangeCount(0L);
        profile.setUsernameChangeCount(0L);
        profile.setProfileImageChangeCount(0L);
        profile.setTotalChangeCount(0L);
    }

    @Nested
    class HandleAccountChangeEvent {

        @Nested
        class ProfileNotFound {

            @BeforeEach
            void setUp() {
                when(event.userId()).thenReturn(USER_ID);
                when(accountChangeProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.empty());
            }

            @Test
            void shouldThrowExceptionWhenProfileNotFound() {
                assertThrows(RequestedEntityNotFoundException.class, () -> service.handleAccountChangeEvent(event));
            }

            @Test
            void shouldThrowExceptionWithUserIdInMessageWhenProfileNotFound() {
                RequestedEntityNotFoundException exception = assertThrows(
                        RequestedEntityNotFoundException.class, () -> service.handleAccountChangeEvent(event));

                assertEquals("Account change profile not found for userId=" + USER_ID, exception.getMessage());
            }

            @Test
            void shouldNotSaveProfileWhenProfileNotFound() {
                assertThrows(RequestedEntityNotFoundException.class, () -> service.handleAccountChangeEvent(event));

                verify(accountChangeProfileRepository, never()).save(any(AccountChangeProfile.class));
            }
        }

        @Nested
        class PasswordChanged {

            @BeforeEach
            void setUp() {
                when(event.userId()).thenReturn(USER_ID);
                when(event.type()).thenReturn(AccountChangeActivityType.PASSWORD_CHANGED);
                when(event.occurredAt()).thenReturn(OCCURRED_AT);
                when(accountChangeProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));
            }

            @Test
            void shouldIncrementPasswordChangeCountWhenCountIsZero() {
                service.handleAccountChangeEvent(event);

                assertEquals(1L, profile.getPasswordChangeCount());
            }

            @Test
            void shouldIncrementPasswordChangeCountWhenCountIsPositive() {
                profile.setPasswordChangeCount(4L);

                service.handleAccountChangeEvent(event);

                assertEquals(5L, profile.getPasswordChangeCount());
            }

            @Test
            void shouldSetLastPasswordChangeAtWhenPasswordChanged() {
                service.handleAccountChangeEvent(event);

                assertEquals(OCCURRED_AT, profile.getLastPasswordChangeAt());
            }

            @Test
            void shouldOverwritePreviousLastPasswordChangeAtWhenPasswordChangedAgain() {
                profile.setLastPasswordChangeAt(OCCURRED_AT.minusDays(10));

                service.handleAccountChangeEvent(event);

                assertEquals(OCCURRED_AT, profile.getLastPasswordChangeAt());
            }

            @Test
            void shouldNotModifyOtherCountersWhenPasswordChanged() {
                service.handleAccountChangeEvent(event);

                assertEquals(0L, profile.getEmailChangeCount());
                assertEquals(0L, profile.getUsernameChangeCount());
                assertEquals(0L, profile.getProfileImageChangeCount());
            }

            @Test
            void shouldNotModifyOtherTimestampsWhenPasswordChanged() {
                service.handleAccountChangeEvent(event);

                assertNull(profile.getLastEmailChangeAt());
                assertNull(profile.getLastUsernameChangeAt());
                assertNull(profile.getLastProfileImageChangeAt());
            }
        }

        @Nested
        class EmailChanged {

            @BeforeEach
            void setUp() {
                when(event.userId()).thenReturn(USER_ID);
                when(event.type()).thenReturn(AccountChangeActivityType.EMAIL_CHANGED);
                when(event.occurredAt()).thenReturn(OCCURRED_AT);
                when(accountChangeProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));
            }

            @Test
            void shouldIncrementEmailChangeCountWhenCountIsZero() {
                service.handleAccountChangeEvent(event);

                assertEquals(1L, profile.getEmailChangeCount());
            }

            @Test
            void shouldIncrementEmailChangeCountWhenCountIsPositive() {
                profile.setEmailChangeCount(7L);

                service.handleAccountChangeEvent(event);

                assertEquals(8L, profile.getEmailChangeCount());
            }

            @Test
            void shouldSetLastEmailChangeAtWhenEmailChanged() {
                service.handleAccountChangeEvent(event);

                assertEquals(OCCURRED_AT, profile.getLastEmailChangeAt());
            }

            @Test
            void shouldOverwritePreviousLastEmailChangeAtWhenEmailChangedAgain() {
                profile.setLastEmailChangeAt(OCCURRED_AT.minusDays(10));

                service.handleAccountChangeEvent(event);

                assertEquals(OCCURRED_AT, profile.getLastEmailChangeAt());
            }

            @Test
            void shouldNotModifyOtherCountersWhenEmailChanged() {
                service.handleAccountChangeEvent(event);

                assertEquals(0L, profile.getPasswordChangeCount());
                assertEquals(0L, profile.getUsernameChangeCount());
                assertEquals(0L, profile.getProfileImageChangeCount());
            }

            @Test
            void shouldNotModifyOtherTimestampsWhenEmailChanged() {
                service.handleAccountChangeEvent(event);

                assertNull(profile.getLastPasswordChangeAt());
                assertNull(profile.getLastUsernameChangeAt());
                assertNull(profile.getLastProfileImageChangeAt());
            }
        }

        @Nested
        class UsernameChanged {

            @BeforeEach
            void setUp() {
                when(event.userId()).thenReturn(USER_ID);
                when(event.type()).thenReturn(AccountChangeActivityType.USERNAME_CHANGED);
                when(event.occurredAt()).thenReturn(OCCURRED_AT);
                when(accountChangeProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));
            }

            @Test
            void shouldIncrementUsernameChangeCountWhenCountIsZero() {
                service.handleAccountChangeEvent(event);

                assertEquals(1L, profile.getUsernameChangeCount());
            }

            @Test
            void shouldIncrementUsernameChangeCountWhenCountIsPositive() {
                profile.setUsernameChangeCount(2L);

                service.handleAccountChangeEvent(event);

                assertEquals(3L, profile.getUsernameChangeCount());
            }

            @Test
            void shouldSetLastUsernameChangeAtWhenUsernameChanged() {
                service.handleAccountChangeEvent(event);

                assertEquals(OCCURRED_AT, profile.getLastUsernameChangeAt());
            }

            @Test
            void shouldOverwritePreviousLastUsernameChangeAtWhenUsernameChangedAgain() {
                profile.setLastUsernameChangeAt(OCCURRED_AT.minusDays(10));

                service.handleAccountChangeEvent(event);

                assertEquals(OCCURRED_AT, profile.getLastUsernameChangeAt());
            }

            @Test
            void shouldNotModifyOtherCountersWhenUsernameChanged() {
                service.handleAccountChangeEvent(event);

                assertEquals(0L, profile.getPasswordChangeCount());
                assertEquals(0L, profile.getEmailChangeCount());
                assertEquals(0L, profile.getProfileImageChangeCount());
            }

            @Test
            void shouldNotModifyOtherTimestampsWhenUsernameChanged() {
                service.handleAccountChangeEvent(event);

                assertNull(profile.getLastPasswordChangeAt());
                assertNull(profile.getLastEmailChangeAt());
                assertNull(profile.getLastProfileImageChangeAt());
            }
        }

        @Nested
        class ProfileImageChanged {

            @BeforeEach
            void setUp() {
                when(event.userId()).thenReturn(USER_ID);
                when(event.type()).thenReturn(AccountChangeActivityType.PROFILE_IMG_CHANGED);
                when(event.occurredAt()).thenReturn(OCCURRED_AT);
                when(accountChangeProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));
            }

            @Test
            void shouldIncrementProfileImageChangeCountWhenCountIsZero() {
                service.handleAccountChangeEvent(event);

                assertEquals(1L, profile.getProfileImageChangeCount());
            }

            @Test
            void shouldIncrementProfileImageChangeCountWhenCountIsPositive() {
                profile.setProfileImageChangeCount(9L);

                service.handleAccountChangeEvent(event);

                assertEquals(10L, profile.getProfileImageChangeCount());
            }

            @Test
            void shouldSetLastProfileImageChangeAtWhenProfileImageChanged() {
                service.handleAccountChangeEvent(event);

                assertEquals(OCCURRED_AT, profile.getLastProfileImageChangeAt());
            }

            @Test
            void shouldOverwritePreviousLastProfileImageChangeAtWhenProfileImageChangedAgain() {
                profile.setLastProfileImageChangeAt(OCCURRED_AT.minusDays(10));

                service.handleAccountChangeEvent(event);

                assertEquals(OCCURRED_AT, profile.getLastProfileImageChangeAt());
            }

            @Test
            void shouldNotModifyOtherCountersWhenProfileImageChanged() {
                service.handleAccountChangeEvent(event);

                assertEquals(0L, profile.getPasswordChangeCount());
                assertEquals(0L, profile.getEmailChangeCount());
                assertEquals(0L, profile.getUsernameChangeCount());
            }

            @Test
            void shouldNotModifyOtherTimestampsWhenProfileImageChanged() {
                service.handleAccountChangeEvent(event);

                assertNull(profile.getLastPasswordChangeAt());
                assertNull(profile.getLastEmailChangeAt());
                assertNull(profile.getLastUsernameChangeAt());
            }
        }

        @Nested
        class CommonBehavior {

            @BeforeEach
            void setUp() {
                when(event.userId()).thenReturn(USER_ID);
                when(event.occurredAt()).thenReturn(OCCURRED_AT);
                when(accountChangeProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));
            }

            @ParameterizedTest
            @EnumSource(AccountChangeActivityType.class)
            void shouldIncrementTotalChangeCountWhenCountIsZero(AccountChangeActivityType type) {
                when(event.type()).thenReturn(type);

                service.handleAccountChangeEvent(event);

                assertEquals(1L, profile.getTotalChangeCount());
            }

            @ParameterizedTest
            @EnumSource(AccountChangeActivityType.class)
            void shouldIncrementTotalChangeCountWhenCountIsPositive(AccountChangeActivityType type) {
                profile.setTotalChangeCount(12L);
                when(event.type()).thenReturn(type);

                service.handleAccountChangeEvent(event);

                assertEquals(13L, profile.getTotalChangeCount());
            }

            @ParameterizedTest
            @EnumSource(AccountChangeActivityType.class)
            void shouldSetLastChangeTypeWhenChangeHandled(AccountChangeActivityType type) {
                when(event.type()).thenReturn(type);

                service.handleAccountChangeEvent(event);

                assertEquals(type, profile.getLastChangeType());
            }

            @ParameterizedTest
            @EnumSource(AccountChangeActivityType.class)
            void shouldSetLastChangeAtWhenChangeHandled(AccountChangeActivityType type) {
                when(event.type()).thenReturn(type);

                service.handleAccountChangeEvent(event);

                assertEquals(OCCURRED_AT, profile.getLastChangeAt());
            }

            @ParameterizedTest
            @EnumSource(AccountChangeActivityType.class)
            void shouldOverwritePreviousLastChangeFieldsWhenProfileHadPreviousChange(AccountChangeActivityType type) {
                profile.setLastChangeType(AccountChangeActivityType.PASSWORD_CHANGED);
                profile.setLastChangeAt(OCCURRED_AT.minusDays(3));
                when(event.type()).thenReturn(type);

                service.handleAccountChangeEvent(event);

                assertEquals(type, profile.getLastChangeType());
                assertEquals(OCCURRED_AT, profile.getLastChangeAt());
            }

            @ParameterizedTest
            @EnumSource(AccountChangeActivityType.class)
            void shouldSetUpdatedAtWhenChangeHandled(AccountChangeActivityType type) {
                when(event.type()).thenReturn(type);
                LocalDateTime before = LocalDateTime.now();

                service.handleAccountChangeEvent(event);

                assertNotNull(profile.getUpdatedAt());
                assertFalse(profile.getUpdatedAt().isBefore(before));
            }

            @ParameterizedTest
            @EnumSource(AccountChangeActivityType.class)
            void shouldSaveProfileWhenChangeHandled(AccountChangeActivityType type) {
                when(event.type()).thenReturn(type);

                service.handleAccountChangeEvent(event);

                verify(accountChangeProfileRepository).findByUserIdForUpdate(USER_ID);
                verify(accountChangeProfileRepository).save(profile);
                verifyNoMoreInteractions(accountChangeProfileRepository);
            }
        }

        @Nested
        class NullEventType {

            @BeforeEach
            void setUp() {
                when(event.userId()).thenReturn(USER_ID);
                when(event.type()).thenReturn(null);
                when(accountChangeProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));
            }

            @Test
            void shouldThrowExceptionWhenEventTypeIsNull() {
                assertThrows(NullPointerException.class, () -> service.handleAccountChangeEvent(event));
            }

            @Test
            void shouldNotSaveProfileWhenEventTypeIsNull() {
                assertThrows(NullPointerException.class, () -> service.handleAccountChangeEvent(event));

                verify(accountChangeProfileRepository, never()).save(any(AccountChangeProfile.class));
            }

            @Test
            void shouldNotModifyTotalChangeCountWhenEventTypeIsNull() {
                assertThrows(NullPointerException.class, () -> service.handleAccountChangeEvent(event));

                assertEquals(0L, profile.getTotalChangeCount());
            }
        }
    }

    @Nested
    class DeleteByUserId {

        @Test
        void shouldDeleteProfileWhenUserIdProvided() {
            when(accountChangeProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

            service.deleteByUserId(USER_ID);

            verify(accountChangeProfileRepository).findByUserId(USER_ID);
            verify(accountChangeProfileRepository).delete(profile);
        }

        @Test
        void shouldDeleteProfileWhenUserIdIsNull() {
            when(accountChangeProfileRepository.findByUserId(null)).thenReturn(Optional.of(profile));

            service.deleteByUserId(null);

            verify(accountChangeProfileRepository).findByUserId(null);
            verify(accountChangeProfileRepository).delete(profile);
        }

        @Test
        void shouldThrowExceptionWhenProfileNotFoundOnDelete() {
            when(accountChangeProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.deleteByUserId(USER_ID));
            verify(accountChangeProfileRepository, never()).delete(any(AccountChangeProfile.class));
        }
    }
}