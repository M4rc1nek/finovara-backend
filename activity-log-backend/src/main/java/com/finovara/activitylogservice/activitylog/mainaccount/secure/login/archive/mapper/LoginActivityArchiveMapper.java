package com.finovara.activitylogservice.activitylog.mainaccount.secure.login.archive.mapper;

import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.archive.dto.LoginActivityArchiveDto;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.archive.model.LoginActivityArchive;
import org.springframework.stereotype.Component;

@Component
public class LoginActivityArchiveMapper {

    public LoginActivityArchiveDto mapToDto(LoginActivityArchive activityArchive) {
        return new LoginActivityArchiveDto(
                activityArchive.getType(),
                activityArchive.getStatus(),
                activityArchive.getMoveToArchiveDate(),
                activityArchive.getCreatedAt(),
                activityArchive.getBrowser(),
                activityArchive.getIpAddress(),
                activityArchive.getLocation());
    }
}
