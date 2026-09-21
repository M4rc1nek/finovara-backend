package com.finovara.contracts.clientdata.location;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@UtilityClass
public class UserLocation {

    private static final int CONNECT_TIMEOUT_MS = 300;
    private static final int READ_TIMEOUT_MS = 500;

    private static final RestTemplate REST_TEMPLATE = buildRestTemplate();

    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        return new RestTemplate(factory);
    }

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

            Map<String, Object> response = REST_TEMPLATE.getForObject(url, Map.class);

            if (response == null) {
                return "Unknown";
            }

            String city = (String) response.get("city");
            String country = (String) response.get("country");

            if (city == null || country == null) {
                return "Unknown";
            }

            return city + ", " + country;

        } catch (RestClientException exception) {
            log.warn("Failed to resolve location for ip={}: {}", ip, exception.getMessage());
            return "Unknown";
        } catch (Exception exception) {
            log.error("Unexpected error while resolving location for ip={}", ip, exception);
            return "Unknown";
        }
    }
}