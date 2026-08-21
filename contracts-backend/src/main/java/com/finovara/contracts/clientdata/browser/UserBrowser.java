package com.finovara.contracts.clientdata.browser;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;
import ua_parser.Client;
import ua_parser.Parser;

import java.util.List;

@UtilityClass
public class UserBrowser {

    private static final Parser parser = new Parser();

    public static String getBrowser(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");

        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown";
        }

        Client client = parser.parse(userAgent);
        return client.userAgent.family;
    }

    public static List<String> getBrowsers(HttpServletRequest request) {
        return List.of(getBrowser(request));
    }
}