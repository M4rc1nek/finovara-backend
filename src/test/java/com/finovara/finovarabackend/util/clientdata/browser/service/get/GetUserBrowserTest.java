package com.finovara.finovarabackend.util.clientdata.browser.service.get;

import com.finovara.finovarabackend.util.clientdata.browser.UserBrowser;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetUserBrowserTest {

    @Mock
    private HttpServletRequest request;

    @Test
    void shouldReturnUnknownWhenUserAgentIsNull() {
        when(request.getHeader("User-Agent")).thenReturn(null);

        String result = UserBrowser.getBrowser(request);

        assertEquals("Unknown", result);
    }

    @Test
    void shouldReturnCorrectBrowserFromUserAgent() {
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                + "AppleWebKit/537.36 (KHTML, like Gecko) "
                + "Chrome/111.0.0.0 Safari/537.36";
        when(request.getHeader("User-Agent")).thenReturn(userAgent);

        String result = UserBrowser.getBrowser(request);

        assertEquals("Chrome", result);
    }
}