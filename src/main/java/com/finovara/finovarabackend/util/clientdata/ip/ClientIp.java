package com.finovara.finovarabackend.util.clientdata.ip;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class ClientIp {

    public String getClientIpAddress(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For"); // proxy
        if (xfHeader == null) {
            return request.getRemoteAddr(); // get user ip
        }
        return xfHeader.split(",")[0];
    }
}
