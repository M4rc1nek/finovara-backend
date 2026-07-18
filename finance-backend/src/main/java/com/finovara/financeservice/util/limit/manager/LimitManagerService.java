package com.finovara.financeservice.util.limit.manager;

import com.finovara.financeservice.limit.model.Limit;
import com.finovara.financeservice.limit.repository.LimitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LimitManagerService {
    private final LimitRepository limitRepository;

    public List<Limit> getLimitsByUserId(Long userId) {
        return limitRepository.findAllByUserId(userId);
    }
}
