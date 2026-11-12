package com.finovara.financeservice.trash.service;

import com.finovara.financeservice.trash.model.TrashEntityType;
import com.finovara.financeservice.trash.model.TrashItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrashRestoreServiceTest {

    @Mock
    private List<TrashRestoreStrategy> strategies;
    @Mock
    private TrashRestoreStrategy expenseStrategy;

    @InjectMocks
    private TrashRestoreService service;

    private TrashItem item;

    @BeforeEach
    void setUp() {
        item = TrashItem.builder().entityType(TrashEntityType.EXPENSE).build();
        when(strategies.stream()).thenReturn(List.of(expenseStrategy).stream());
        when(expenseStrategy.supports(TrashEntityType.EXPENSE)).thenReturn(true);
    }

    @Nested
    class Restore {
        @Test
        void shouldDelegateToStrategyMatchingEntityType() {
            service.restore(item);

            verify(expenseStrategy).restore(item);
        }

        @Test
        void shouldThrowExceptionWhenNoStrategySupportsEntityType() {
            when(expenseStrategy.supports(TrashEntityType.EXPENSE)).thenReturn(false);

            assertThrows(IllegalStateException.class, () -> service.restore(item));
        }
    }
}
