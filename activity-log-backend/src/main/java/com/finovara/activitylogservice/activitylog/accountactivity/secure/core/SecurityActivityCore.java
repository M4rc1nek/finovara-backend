package com.finovara.activitylogservice.activitylog.accountactivity.secure.core;

import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public abstract class SecurityActivityCore<A, AR> {

    protected abstract void saveActivity(A activity);

    protected abstract long countActivities(Long userId);

    protected abstract List<A> findActivitiesToArchive(Long userId, int pageSize);

    protected abstract AR mapToArchive(A activity);

    protected abstract void archive(List<AR> archives);

    protected abstract void deleteActivities(List<A> activities);

    protected void moveToArchive(Long userId, int pageSize) {
        long count = countActivities(userId);

        if (count > pageSize) {
            List<A> toMove = findActivitiesToArchive(userId, pageSize);
            List<AR> archives = toMove.stream().map(this::mapToArchive).toList();

            archive(archives);
            deleteActivities(toMove);
        }
    }
}

