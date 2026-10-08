package com.finovara.activitylogservice.activitylog.mainaccount.core;

import com.finovara.contracts.util.SortType;
import org.springframework.data.domain.Pageable;

import java.util.List;

public abstract class AccountActivityCore<T, D> {

    public List<D> getActivities(Long userId, SortType sort, int pageSize) {
        Pageable pageable = sort.getPageable(pageSize);
        return getRepositoryFindByUserId(userId, pageable)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    protected abstract List<T> getRepositoryFindByUserId(Long userId, Pageable pageable);

    protected abstract D mapToDto(T entity);
}
