package com.finovara.finovarabackend.util.clientdata.browser;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import ua_parser.Client;
import ua_parser.Parser;

@Service
public class UserBrowser {

    public String getBrowser(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");

        if (userAgent == null) return "Unknown";

        Parser parser = new Parser();
        Client client = parser.parse(userAgent);

        return client.userAgent.family;
    }

}
