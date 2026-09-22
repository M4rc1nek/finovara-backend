package com.finovara.securitymonitoring.accountchange.service;

import com.finovara.contracts.activity.event.secure.accountchange.activity.AccountChangesActivityEvent;
import com.finovara.contracts.model.activity.AccountChangesActivityType;
import com.finovara.securitymonitoring.accountchange.model.AccountChangeProfile;
import com.finovara.securitymonitoring.accountchange.repository.AccountChangeProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountChangeProfileUpdateServiceTest {

    @Mock
    private AccountChangeProfileRepository accountChangeProfileRepository;

    @InjectMocks
    private AccountChangeProfileUpdateService accountChangeProfileUpdateService;

    private static final Long USER_ID = 1L;
    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2024, 6, 1, 12, 0);

    private AccountChangesActivityEvent event;
    private AccountChangeProfile profile;

    @BeforeEach
    void setUp() {
        event = mock(AccountChangesActivityEvent.class);
        profile = mock(AccountChangeProfile.class);
        when(event.userId()).thenReturn(USER_ID);
        when(event.occurredAt()).thenReturn(OCCURRED_AT);
    }

    @Nested
    class HandleAccountChangeEvent {

        @BeforeEach
        void setUp() {
            when(accountChangeProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
        }

        @Test
        void shouldIncrementPasswordChangeCountWhenTypeIsPasswordChanged() {
            when(event.type()).thenReturn(AccountChangesActivityType.PASSWORD_CHANGED);
            when(profile.getPasswordChangeCount()).thenReturn(3L);

            accountChangeProfileUpdateService.handleAccountChangeEvent(event);

            verify(profile).setPasswordChangeCount(4L);
        }

        @Test
        void shouldSetLastPasswordChangeAtWhenTypeIsPasswordChanged() {
            when(event.type()).thenReturn(AccountChangesActivityType.PASSWORD_CHANGED);
            when(profile.getPasswordChangeCount()).thenReturn(0L);

            accountChangeProfileUpdateService.handleAccountChangeEvent(event);

            verify(profile).setLastPasswordChangeAt(OCCURRED_AT);
        }

        @Test
        void shouldNotChangeEmailUsernameOrProfileImageCountersWhenTypeIsPasswordChanged() {
            when(event.type()).thenReturn(AccountChangesActivityType.PASSWORD_CHANGED);
            when(profile.getPasswordChangeCount()).thenReturn(0L);

            accountChangeProfileUpdateService.handleAccountChangeEvent(event);

            verify(profile, never()).setEmailChangeCount(any(Long.class));
            verify(profile, never()).setUsernameChangeCount(any(Long.class));
            verify(profile, never()).setProfileImageChangeCount(any(Long.class));
        }

        @Test
        void shouldIncrementEmailChangeCountWhenTypeIsEmailChanged() {
            when(event.type()).thenReturn(AccountChangesActivityType.EMAIL_CHANGED);
            when(profile.getEmailChangeCount()).thenReturn(1L);

            accountChangeProfileUpdateService.handleAccountChangeEvent(event);

            verify(profile).setEmailChangeCount(2L);
        }

        @Test
        void shouldSetLastEmailChangeAtWhenTypeIsEmailChanged() {
            when(event.type()).thenReturn(AccountChangesActivityType.EMAIL_CHANGED);
            when(profile.getEmailChangeCount()).thenReturn(0L);

            accountChangeProfileUpdateService.handleAccountChangeEvent(event);

            verify(profile).setLastEmailChangeAt(OCCURRED_AT);
        }

        @Test
        void shouldIncrementUsernameChangeCountWhenTypeIsUsernameChanged() {
            when(event.type()).thenReturn(AccountChangesActivityType.USERNAME_CHANGED);
            when(profile.getUsernameChangeCount()).thenReturn(5L);

            accountChangeProfileUpdateService.handleAccountChangeEvent(event);

            verify(profile).setUsernameChangeCount(6L);
        }

        @Test
        void shouldSetLastUsernameChangeAtWhenTypeIsUsernameChanged() {
            when(event.type()).thenReturn(AccountChangesActivityType.USERNAME_CHANGED);
            when(profile.getUsernameChangeCount()).thenReturn(0L);

            accountChangeProfileUpdateService.handleAccountChangeEvent(event);

            verify(profile).setLastUsernameChangeAt(OCCURRED_AT);
        }

        @Test
        void shouldIncrementProfileImageChangeCountWhenTypeIsProfileImgChanged() {
            when(event.type()).thenReturn(AccountChangesActivityType.PROFILE_IMG_CHANGED);
            when(profile.getProfileImageChangeCount()).thenReturn(2L);

            accountChangeProfileUpdateService.handleAccountChangeEvent(event);

            verify(profile).setProfileImageChangeCount(3L);
        }

        @Test
        void shouldSetLastProfileImageChangeAtWhenTypeIsProfileImgChanged() {
            when(event.type()).thenReturn(AccountChangesActivityType.PROFILE_IMG_CHANGED);
            when(profile.getProfileImageChangeCount()).thenReturn(0L);

            accountChangeProfileUpdateService.handleAccountChangeEvent(event);

            verify(profile).setLastProfileImageChangeAt(OCCURRED_AT);
        }

        @Test
        void shouldIncrementTotalChangeCountRegardlessOfType() {
            when(event.type()).thenReturn(AccountChangesActivityType.PASSWORD_CHANGED);
            when(profile.getPasswordChangeCount()).thenReturn(0L);
            when(profile.getTotalChangeCount()).thenReturn(9L);

            accountChangeProfileUpdateService.handleAccountChangeEvent(event);

            verify(profile).setTotalChangeCount(10L);
        }

        @Test
        void shouldSetLastChangeTypeAndLastChangeAtRegardlessOfType() {
            when(event.type()).thenReturn(AccountChangesActivityType.USERNAME_CHANGED);
            when(profile.getUsernameChangeCount()).thenReturn(0L);

            accountChangeProfileUpdateService.handleAccountChangeEvent(event);

            verify(profile).setLastChangeType(AccountChangesActivityType.USERNAME_CHANGED);
            verify(profile).setLastChangeAt(OCCURRED_AT);
        }

        @Test
        void shouldSetUpdatedAtRegardlessOfType() {
            when(event.type()).thenReturn(AccountChangesActivityType.EMAIL_CHANGED);
            when(profile.getEmailChangeCount()).thenReturn(0L);

            accountChangeProfileUpdateService.handleAccountChangeEvent(event);

            verify(profile).setUpdatedAt(any(LocalDateTime.class));
        }

        @Test
        void shouldSaveProfileAfterUpdating() {
            when(event.type()).thenReturn(AccountChangesActivityType.PASSWORD_CHANGED);
            when(profile.getPasswordChangeCount()).thenReturn(0L);

            accountChangeProfileUpdateService.handleAccountChangeEvent(event);

            verify(accountChangeProfileRepository).save(profile);
        }

        @Test
        void shouldNotIncrementAnySpecificCounterWhenTypeIsUnhandledByCases() {
            when(event.type()).thenReturn(AccountChangesActivityType.PROFILE_IMG_DELETED);
            when(profile.getTotalChangeCount()).thenReturn(0L);

            accountChangeProfileUpdateService.handleAccountChangeEvent(event);

            verify(profile, never()).setPasswordChangeCount(any(Long.class));
            verify(profile, never()).setEmailChangeCount(any(Long.class));
            verify(profile, never()).setUsernameChangeCount(any(Long.class));
            verify(profile, never()).setProfileImageChangeCount(any(Long.class));
            verify(profile).setTotalChangeCount(1L);
            verify(accountChangeProfileRepository).save(profile);
        }

        @Test
        void shouldFetchProfileByUserIdFromEvent() {
            when(event.type()).thenReturn(AccountChangesActivityType.PASSWORD_CHANGED);
            when(profile.getPasswordChangeCount()).thenReturn(0L);

            accountChangeProfileUpdateService.handleAccountChangeEvent(event);

            verify(accountChangeProfileRepository).findByUserId(USER_ID);
        }
    }
}