package com.finovara.finovarabackend.accountactivity.accountchange.activities.service.move;

import com.finovara.finovarabackend.accountactivity.accountchange.activities.model.AccountChangesActivity;
import com.finovara.finovarabackend.accountactivity.accountchange.activities.repository.AccountChangesActivityRepository;
import com.finovara.finovarabackend.accountactivity.accountchange.activities.service.AccountChangesActivityService;
import com.finovara.finovarabackend.accountactivity.accountchange.archive.model.AccountChangeArchive;
import com.finovara.finovarabackend.accountactivity.accountchange.archive.service.AccountChangeArchiveService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MoveToArchiveAccountChangesActivityTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private AccountChangesActivityRepository accountChangesActivityRepository;

    @Mock
    private AccountChangeArchiveService accountChangeArchiveService;

    @InjectMocks
    private AccountChangesActivityService accountChangesActivityService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        ReflectionTestUtils.setField(accountChangesActivityService, "pageSize", 2);
    }

    @Test
    void shouldMoveToArchiveWhenCountExceedsPageSize() {
        AccountChangesActivity activity1 = new AccountChangesActivity();
        AccountChangesActivity activity2 = new AccountChangesActivity();
        List<AccountChangesActivity> activities = List.of(activity1, activity2);

        when(userManagerService.getUserByEmailOrThrow("test@example.com")).thenReturn(user);
        when(accountChangesActivityRepository.countAccountChangesByUserAssignedId(user.getId())).thenReturn(3L);
        when(accountChangesActivityRepository.findFewByUserAssignedId(user.getId(), PageRequest.of(0, 2)))
                .thenReturn(activities);
        when(accountChangeArchiveService.mapToArchive(activity1)).thenReturn(new AccountChangeArchive());
        when(accountChangeArchiveService.mapToArchive(activity2)).thenReturn(new AccountChangeArchive());

        accountChangesActivityService.moveToArchive("test@example.com");

        verify(userManagerService).getUserByEmailOrThrow("test@example.com");
        verify(accountChangesActivityRepository).countAccountChangesByUserAssignedId(user.getId());
        verify(accountChangesActivityRepository).findFewByUserAssignedId(user.getId(), PageRequest.of(0, 2));
        verify(accountChangeArchiveService, times(2)).mapToArchive(any());
        verify(accountChangeArchiveService).archive(anyList());
        verify(accountChangesActivityRepository).deleteAll(activities);
    }

    @Test
    void shouldNotMoveToArchiveWhenCountDoesNotExceedPageSize() {
        when(userManagerService.getUserByEmailOrThrow("test@example.com")).thenReturn(user);
        when(accountChangesActivityRepository.countAccountChangesByUserAssignedId(user.getId())).thenReturn(2L);

        accountChangesActivityService.moveToArchive("test@example.com");

        verify(userManagerService).getUserByEmailOrThrow("test@example.com");
        verify(accountChangesActivityRepository).countAccountChangesByUserAssignedId(user.getId());
        verify(accountChangesActivityRepository, never()).findFewByUserAssignedId(anyLong(), any());
        verify(accountChangeArchiveService, never()).archive(anyList());
        verify(accountChangesActivityRepository, never()).deleteAll(anyList());
    }
}