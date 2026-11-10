package com.finovara.financeservice.trash.processor;

import com.finovara.financeservice.trash.model.TrashItem;
import com.finovara.financeservice.trash.repository.TrashItemRepository;
import com.finovara.financeservice.trash.service.TrashPermanentDeletionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyList;

@ExtendWith(MockitoExtension.class)
class TrashProcessorTest {

    @Mock
    private TrashItemRepository trashItemRepository;
    @Mock
    private TrashPermanentDeletionService permanentDeletionService;

    @InjectMocks
    private TrashProcessor processor;

    @Nested
    class DeleteAllTrashes {
        private List<TrashItem> items;

        @BeforeEach
        void setUp() {
            items = List.of(TrashItem.builder().id(1L).build());
        }

        @Test
        void shouldDeleteSourceEntitiesBeforeTrashMetadata() {
            when(trashItemRepository.findAllForUpdate()).thenReturn(items);

            processor.deleteAllTrashes();

            org.mockito.InOrder inOrder = inOrder(permanentDeletionService, trashItemRepository);
            inOrder.verify(trashItemRepository).findAllForUpdate();
            inOrder.verify(permanentDeletionService).deleteAll(items);
            inOrder.verify(trashItemRepository).deleteAllInBatch(items);
        }

        @Test
        void shouldNotDeleteAnythingWhenTrashIsEmpty() {
            when(trashItemRepository.findAllForUpdate()).thenReturn(List.of());

            processor.deleteAllTrashes();

            verify(trashItemRepository, never()).deleteAllInBatch(anyList());
            verifyNoInteractions(permanentDeletionService);
        }

        @Test
        void shouldKeepTrashMetadataWhenSourceDeletionFails() {
            IllegalStateException failure = new IllegalStateException("Source entity deletion failed");
            when(trashItemRepository.findAllForUpdate()).thenReturn(items);
            doThrow(failure).when(permanentDeletionService).deleteAll(items);

            assertThrows(IllegalStateException.class, () -> processor.deleteAllTrashes());

            verify(trashItemRepository, never()).deleteAllInBatch(anyList());
        }
    }
}
