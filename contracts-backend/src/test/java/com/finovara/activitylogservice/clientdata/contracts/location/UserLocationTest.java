package com.finovara.activityservice.clientdata.contracts.location;

import com.finovara.contracts.clientdata.location.UserLocation;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserLocationTest {

    @Test
    void shouldReturnLocalhostForLocalIp() {
        String ipv4Result = UserLocation.getLocationFromIp("127.0.0.1");
        String ipv6Result = UserLocation.getLocationFromIp("0:0:0:0:0:0:0:1");

        assertEquals("Localhost", ipv4Result);
        assertEquals("Localhost", ipv6Result);
    }

    @Test
    void shouldReturnDockerGatewayForDockerIp() {
        String result = UserLocation.getLocationFromIp("172.18.0.1");

        assertEquals("Host dockera (gateway)", result);
    }

    @Test
    void shouldReturnUnknownWhenIpIsNull() {
        String result = UserLocation.getLocationFromIp(null);

        assertEquals("Unknown", result);
    }

    @Test
    void shouldReturnRealLocationWhenApiReturnsValidResponse() {
        String publicIp = "8.8.8.8";
        Set<String> validLocations = Set.of("Mountain View, United States", "Ashburn, United States");

        String result = UserLocation.getLocationFromIp(publicIp);

        assertTrue(validLocations.contains(result));
    }

    @Test
    void shouldReturnUnknownForInvalidIpAddressFormat() {
        String invalidIp = "not-an-ip-address";

        String result = UserLocation.getLocationFromIp(invalidIp);

        assertEquals("Unknown", result);
    }
}
