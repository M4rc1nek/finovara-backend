package com.finovara.finovarabackend.accountactivity.accountchange.archive.service.archive;

import com.finovara.finovarabackend.accountactivity.accountchange.archive.model.AccountChangeArchive;
import com.finovara.finovarabackend.accountactivity.accountchange.archive.repository.AccountChangeArchiveRepository;
import com.finovara.finovarabackend.accountactivity.accountchange.archive.service.AccountChangeArchiveService;
import com.finovara.finovarabackend.config.TimeConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArchiveAccountChangesTest {

    @Mock
    private AccountChangeArchiveRepository accountChangeArchiveRepository;
    @Mock
    private TimeConfig timeConfig;

    @InjectMocks
    private AccountChangeArchiveService service;

    @Test
    void shouldArchiveMultipleActivities() {
        AccountChangeArchive activity1 = AccountChangeArchive.builder().build();
        AccountChangeArchive activity2 = AccountChangeArchive.builder().build();

        List<AccountChangeArchive> listToArchive = List.of(activity1, activity2);

        service.archive(listToArchive);

        verify(accountChangeArchiveRepository, times(1)).saveAll(listToArchive);
    }

    @Test
    void shouldHandleEmptyList() {
        List<AccountChangeArchive> emptyList = List.of();

        service.archive(emptyList);

        verify(accountChangeArchiveRepository, times(1)).saveAll(emptyList);
    }
}