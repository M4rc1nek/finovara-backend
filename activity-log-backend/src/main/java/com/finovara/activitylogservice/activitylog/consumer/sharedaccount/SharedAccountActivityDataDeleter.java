package com.finovara.activitylogservice.activitylog.consumer.sharedaccount;

import com.finovara.activitylogservice.activitylog.sharedaccount.repository.SharedAccountActivityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SharedAccountActivityDataDeleter {

    private final SharedAccountActivityLogRepository sharedAccountActivityLogRepository;

    @Transactional
    public void deleteSharedAccountActivityLogData(Long ownerId, Long memberId) {
        sharedAccountActivityLogRepository.deleteByOwnerIdAndMemberId(ownerId, memberId);
        log.info("Shared account finance activities has been deleted.");
    }

}
