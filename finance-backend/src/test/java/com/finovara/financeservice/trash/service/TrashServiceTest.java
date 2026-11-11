package com.finovara.financeservice.trash.service;

import com.finovara.contracts.user.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.trash.dto.TrashItemDto;
import com.finovara.financeservice.trash.model.TrashEntityType;
import com.finovara.financeservice.trash.model.TrashItem;
import com.finovara.financeservice.trash.repository.TrashItemRepository;
import com.finovara.financeservice.wallet.service.WalletService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrashServiceTest {

    private static final Long USER_ID = 12L;
    private static final Long TRASH_ITEM_ID = 33L;
    private static final Long EXPENSE_ID = 24L;
    private static final Long REVENUE_ID = 25L;
    private static final String AUTHORIZATION_CODE = "code";

    @Mock
    private TrashItemRepository trashItemRepository;
    @Mock
    private TrashRestoreService trashRestoreService;
    @Mock
    private TrashPermanentDeletionService permanentDeletionService;
    @Mock
    private WalletService walletService;
    @Mock
    private AuthBackendClient authBackendClient;
    @Mock
    private AdditionalAuthorizationCodeResolver authorizationCodeResolver;

    @Captor
    private ArgumentCaptor<List<TrashItem>> itemsCaptor;

    @InjectMocks
    private TrashService service;

    @Nested
    class Add {
        @Test
        void shouldStoreOnlyTrashMetadataWhenAdded() {
            LocalDateTime deletedAt = LocalDateTime.now();

            service.add(TrashEntityType.EXPENSE, EXPENSE_ID, USER_ID, deletedAt, null, true);

            ArgumentCaptor<TrashItem> itemCaptor = ArgumentCaptor.forClass(TrashItem.class);
            verify(trashItemRepository).save(itemCaptor.capture());
            TrashItem item = itemCaptor.getValue();
            assertEquals(TrashEntityType.EXPENSE, item.getEntityType());
            assertEquals(EXPENSE_ID, item.getEntityId());
            assertEquals(USER_ID, item.getUserId());
            assertEquals(deletedAt, item.getDeletedAt());
            assertNull(item.getRestoreRecurringEnabled());
            assertEquals(true, item.getFinancialEffectsReverted());
        }

        @Test
        void shouldSaveSingleAutomaticallyArchivedItem() {
            LocalDateTime deletedAt = LocalDateTime.now();
            TrashItem item = TrashItem.builder()
                    .entityType(TrashEntityType.EXPENSE)
                    .entityId(EXPENSE_ID)
                    .userId(USER_ID)
                    .deletedAt(deletedAt)
                    .financialEffectsReverted(false)
                    .build();

            service.addAll(List.of(item));

            verify(trashItemRepository).saveAll(List.of(item));
            assertEquals(false, item.getFinancialEffectsReverted());
        }

        @Test
        void shouldSaveMultipleAutomaticallyArchivedItemsTogether() {
            List<TrashItem> items = List.of(
                    TrashItem.builder().entityType(TrashEntityType.EXPENSE).entityId(EXPENSE_ID)
                            .userId(USER_ID).deletedAt(LocalDateTime.now()).financialEffectsReverted(false).build(),
                    TrashItem.builder().entityType(TrashEntityType.REVENUE).entityId(REVENUE_ID)
                            .userId(USER_ID).deletedAt(LocalDateTime.now()).financialEffectsReverted(false).build());

            service.addAll(items);

            verify(trashItemRepository).saveAll(itemsCaptor.capture());
            assertEquals(2, itemsCaptor.getValue().size());
            assertEquals(false, itemsCaptor.getValue().get(0).getFinancialEffectsReverted());
            assertEquals(false, itemsCaptor.getValue().get(1).getFinancialEffectsReverted());
        }
    }

    @Nested
    class GetTrash {
        @Test
        void shouldReturnDtosWhenUserHasTrashItems() {
            TrashItemDto dto = new TrashItemDto(TRASH_ITEM_ID, TrashEntityType.EXPENSE, EXPENSE_ID,
                    LocalDateTime.now(), null);
            when(trashItemRepository.findAllForUser(USER_ID)).thenReturn(List.of(dto));

            List<TrashItemDto> result = service.getTrash(USER_ID);

            assertEquals(1, result.size());
            assertEquals(TRASH_ITEM_ID, result.getFirst().id());
            assertEquals(TrashEntityType.EXPENSE, result.getFirst().entityType());
            assertEquals(EXPENSE_ID, result.getFirst().entityId());
        }
    }

    @Nested
    class DeleteByUserId {
        @Test
        void shouldLockAndDeleteTrashMetadataWhenUserIsDeleted() {
            TrashItem item = trashItem();
            when(trashItemRepository.findAllForUserForUpdate(USER_ID)).thenReturn(List.of(item));

            service.deleteByUserId(USER_ID);

            verify(trashItemRepository).findAllForUserForUpdate(USER_ID);
            verify(trashItemRepository).deleteAllInBatch(List.of(item));
        }
    }

    @Nested
    class Restore {
        @Test
        void shouldLockWalletAndTrashItemBeforeDelegatingRestore() {
            TrashItem item = trashItem();
            when(trashItemRepository.findByIdAndUserIdForUpdate(TRASH_ITEM_ID, USER_ID))
                    .thenReturn(Optional.of(item));

            service.restore(USER_ID, TRASH_ITEM_ID, AUTHORIZATION_CODE);

            InOrder inOrder = inOrder(walletService, trashItemRepository, trashRestoreService);
            inOrder.verify(trashItemRepository).findByIdAndUserIdForUpdate(TRASH_ITEM_ID, USER_ID);
            inOrder.verify(walletService).lockWalletForUpdate(USER_ID);
            inOrder.verify(trashRestoreService).restore(item);
            inOrder.verify(trashItemRepository).delete(item);
            verify(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());
        }

        @Test
        void shouldLockWalletWhenRestoringPiggyBank() {
            TrashItem item = TrashItem.builder()
                    .id(TRASH_ITEM_ID)
                    .entityId(40L)
                    .entityType(TrashEntityType.PIGGY_BANK)
                    .userId(USER_ID)
                    .build();
            when(trashItemRepository.findByIdAndUserIdForUpdate(TRASH_ITEM_ID, USER_ID))
                    .thenReturn(Optional.of(item));

            service.restore(USER_ID, TRASH_ITEM_ID, AUTHORIZATION_CODE);

            verify(walletService).lockWalletForUpdate(USER_ID);
            verify(trashRestoreService).restore(item);
        }
    }

    @Nested
    class PermanentlyDelete {
        @Test
        void shouldDeleteSourceBeforeRemovingTrashMetadata() {
            TrashItem item = trashItem();
            when(trashItemRepository.findByIdAndUserIdForUpdate(TRASH_ITEM_ID, USER_ID))
                    .thenReturn(Optional.of(item));

            service.permanentlyDelete(USER_ID, TRASH_ITEM_ID, AUTHORIZATION_CODE);

            InOrder inOrder = inOrder(walletService, trashItemRepository, permanentDeletionService);
            inOrder.verify(trashItemRepository).findByIdAndUserIdForUpdate(TRASH_ITEM_ID, USER_ID);
            inOrder.verify(walletService).lockWalletForUpdate(USER_ID);
            inOrder.verify(permanentDeletionService).delete(item);
            inOrder.verify(trashItemRepository).delete(item);
        }

        @Test
        void shouldNotLockWalletWhenPermanentlyDeletingPiggyBank() {
            TrashItem item = TrashItem.builder()
                    .id(TRASH_ITEM_ID)
                    .entityId(40L)
                    .entityType(TrashEntityType.PIGGY_BANK)
                    .userId(USER_ID)
                    .build();
            when(trashItemRepository.findByIdAndUserIdForUpdate(TRASH_ITEM_ID, USER_ID))
                    .thenReturn(Optional.of(item));

            service.permanentlyDelete(USER_ID, TRASH_ITEM_ID, AUTHORIZATION_CODE);

            verify(permanentDeletionService).delete(item);
            verify(walletService, never()).lockWalletForUpdate(USER_ID);
        }
    }

    private TrashItem trashItem() {
        return TrashItem.builder()
                .id(TRASH_ITEM_ID)
                .entityId(EXPENSE_ID)
                .entityType(TrashEntityType.EXPENSE)
                .userId(USER_ID)
                .deletedAt(LocalDateTime.now())
                .build();
    }
}
