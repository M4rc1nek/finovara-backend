package com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.archive.mapper;

import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.archive.dto.AccountChangeArchiveDto;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.archive.model.AccountChangeArchive;
import org.springframework.stereotype.Component;

@Component
public class AccountChangeArchiveMapper {

    public AccountChangeArchiveDto mapToDto(AccountChangeArchive accountChangeArchive) {
        return new AccountChangeArchiveDto(
                accountChangeArchive.getType(),
                accountChangeArchive.getMoveToArchiveDate(),
                accountChangeArchive.getActivityAccountChangeDate(),
                accountChangeArchive.getBrowser(),
                accountChangeArchive.getIpAddress(),
                accountChangeArchive.getLocation());
    }
}
