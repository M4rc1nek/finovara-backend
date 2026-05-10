package com.finovara.finovarabackend.security;

import com.finovara.finovarabackend.security.jwt.CustomUserDetails;
import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@UtilityClass
public class SecurityUtils {

    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user in security context");
        }

        Object principal = authentication.getPrincipal();
        if (principal == null || "anonymousUser".equals(principal)) {
            throw new IllegalStateException("No authenticated user in security context");
        }

        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails.getId();
        }

        if (principal instanceof String principalValue) {
            try {
                return Long.parseLong(principalValue);
            } catch (NumberFormatException ex) {
                throw new IllegalStateException("Authenticated principal does not contain numeric user id", ex);
            }
        }

        throw new IllegalStateException("Unsupported principal type: " + principal.getClass().getName());
    }
}
