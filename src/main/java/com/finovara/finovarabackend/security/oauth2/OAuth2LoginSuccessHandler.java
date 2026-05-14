package com.finovara.finovarabackend.security.oauth2;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.exception.conflict.NameAlreadyExistsException;
import com.finovara.finovarabackend.security.jwt.JwtService;
import com.finovara.finovarabackend.user.exception.conflict.EmailAlreadyExistsException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.profile.ProfileImageUrlBuilder;
import jakarta.servlet.http.Cookie;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final String ACCESS_TOKEN_COOKIE_NAME = "oauth2_access_token";
    private static final int ACCESS_TOKEN_COOKIE_MAX_AGE_SECONDS = 24 * 60 * 60;

    private final GoogleOAuth2UserService googleOAuth2UserService;
    private final JwtService jwtService;
    private final OAuth2AuthorizationRequestCookieStore authorizationRequestRepository;

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
            String token = jwtService.generateToken(user);


            String profileImageUrl = ProfileImageUrlBuilder.buildProfileImageUrl(user.getProfileImagePath());

            addAccessTokenCookie(response, token, request.isSecure());

            String redirectUrl = UriComponentsBuilder
                    .fromUriString("https://localhost:5173/oauth2/success")
                    .queryParam("id", user.getId())
                    .queryParam("username", user.getUsername())
                    .queryParam("email", user.getEmail())
                    .queryParam("profileImageUrl", profileImageUrl != null ? profileImageUrl : "")
                    .queryParam("passwordSet", user.isPasswordSet())
                    .encode()
                    .build()
                    .toUriString();

            authorizationRequestRepository.removeAuthorizationRequest(request, response);
            SecurityContextHolder.clearContext();

            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }

            response.sendRedirect(redirectUrl);

        } catch (EmailAlreadyExistsException | NameAlreadyExistsException | InvalidInputException exception) {
            log.error("OAuth2 business validation failed", exception);
            response.sendRedirect("https://localhost:5173/auth?error=" + exception.getMessage());

        } catch (RuntimeException exception) {
            log.error("OAuth2 authentication failed", exception);
            response.sendRedirect("https://localhost:5173/auth?error=oauth2_authentication_failed");
        }
    }

    private void addAccessTokenCookie(HttpServletResponse response, String token, boolean secure) {
        Cookie cookie = new Cookie(ACCESS_TOKEN_COOKIE_NAME, token);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setMaxAge(ACCESS_TOKEN_COOKIE_MAX_AGE_SECONDS);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

}
