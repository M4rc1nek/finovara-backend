package com.finovara.finovarabackend.util.clientdata.location;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserLocation {

    private final RestTemplate restTemplate;

    public String getLocationFromIp(String ip) {

        if (ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1")) {
            return "Localhost";
        }

        try {
            String url = "http://ip-api.com/json/" + ip;

            Map<String, Object> response =
                    restTemplate.getForObject(url, Map.class);

            String city = (String) response.get("city");
            String country = (String) response.get("country");

            if (city == null || country == null) {
                return "Unknown";
            }

            return city + ", " + country;

        } catch (Exception e) {
            return "Unknown";
        }
    }
}
