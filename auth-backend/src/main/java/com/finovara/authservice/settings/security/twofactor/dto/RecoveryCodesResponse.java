package com.finovara.authservice.settings.security.twofactor.dto;

import java.util.List;

public record RecoveryCodesResponse(List<String> backupCodes) {
}
