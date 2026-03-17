package com.finovara.finovarabackend.util.clientdata.location.service.get;

import com.finovara.finovarabackend.util.clientdata.location.UserLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class GetUserLocationTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private UserLocation userLocation;

    @Test
    void shouldReturnLocalhostForLocalIp() {
        String result = userLocation.getLocationFromIp("127.0.0.1");
        assertEquals("Localhost", result);

        result = userLocation.getLocationFromIp("0:0:0:0:0:0:0:1");
        assertEquals("Localhost", result);
    }

    @Test
    void shouldReturnCityAndCountryWhenApiReturnsValidResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("city", "Warsaw");
        response.put("country", "Poland");

        when(restTemplate.getForObject(anyString(), any(Class.class))).thenReturn(response);

        String result = userLocation.getLocationFromIp("8.8.8.8");

        assertEquals("Warsaw, Poland", result);
    }

    @Test
    void shouldReturnUnknownWhenApiReturnsNullValues() {
        Map<String, Object> response = new HashMap<>();
        response.put("city", null);
        response.put("country", null);

        when(restTemplate.getForObject(anyString(), any(Class.class))).thenReturn(response);

        String result = userLocation.getLocationFromIp("8.8.8.8");

        assertEquals("Unknown", result);
    }

    @Test
    void shouldReturnUnknownWhenApiThrowsException() {
        when(restTemplate.getForObject(anyString(), any(Class.class))).thenThrow(new RuntimeException());

        String result = userLocation.getLocationFromIp("8.8.8.8");

        assertEquals("Unknown", result);
    }
}