package com.finovara.activitylogservice.activitylog.mainaccount.core;

import com.finovara.contracts.util.SortType;
import org.springframework.data.domain.Pageable;

import java.util.List;

public abstract class AccountActivityCore<T, D> {

    public List<D> getActivities(Long userId, SortType sort, int page, int pageSize) {
        return getRepositoryFindByUserId(userId, ActivityPageRequest.of(sort, page, pageSize))
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    protected abstract List<T> getRepositoryFindByUserId(Long userId, Pageable pageable);

    protected abstract D mapToDto(T entity);
}