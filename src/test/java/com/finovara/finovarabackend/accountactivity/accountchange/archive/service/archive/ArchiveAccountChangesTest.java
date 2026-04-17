package com.finovara.finovarabackend.accountactivity.accountchange.archive.service.archive;

import com.finovara.finovarabackend.accountactivity.security.accountchange.archive.model.AccountChangeArchive;
import com.finovara.finovarabackend.accountactivity.security.accountchange.archive.repository.AccountChangeArchiveRepository;
import com.finovara.finovarabackend.accountactivity.security.accountchange.archive.service.AccountChangeArchiveService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ArchiveAccountChangesTest {

    @Mock
    private AccountChangeArchiveRepository accountChangeArchiveRepository;
    @InjectMocks
    private AccountChangeArchiveService accountChangeArchiveService;

    @Test
    void shouldArchiveMultipleActivities() {
        AccountChangeArchive activity1 = AccountChangeArchive.builder().build();
        AccountChangeArchive activity2 = AccountChangeArchive.builder().build();

        List<AccountChangeArchive> listToArchive = List.of(activity1, activity2);

        accountChangeArchiveService.archive(listToArchive);

        verify(accountChangeArchiveRepository, times(1)).saveAll(listToArchive);
    }

    @Test
    void shouldHandleEmptyList() {
        List<AccountChangeArchive> emptyList = List.of();

        accountChangeArchiveService.archive(emptyList);

        verify(accountChangeArchiveRepository, times(1)).saveAll(emptyList);
    }
}