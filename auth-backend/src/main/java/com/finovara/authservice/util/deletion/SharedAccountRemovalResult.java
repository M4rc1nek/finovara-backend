package com.finovara.authservice.util.deletion;

import com.finovara.authservice.sharedaccount.context.SharedAccountUsers;
import com.finovara.authservice.sharedaccount.dto.SharedAccountDetailsDto;

public record SharedAccountRemovalResult(SharedAccountDetailsDto details, SharedAccountUsers users) {
}