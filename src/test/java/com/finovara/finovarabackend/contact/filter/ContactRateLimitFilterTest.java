package com.finovara.finovarabackend.contact.filter;

import com.finovara.finovarabackend.exception.tomanyrequest.TooManyRequests;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactRateLimitFilterTest {

    @Mock
    private ContactRateLimitProperties properties;

    @Mock
    private FilterChain filterChain;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private ContactRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        when(properties.getMaxRequests()).thenReturn(3);
        when(properties.getWindowHours()).thenReturn(1);
        when(request.getRequestURI()).thenReturn("/api/contact");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
    }

    @Test
    void shouldPassRequestWhenUnderLimit() throws Exception {
        assertThatCode(() -> filter.doFilterInternal(request, response, filterChain))
                .doesNotThrowAnyException();

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldThrowTooManyRequestsWhenLimitExceeded() throws Exception {
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);

        assertThatThrownBy(() -> filter.doFilterInternal(request, response, filterChain))
                .isInstanceOf(TooManyRequests.class)
                .hasMessageContaining("Too many requests");
    }

    @Test
    void shouldTrackLimitsSeparatelyPerIp() throws Exception {
        HttpServletRequest requestFromOtherIp = mock(HttpServletRequest.class);
        when(requestFromOtherIp.getRequestURI()).thenReturn("/api/contact");
        when(requestFromOtherIp.getHeader("X-Forwarded-For")).thenReturn(null);
        when(requestFromOtherIp.getRemoteAddr()).thenReturn("192.168.1.2");

        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);

        assertThatCode(() -> filter.doFilterInternal(requestFromOtherIp, response, filterChain))
                .doesNotThrowAnyException();
    }
}