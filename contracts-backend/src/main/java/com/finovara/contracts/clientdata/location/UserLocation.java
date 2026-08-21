package com.finovara.contracts.clientdata.location;

import lombok.experimental.UtilityClass;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@UtilityClass
public class UserLocation {

    private static final RestTemplate REST_TEMPLATE = new RestTemplate();

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

            Map<String, Object> response =
                    REST_TEMPLATE.getForObject(url, Map.class);

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

    public static List<String> getLocationsFromIps(List<String> ips) {
        if (ips == null || ips.isEmpty()) {
            return List.of();
        }

        return ips.stream()
                .map(UserLocation::getLocationFromIp)
                .distinct()
                .toList();
    }
}