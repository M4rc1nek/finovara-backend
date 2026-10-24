package com.finovara.activitylogservice.activitylog.mainaccount.core;

import com.finovara.contracts.util.SortType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class ActivityPageRequest {

    private static final String TIEBREAKER_PROPERTY = "id";

    private ActivityPageRequest() {
    }

    public static Pageable of(SortType sort, int page, int pageSize) {
        Sort baseSort = sort.getPageable(pageSize).getSort();
        Sort.Direction direction = baseSort.stream()
                .findFirst()
                .map(Sort.Order::getDirection)
                .orElse(Sort.Direction.DESC);
        return PageRequest.of(page, pageSize, baseSort.and(Sort.by(direction, TIEBREAKER_PROPERTY)));
    }
}