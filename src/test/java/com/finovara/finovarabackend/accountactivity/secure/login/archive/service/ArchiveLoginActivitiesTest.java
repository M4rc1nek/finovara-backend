package com.finovara.finovarabackend.accountactivity.secure.login.archive.service;

import com.finovara.finovarabackend.accountactivity.secure.login.archive.model.LoginActivityArchive;
import com.finovara.finovarabackend.accountactivity.secure.login.archive.repository.LoginActivityArchiveRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ArchiveLoginActivitiesTest {

    @Mock
    private LoginActivityArchiveRepository loginActivityArchiveRepository;
    @InjectMocks
    private LoginActivityArchiveService loginActivityArchiveService;

    @Test
    void shouldArchiveMultipleActivities() {
        LoginActivityArchive activity1 = LoginActivityArchive.builder().build();
        LoginActivityArchive activity2 = LoginActivityArchive.builder().build();

        List<LoginActivityArchive> listToArchive = List.of(activity1, activity2);

        loginActivityArchiveService.archive(listToArchive);

        verify(loginActivityArchiveRepository, times(1)).saveAll(listToArchive);
    }

    @Test
    void shouldHandleEmptyList() {
        List<LoginActivityArchive> emptyList = List.of();

        loginActivityArchiveService.archive(emptyList);

        verify(loginActivityArchiveRepository, times(1)).saveAll(emptyList);
    }
}