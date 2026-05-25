package com.finovara.corebackend.security.oauth2.dto;

import com.finovara.corebackend.exception.badrequest.InvalidInputException;
import org.springframework.util.StringUtils;

import java.util.Map;

public record GoogleOAuth2UserInfo(
        String providerUserId,
        String email,
        String name,
        String picture
) {

    public static GoogleOAuth2UserInfo from(Map<String, Object> attributes) {
        String providerUserId = requiredString(attributes, "sub");
        String email = requiredString(attributes, "email");
        String name = optionalString(attributes, "name");
        String picture = optionalString(attributes, "picture");

        return new GoogleOAuth2UserInfo(providerUserId, email, name, picture);
    }

    private static String requiredString(Map<String, Object> attributes, String key) {
        String value = optionalString(attributes, key);
        if (!StringUtils.hasText(value)) {
            throw new InvalidInputException("Google account is missing required attribute: " + key);
        }
        return value;
    }

    private static String optionalString(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        return value instanceof String text ? text.trim() : null;
    }
}
