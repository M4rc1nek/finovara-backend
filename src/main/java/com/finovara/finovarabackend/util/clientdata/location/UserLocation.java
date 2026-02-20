package com.finovara.finovarabackend.util.clientdata.location;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class UserLocation {

    private final RestTemplate restTemplate = new RestTemplate();

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
