package com.finovara.finovarabackend.util.clientdata.browser;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;
import ua_parser.Client;
import ua_parser.Parser;

@UtilityClass
public class UserBrowser {

    private static final Parser parser = new Parser();

    public static String getBrowser(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");

        if (userAgent == null) return "Unknown";

        Client client = parser.parse(userAgent);
        return client.userAgent.family;
    }
}
