package com.finovara.financeservice.trash.service;

import com.finovara.financeservice.trash.model.TrashEntityType;
import com.finovara.financeservice.trash.model.TrashItem;
import com.finovara.financeservice.trash.service.deletion.TrashPermanentDeletionStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrashPermanentDeletionServiceTest {

    @Mock
    private TrashPermanentDeletionStrategy strategy;

    @InjectMocks
    private TrashPermanentDeletionService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "strategies", List.of(strategy));
    }

    @Nested
    class Delete {
        @Test
        void shouldDelegateDeletionToMatchingStrategy() {
            TrashItem item = trashItem(TrashEntityType.EXPENSE, 10L);
            when(strategy.supports(TrashEntityType.EXPENSE)).thenReturn(true);

            service.delete(item);

            verify(strategy).delete(item);
        }

        @Test
        void shouldThrowExceptionWhenNoStrategySupportsItemType() {
            TrashItem item = trashItem(TrashEntityType.EXPENSE, 10L);

            assertThrows(IllegalStateException.class, () -> service.delete(item));

            verify(strategy).supports(TrashEntityType.EXPENSE);
        }
    }

    @Nested
    class DeleteAll {
        @Test
        void shouldGroupItemsAndDelegateEachTypeToMatchingStrategy() {
            TrashItem expense = trashItem(TrashEntityType.EXPENSE, 10L);
            TrashItem revenue = trashItem(TrashEntityType.REVENUE, 11L);
            when(strategy.supports(TrashEntityType.EXPENSE)).thenReturn(true);
            when(strategy.supports(TrashEntityType.REVENUE)).thenReturn(true);

            service.deleteAll(List.of(expense, revenue));

            verify(strategy).deleteAll(List.of(expense));
            verify(strategy).deleteAll(List.of(revenue));
        }

        @Test
        void shouldSplitLargeTypeGroupsIntoBoundedBatches() {
            List<TrashItem> items = new ArrayList<>();
            for (long entityId = 1; entityId <= 501; entityId++) {
                items.add(trashItem(TrashEntityType.EXPENSE, entityId));
            }
            when(strategy.supports(TrashEntityType.EXPENSE)).thenReturn(true);

            service.deleteAll(items);

            verify(strategy).deleteAll(items.subList(0, 500));
            verify(strategy).deleteAll(items.subList(500, 501));
        }

        @Test
        void shouldDoNothingWhenItemListIsEmpty() {
            service.deleteAll(List.of());

            verify(strategy, never()).deleteAll(anyList());
        }

        @Test
        void shouldThrowExceptionWhenItemHasNoEntityType() {
            TrashItem item = trashItem(null, 10L);

            assertThrows(NullPointerException.class, () -> service.deleteAll(List.of(item)));
        }

        @Test
        void shouldThrowExceptionWhenNoStrategySupportsItemType() {
            TrashItem item = trashItem(TrashEntityType.EXPENSE, 10L);

            assertThrows(IllegalStateException.class, () -> service.deleteAll(List.of(item)));
        }
    }

    private TrashItem trashItem(TrashEntityType type, Long entityId) {
        return TrashItem.builder().entityType(type).entityId(entityId).userId(2L).build();
    }
}
