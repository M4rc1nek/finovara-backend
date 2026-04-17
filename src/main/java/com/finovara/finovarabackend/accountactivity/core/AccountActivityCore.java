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

	public List<D> getActivities(String email, SortType sort, int pageSize) {
		Pageable pageable = sort.getPageable(pageSize);
		return getRepositoryFindByUserEmail(email, pageable)
				.stream()
				.map(this::mapToDto)
				.toList();
	}

	protected abstract List<T> getRepositoryFindByUserEmail(String email, Pageable pageable);

	protected abstract D mapToDto(T entity);

	protected abstract T buildActivity(String email, S source);

	protected User getUser(String email) {
		return userManagerService.getUserByEmailOrThrow(email);
	}
}
