package com.finovara.finovarabackend.util.clientdata.ip.service.get;

import com.finovara.finovarabackend.util.clientdata.ip.ClientIp;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetClientIpAddressTest {

    @Mock
    private HttpServletRequest request;

    @Test
    void shouldReturnRemoteAddressWhenXForwardedForIsNull() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.10");

        try (MockedStatic<ClientIp> clientIpMock = mockStatic(ClientIp.class)) {
            clientIpMock.when(() -> ClientIp.getClientIpAddress(request))
                    .thenCallRealMethod();

            String result = ClientIp.getClientIpAddress(request);

            assertEquals("192.168.1.10", result);
        }
    }

    @Test
    void shouldReturnFirstIpFromXForwardedForWhenHeaderIsPresent() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 198.51.100.2");

        try (MockedStatic<ClientIp> clientIpMock = mockStatic(ClientIp.class)) {
            clientIpMock.when(() -> ClientIp.getClientIpAddress(request))
                    .thenCallRealMethod();

            String result = ClientIp.getClientIpAddress(request);

            assertEquals("203.0.113.1", result);
        }
    }
}