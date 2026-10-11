package com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.activity.mapper;

import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.activity.dto.AccountChangeActivityDto;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.activity.model.AccountChangeActivity;
import org.springframework.stereotype.Component;

@Component
public class AccountChangeActivityMapper {

    public AccountChangeActivityDto mapToDto(AccountChangeActivity accountChangeActivity) {
        return new AccountChangeActivityDto(
                accountChangeActivity.getType(),
                accountChangeActivity.getCreatedAt(),
                accountChangeActivity.getBrowser(),
                accountChangeActivity.getIpAddress(),
                accountChangeActivity.getLocation());
    }
}
