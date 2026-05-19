package com.finovara.finovarabackend.accountactivity.core;

import com.finovara.finovarabackend.util.model.SortType;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RequiredArgsConstructor
public abstract class AccountActivityCore<T, D, S> {

	protected final UserManagerService userManagerService;

	public List<D> getActivities(Long userId, SortType sort, int pageSize) {
		Pageable pageable = sort.getPageable(pageSize);
		return getRepositoryFindByUserId(userId, pageable)
				.stream()
				.map(this::mapToDto)
				.toList();
	}

	protected abstract List<T> getRepositoryFindByUserId(Long userId, Pageable pageable);

	protected abstract D mapToDto(T entity);

	protected abstract T buildActivity(Long userId, S source);

	protected User getUser(Long userId) {
		return userManagerService.getUserByIdOrThrow(userId);
	}
}
