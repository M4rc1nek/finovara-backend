package com.finovara.finovarabackend.util.clientdata.metadata;

import com.finovara.finovarabackend.util.clientdata.browser.UserBrowser;
import com.finovara.finovarabackend.util.clientdata.ip.ClientIp;
import com.finovara.finovarabackend.util.clientdata.location.UserLocation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClientData {

    private final UserLocation userLocation;
    private final ClientIp clientIp;
    private final UserBrowser userBrowser;

    public String getClientIp(HttpServletRequest request) {
        return clientIp.getClientIpAddress(request);
    }

    public String getUserLocation(String ip) {
        return userLocation.getLocationFromIp(ip);
    }

    public String getUserBrowser(HttpServletRequest request) {
        return userBrowser.getBrowser(request);
    }

}
