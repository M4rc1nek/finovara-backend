package com.finovara.authservice.security.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finovara.contracts.exception.ErrorResponseDto;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


@Component
@RequiredArgsConstructor
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper;
    private final OAuth2AuthorizationRequestCookieStore authorizationRequestRepository;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        authorizationRequestRepository.removeAuthorizationRequest(request, response);


        String accept = request.getHeader("Accept");
        boolean wantsJson = accept != null && accept.contains("application/json");

        if (!wantsJson) {
            String errorCode = "oauth2_expired";
            if (exception.getMessage() != null && exception.getMessage().contains("authorization_request_not_found")) {
                errorCode = "oauth2_expired";
            }
            response.sendRedirect("https://localhost:5173/auth?error=" + URLEncoder.encode(errorCode, StandardCharsets.UTF_8));
            return;
        }

        HttpStatus status = HttpStatus.UNAUTHORIZED;
        ErrorResponseDto body = new ErrorResponseDto(
                status.value(),
                status.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI()
        );

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
        response.flushBuffer();
    }
}

