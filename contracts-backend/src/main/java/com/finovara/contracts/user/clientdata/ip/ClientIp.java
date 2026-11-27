package com.finovara.contracts.user.clientdata.ip;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;

import java.util.Random;

@UtilityClass
public class ClientIp {

    private static final boolean DEV_IP_OVERRIDE_ENABLED =
            Boolean.parseBoolean(System.getenv().getOrDefault("DEV_IP_OVERRIDE_ENABLED", "false"));

    private static final boolean DEV_IP_RANDOM_ENABLED =
            Boolean.parseBoolean(System.getenv().getOrDefault("DEV_IP_RANDOM_ENABLED", "false"));

    private static final Random RANDOM = new Random();

    public static String getClientIpAddress(HttpServletRequest request) {
        if (DEV_IP_OVERRIDE_ENABLED) {
            String debugIp = request.getHeader("X-Debug-Ip");
            if (debugIp != null && !debugIp.isBlank()) {
                return debugIp.trim();
            }
          


        }

        String xForwardedFor = request.getHeader("X-Forwarded-For");

        if (xForwardedFor == null) {
            return request.getRemoteAddr();
        }

        return xForwardedFor.split(",")[0].trim();
    }











}