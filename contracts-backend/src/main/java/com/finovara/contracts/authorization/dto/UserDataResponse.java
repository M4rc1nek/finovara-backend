package com.finovara.contracts.authorization.dto;

import java.util.Optional;

public record UserDataResponse(
        Long userId,
        Optional<String> username,
        Optional<String> email
) {
}
