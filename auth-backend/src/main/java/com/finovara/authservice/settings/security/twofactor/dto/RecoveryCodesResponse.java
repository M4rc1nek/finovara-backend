package com.finovara.authservice.twofactor.dto;

import java.util.List;

public record RecoveryCodesResponse(List<String> backupCodes) {
}
