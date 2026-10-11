package com.finovara.activitylogservice.activitylog.mainaccount.secure.login.activity.mapper;

import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.activity.dto.LoginActivityDto;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.activity.model.LoginActivity;
import org.springframework.stereotype.Component;

@Component
public class LoginActivityMapper {

    public LoginActivityDto mapToDto(LoginActivity loginActivity) {
        return new LoginActivityDto(
                loginActivity.getType(),
                loginActivity.getStatus(),
                loginActivity.getCreatedAt(),
                loginActivity.getBrowser(),
                loginActivity.getIpAddress(),
                loginActivity.getLocation());
    }
}
