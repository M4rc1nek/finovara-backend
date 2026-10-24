package com.finovara.activitylogservice.activitylog.mainaccount.secure.core;

import com.finovara.activitylogservice.activitylog.mainaccount.core.AccountActivityCore;

import java.util.List;

public abstract class SecurityActivityCore<E, AR, D> extends AccountActivityCore<E, D> {

    protected abstract void saveActivity(E activity);

    protected abstract List<E> findOldestForArchive(Long userId, int limit);

    protected abstract AR mapToArchive(E activity);

    protected abstract void archive(List<AR> archives);

    protected abstract void deleteActivities(List<E> activities);

    protected void moveToArchive(Long userId, int pageSize) {
        List<E> oldestActivities = findOldestForArchive(userId, pageSize + 1);

        if (oldestActivities.size() <= pageSize) {
            return;
        }

        List<E> activitiesToMove = oldestActivities.subList(0, pageSize);
        List<AR> archives = activitiesToMove.stream().map(this::mapToArchive).toList();

        archive(archives);
        deleteActivities(activitiesToMove);
    }
}