package com.finovara.contracts.clientdata.ip;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.List;

@UtilityClass
public class ClientIp {

    public static String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");

        if (xForwardedFor == null) {
            return request.getRemoteAddr();
        }

        return xForwardedFor.split(",")[0].trim();
    }

    public static List<String> getClientIpAddresses(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");

        if (xForwardedFor == null) {
            return List.of(request.getRemoteAddr());
        }

        return Arrays.stream(xForwardedFor.split(","))
                .map(String::trim)
                .toList();
    }
}