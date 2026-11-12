package com.finovara.financeservice.revenue.processor;

import com.finovara.financeservice.revenue.model.Revenue;
import com.finovara.financeservice.revenue.repository.RevenueRepository;
import com.finovara.financeservice.trash.service.TrashMoverService;
import com.finovara.financeservice.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RevenueProcessor {

    private final RevenueRepository revenueRepository;
    private final WalletService walletService;
    private final TrashMoverService trashMoverService;

    @Transactional
    @CacheEvict(value = "revenue:suggestion", allEntries = true)
    public void moveAllRevenuesToTrash() {
        List<Long> userIds = revenueRepository.findUserIdsWithActiveRevenues().stream().sorted().toList();
        if (userIds.isEmpty()) {
            log.info("Moved 0 revenues to trash");
            return;
        }
        walletService.lockWalletsForUpdate(userIds);
        List<Revenue> revenues = revenueRepository.findAllActiveByUserIdInForUpdate(userIds);
        trashMoverService.moveRevenuesToTrash(revenues);
        log.info("Moved {} revenues to trash", revenues.size());
    }
}
