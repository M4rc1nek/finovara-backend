package com.finovara.finovarabackend.util.clientdata.metadata.service;

import com.finovara.finovarabackend.util.clientdata.browser.UserBrowser;
import com.finovara.finovarabackend.util.clientdata.ip.ClientIp;
import com.finovara.finovarabackend.util.clientdata.location.UserLocation;
import com.finovara.finovarabackend.util.clientdata.metadata.ClientData;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetClientDataTest {

    @Mock
    private UserLocation userLocation;
    @InjectMocks
    private ClientData clientData;

    @Mock
    private HttpServletRequest request;

    @Test
    void shouldReturnClientIpFromClientIpService() {
        try(MockedStatic<ClientIp> clientIpStatic = mockStatic(ClientIp.class)) {
            clientIpStatic.when(() -> ClientIp.getClientIpAddress(request)).thenReturn("192.168.1.10");

            String result = clientData.getClientIp(request);

            assertEquals("192.168.1.10", result);
        }
    }

    @Test
    void shouldReturnUserBrowserFromUserBrowserService() {
        try (MockedStatic<UserBrowser> browserMock = mockStatic(UserBrowser.class)) {
            browserMock.when(() -> UserBrowser.getBrowser(request)).thenReturn("Chrome");

            String result = clientData.getUserBrowser(request);

            assertEquals("Chrome", result);
        }
    }

    @Test
    void shouldReturnUserLocationFromUserLocationService() {
        when(userLocation.getLocationFromIp("8.8.8.8")).thenReturn("Warsaw, Poland");

        String result = clientData.getUserLocation("8.8.8.8");

        assertEquals("Warsaw, Poland", result);
    }
}