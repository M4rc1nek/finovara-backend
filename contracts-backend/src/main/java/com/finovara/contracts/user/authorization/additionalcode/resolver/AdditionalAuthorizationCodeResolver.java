package com.finovara.contracts.user.authorization.additionalcode.resolver;

import com.finovara.contracts.user.authorization.dto.ConfirmAuthorizationCodeDto;
import org.springframework.stereotype.Component;

@Component
public class AdditionalAuthorizationCodeResolver {

    public ConfirmAuthorizationCodeDto resolve(ConfirmAuthorizationCodeDto dto) {
        return new ConfirmAuthorizationCodeDto(dto != null ? dto.code() : null);
    }

    public ConfirmAuthorizationCodeDto resolve(String rawCode) {
        return new ConfirmAuthorizationCodeDto(rawCode);
    }
}
