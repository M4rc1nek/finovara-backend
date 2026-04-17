package com.finovara.finovarabackend.accountactivity.security.core;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.clientdata.metadata.ClientData;
import com.finovara.finovarabackend.util.confirmationpassword.service.PasswordConfirmationService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public abstract class SecurityActivityCore<A, AR> {

    protected final UserManagerService userManagerService;
    protected final PasswordConfirmationService passwordConfirmationService;
    protected final ClientData clientData;

    protected abstract void saveActivity(A activity);

    protected abstract long countActivities(Long userId);

    protected abstract List<A> findActivitiesToArchive(Long userId, int pageSize);

    protected abstract AR mapToArchive(A activity);

    protected abstract void archive(List<AR> archives);

    protected abstract void deleteActivities(List<A> activities);

    protected void moveToArchive(User user, int pageSize) {
        long count = countActivities(user.getId());

        if (count > pageSize) {
            List<A> toMove = findActivitiesToArchive(user.getId(), pageSize);
            List<AR> archives = toMove.stream().map(this::mapToArchive).toList();

            archive(archives);
            deleteActivities(toMove);
        }
    }
}

