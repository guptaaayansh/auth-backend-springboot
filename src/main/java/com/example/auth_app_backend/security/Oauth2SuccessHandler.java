package com.example.auth_app_backend.security;

import com.example.auth_app_backend.entities.Provider;
import com.example.auth_app_backend.entities.RefreshToken;
import com.example.auth_app_backend.entities.User;
import com.example.auth_app_backend.repositories.RefreshTokenRepository;
import com.example.auth_app_backend.repositories.UserRepository;
import com.example.auth_app_backend.services.Impl.CookieService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.juli.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class Oauth2SuccessHandler implements AuthenticationSuccessHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final CookieService cookieService;
    private final RefreshTokenRepository refreshTokenRepository;

//    @Value("${app.auth.frontend.success-redirect}")
//    private String frontEndSuccessUrl;

    /**
     * Called when a user has been successfully authenticated.
     *
     * @param request        the request which caused the successful authentication
     * @param response       the response
     * @param authentication the <tt>Authentication</tt> object which was created during
     *                       the authentication process.
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        logger.info(authentication.toString());


        OAuth2User oAuth2User = (OAuth2User)authentication.getPrincipal();

        String registrationId = "unknown";
        if (authentication instanceof OAuth2AuthenticationToken token){
            registrationId= token.getAuthorizedClientRegistrationId();
        }

        User user;
        switch (registrationId) {
            case "google" -> {
                String googleId = oAuth2User.getAttributes().getOrDefault("sub", "").toString();

                String email = oAuth2User.getAttributes().getOrDefault("email", "").toString();
                String name = oAuth2User.getAttributes().getOrDefault("name", "").toString();
                String picture = oAuth2User.getAttributes().getOrDefault("picture", "").toString();
                User newUser = User.builder()
                        .email(email)
                        .name(name)
                        .image(picture)
                        .enable(true)
                        .provider(Provider.GOOGLE)
//                        .providerId(googleId)
                        .build();


                user = userRepository.findByEmail(email).orElseGet(() -> userRepository.save(newUser));

            }

//            case "github" -> {
//                String name = oAuth2User.getAttributes().getOrDefault("login", "").toString();
//                String githubId = oAuth2User.getAttributes().getOrDefault("id", "").toString();
//                String image = oAuth2User.getAttributes().getOrDefault("avatar_url", "").toString();
//
//                String email = (String) oAuth2User.getAttributes().get("email");
//                if (email == null) {
//                    email = name + "@github.com";
//                }
//
//                User newUser = User.builder()
//                        .email(email)
//                        .name(name)
//                        .image(image)
//                        .enable(true)
//                        .provider(Provider.GITHUB)
//                        .providerId(githubId)
//                        .build();
//                user = userRepository.findByEmail(email).orElseGet(() -> userRepository.save(newUser));
//
//            }

            default -> {
                throw new RuntimeException("Invalid registration id");
            }

        }

        String jti = UUID.randomUUID().toString();

        RefreshToken refreshTokenObj = RefreshToken.builder()
                .jti(jti)
                .user(user)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshTokenObj);
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user, refreshTokenObj.getJti());
        cookieService.attachRefreshCookie(response, refreshToken, jwtService.getRefreshTtlSeconds());


        response.getWriter().write("Login Successful");
    }
}
