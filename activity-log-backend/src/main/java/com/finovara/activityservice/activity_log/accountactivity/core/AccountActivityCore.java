package com.finovara.activityservice.activity_log.accountactivity.core;

import com.finovara.contracts.model.SortType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RequiredArgsConstructor
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
