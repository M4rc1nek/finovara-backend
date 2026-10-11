package com.finovara.activitylogservice.activitylog.mainaccount.secure.core;


import com.finovara.activitylogservice.activitylog.mainaccount.core.AccountActivityCore;

import java.util.List;
public abstract class SecurityActivityCore<E, AR, D> extends AccountActivityCore<E, D> {

    protected abstract void saveActivity(E activity);
    protected abstract long countActivities(Long userId);
    protected abstract List<E> findActivitiesToArchive(Long userId, int pageSize);
    protected abstract AR mapToArchive(E activity);
    protected abstract void archive(List<AR> archives);
    protected abstract void deleteActivities(List<E> activities);

    protected void moveToArchive(Long userId, int pageSize) {
        long count = countActivities(userId);

        if (count > pageSize) {
            List<E> toMove = findActivitiesToArchive(userId, pageSize);
            List<AR> archives = toMove.stream().map(this::mapToArchive).toList();

            archive(archives);
            deleteActivities(toMove);
        }
    }
}
