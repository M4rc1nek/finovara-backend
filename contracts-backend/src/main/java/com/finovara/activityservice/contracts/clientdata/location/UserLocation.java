package com.finovara.activityservice.contracts.clientdata.location;

import org.springframework.web.client.RestTemplate;
import java.util.Map;

public class UserLocation {

    public static String getLocationFromIp(String ip) {
        if (ip == null) {
            return "Unknown";
        }

        if (ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1")) {
            return "Localhost";
        }
        if (ip.equals("172.18.0.1")) {
            return "Host dockera (gateway)";
        }

        try {
            String url = "http://ip-api.com/json/" + ip;


            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null) {
                return "Unknown";
            }

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
