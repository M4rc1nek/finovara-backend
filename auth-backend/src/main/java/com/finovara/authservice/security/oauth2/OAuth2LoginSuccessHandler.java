package com.finovara.authservice.security.oauth2;

import com.finovara.authservice.user.model.User;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final GoogleOAuth2UserService googleOAuth2UserService;
    private final OAuth2AuthorizationRequestCookieStore authorizationRequestRepository;
    private final OAuth2PendingLoginCookie pendingLoginCookie;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        try {
            OAuth2User oauth2User;

            if (authentication.getPrincipal() instanceof DefaultOidcUser) {
                oauth2User = (DefaultOidcUser) authentication.getPrincipal();
            } else if (authentication.getPrincipal() instanceof OAuth2User) {
                oauth2User = (OAuth2User) authentication.getPrincipal();
            } else {
                throw new IllegalStateException("Unsupported principal type");
            }

            User user = googleOAuth2UserService.synchronize(oauth2User);
            pendingLoginCookie.add(response, user.getId(), UUID.randomUUID().toString(), request.isSecure());

            authorizationRequestRepository.removeAuthorizationRequest(request, response);
            SecurityContextHolder.clearContext();

            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }

            response.sendRedirect("https://localhost:5173/oauth2/verify");

        } catch (EntityAlreadyExistsException | InvalidInputException exception) {
            log.error("OAuth2 business validation failed", exception);
            response.sendRedirect("https://localhost:5173/auth?error=" + exception.getMessage());

        } catch (RuntimeException exception) {
            log.error("OAuth2 authentication failed", exception);
            response.sendRedirect("https://localhost:5173/auth?error=oauth2_authentication_failed");
        }
    }
}