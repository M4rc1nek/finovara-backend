package com.finovara.contracts.clientdata.ip;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.List;
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
            if (DEV_IP_RANDOM_ENABLED) {
                return randomPublicIp();
            }
        }

        String xForwardedFor = request.getHeader("X-Forwarded-For");

        if (xForwardedFor == null) {
            return request.getRemoteAddr();
        }

        return xForwardedFor.split(",")[0].trim();
    }

    private static String randomPublicIp() {
        int a;
        do {
            a = 1 + RANDOM.nextInt(223);
        } while (a == 10 || a == 127 || a == 169 || a == 172 || a == 192);
        int b = RANDOM.nextInt(256);
        int c = RANDOM.nextInt(256);
        int d = 1 + RANDOM.nextInt(254);
        return a + "." + b + "." + c + "." + d;
    }
}