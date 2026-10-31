package com.finovara.activitylogservice.activitylog.consumer.sharedaccount;

import com.finovara.activitylogservice.activitylog.sharedaccount.model.SharedAccountActivityLog;
import com.finovara.activitylogservice.activitylog.sharedaccount.repository.SharedAccountActivityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SharedAccountActivityDataDeleter {

    private final SharedAccountActivityLogRepository sharedAccountActivityLogRepository;

    @Transactional
    public void deleteSharedAccountActivityLogData(Long ownerId, Long memberId) {
        List<SharedAccountActivityLog> sharedAccountActivityLog = sharedAccountActivityLogRepository.findAllByOwnerIdAndMemberId(ownerId, memberId);
        sharedAccountActivityLogRepository.deleteAll(sharedAccountActivityLog);
        log.info("Shared account finance activities have been deleted.");
    }

}
