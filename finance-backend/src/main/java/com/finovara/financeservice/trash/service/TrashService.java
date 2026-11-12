package com.finovara.financeservice.trash.service;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.user.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.user.datadeletable.UserDataDeletable;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.trash.dto.TrashFilterDto;
import com.finovara.financeservice.trash.dto.TrashItemDto;
import com.finovara.financeservice.trash.model.TrashEntityType;
import com.finovara.financeservice.trash.model.TrashItem;
import com.finovara.financeservice.trash.repository.TrashItemRepository;
import com.finovara.financeservice.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrashService implements UserDataDeletable {

    private final TrashItemRepository trashItemRepository;
    private final TrashRestoreService trashRestoreService;
    private final TrashPermanentDeletionService permanentDeletionService;
    private final WalletService walletService;
    private final AuthBackendClient authBackendClient;
    private final AdditionalAuthorizationCodeResolver authorizationCodeResolver;

    @Value("${finance.pagination.page-size}")
    private int pageSize;


    @Transactional
    public void add(TrashEntityType type, Long entityId, Long userId, LocalDateTime deletedAt, Boolean restoreRecurringEnabled, boolean financialEffectsReverted) {
        trashItemRepository.save(TrashItem.builder()
                .entityType(type)
                .entityId(entityId)
                .userId(userId)
                .deletedAt(deletedAt)
                .restoreRecurringEnabled(restoreRecurringEnabled)
                .financialEffectsReverted(financialEffectsReverted)
                .build());
    }

    @Transactional
    public void addAll(List<TrashItem> items) {
        trashItemRepository.saveAll(items);
    }

    public List<TrashItemDto> getTrash(Long userId, int page) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.unsorted());
        return trashItemRepository.findAllForUser(userId, pageable);
    }

    public List<TrashItemDto> filterTrash(Long userId, TrashFilterDto filter, int page) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.unsorted());
        return trashItemRepository.findFilteredForUser(userId, filter, filter.deletedAt(), pageable);
    }

    @Transactional
    @CacheEvict(value = {"wallet:user", "expense:suggestion", "revenue:suggestion"}, key = "#userId")
    public void restore(Long userId, Long trashItemId, String authorizationCode) {
        authBackendClient.confirmAuthorizationCode(userId, authorizationCodeResolver.resolve(authorizationCode));
        TrashItem item = getItemForUpdate(userId, trashItemId);
        walletService.lockWalletForUpdate(userId);
        trashRestoreService.restore(item);
        trashItemRepository.delete(item);
    }

    @Transactional
    @CacheEvict(value = {"wallet:user", "expense:suggestion", "revenue:suggestion"}, key = "#userId")
    public void permanentlyDelete(Long userId, Long trashItemId, String authorizationCode) {
        authBackendClient.confirmAuthorizationCode(userId, authorizationCodeResolver.resolve(authorizationCode));
        TrashItem item = getItemForUpdate(userId, trashItemId);
        lockWalletUnlessPiggyBank(userId, item);
        permanentDeletionService.delete(item);
        trashItemRepository.delete(item);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        List<TrashItem> items = trashItemRepository.findAllForUserForUpdate(userId);
        trashItemRepository.deleteAllInBatch(items);
    }

    private TrashItem getItemForUpdate(Long userId, Long trashItemId) {
        return trashItemRepository.findByIdAndUserIdForUpdate(trashItemId, userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Trash item not found"));
    }

    private void lockWalletUnlessPiggyBank(Long userId, TrashItem item) {
        if (item.getEntityType() != TrashEntityType.PIGGY_BANK) {
            walletService.lockWalletForUpdate(userId);
        }
    }
}