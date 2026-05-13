package com.finovara.finovarabackend.security.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finovara.finovarabackend.exception.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginFailureHandlerTest {

    @InjectMocks
    private OAuth2LoginFailureHandler oAuth2LoginFailureHandler;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private OAuth2AuthorizationRequestCookieStore authorizationRequestRepository;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private HttpServletResponse httpServletResponse;

    @Mock
    private AuthenticationException authenticationException;

    @Nested
    class JsonResponse {

        @Test
        void shouldReturnUnauthorizedJsonResponse() throws Exception {
            when(httpServletRequest.getRequestURI()).thenReturn("/test");
            when(httpServletRequest.getHeader("Accept")).thenReturn("application/json");
            when(authenticationException.getMessage()).thenReturn("error");

            StringWriter writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);

            when(httpServletResponse.getWriter()).thenReturn(printWriter);

            oAuth2LoginFailureHandler.onAuthenticationFailure(httpServletRequest, httpServletResponse, authenticationException);

            verify(httpServletResponse).setStatus(HttpStatus.UNAUTHORIZED.value());
            verify(httpServletResponse).setContentType("application/json");
            verify(httpServletResponse).setCharacterEncoding("UTF-8");

            ArgumentCaptor<ErrorResponseDto> captor =
                    ArgumentCaptor.forClass(ErrorResponseDto.class);

            verify(objectMapper).writeValue(eq(printWriter), captor.capture());

            ErrorResponseDto dto = captor.getValue();

            assertEquals(HttpStatus.UNAUTHORIZED.value(), dto.status());
            assertEquals("Unauthorized", dto.error());
            assertEquals("error", dto.message());
            assertEquals("/test", dto.path());
        }

        @Test
        void shouldHandleNullExceptionMessage() throws Exception {
            when(httpServletRequest.getHeader("Accept")).thenReturn("application/json");
            when(authenticationException.getMessage()).thenReturn(null);

            StringWriter writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);

            when(httpServletResponse.getWriter()).thenReturn(printWriter);

            oAuth2LoginFailureHandler.onAuthenticationFailure(httpServletRequest, httpServletResponse, authenticationException);

            verify(objectMapper).writeValue(eq(printWriter), any(ErrorResponseDto.class));
        }

        @Test
        void shouldStillReturnJsonWhenAcceptContainsJson() throws Exception {
            when(httpServletRequest.getHeader("Accept")).thenReturn("text/html,application/json;q=0.9");
            when(authenticationException.getMessage()).thenReturn("error");

            StringWriter writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);

            when(httpServletResponse.getWriter()).thenReturn(printWriter);

            oAuth2LoginFailureHandler.onAuthenticationFailure(httpServletRequest, httpServletResponse, authenticationException);

            verify(objectMapper).writeValue(eq(printWriter), any(ErrorResponseDto.class));
        }
    }

    @Nested
    class RedirectResponse {

        @Test
        void shouldRedirectWhenAcceptHeaderMissing() throws Exception {
            when(httpServletRequest.getHeader("Accept")).thenReturn(null);
            when(authenticationException.getMessage()).thenReturn("error");

            oAuth2LoginFailureHandler.onAuthenticationFailure(httpServletRequest, httpServletResponse, authenticationException);

            verify(httpServletResponse).sendRedirect(contains("auth?error="));
        }

        @Test
        void shouldRedirectWhenAcceptIsHtml() throws Exception {
            when(httpServletRequest.getHeader("Accept")).thenReturn("text/html");
            when(authenticationException.getMessage()).thenReturn("error");

            oAuth2LoginFailureHandler.onAuthenticationFailure(httpServletRequest, httpServletResponse, authenticationException);

            verify(httpServletResponse).sendRedirect(contains("auth?error="));
        }

        @Test
        void shouldUseDefaultErrorCodeWhenAuthorizationRequestNotFound() throws Exception {
            when(httpServletRequest.getHeader("Accept")).thenReturn("text/html");
            when(authenticationException.getMessage()).thenReturn("authorization_request_not_found");

            oAuth2LoginFailureHandler.onAuthenticationFailure(httpServletRequest, httpServletResponse, authenticationException);

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(httpServletResponse).sendRedirect(captor.capture());

            assertTrue(captor.getValue().contains("oauth2_expired"));
        }

        @Test
        void shouldRemoveAuthorizationRequestOnFailure() throws Exception {
            when(httpServletRequest.getHeader("Accept")).thenReturn("text/html");
            when(authenticationException.getMessage()).thenReturn("error");

            oAuth2LoginFailureHandler.onAuthenticationFailure(httpServletRequest, httpServletResponse, authenticationException);

            verify(authorizationRequestRepository)
                    .removeAuthorizationRequest(httpServletRequest, httpServletResponse);
        }
    }
}