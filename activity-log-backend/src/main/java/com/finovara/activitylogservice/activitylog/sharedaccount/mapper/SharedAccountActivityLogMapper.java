package com.finovara.activitylogservice.activitylog.sharedaccount.mapper;

import com.finovara.activitylogservice.activitylog.sharedaccount.dto.SharedAccountActivityLogDto;
import com.finovara.activitylogservice.activitylog.sharedaccount.model.SharedAccountActivityLog;
import com.finovara.contracts.user.authorization.dto.UserDataDto;
import org.springframework.stereotype.Component;

@Component
public class SharedAccountActivityLogMapper {

    public SharedAccountActivityLogDto mapToDto(SharedAccountActivityLog activity, UserDataDto userDataDto) {
        return new SharedAccountActivityLogDto(
                activity.getUserId(),
                userDataDto.username(),
                userDataDto.email(),
                userDataDto.profileImagePath(),
                activity.getActivityType(),
                activity.getCreatedAt());
    }
}