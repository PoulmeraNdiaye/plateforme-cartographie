package sn.esmt.isi.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom handler for OAuth2 authentication failures
 */
@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationFailureHandler.class);

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException {

        logger.error("OAuth2 authentication failed: {}", exception.getMessage());

        // Redirect to login page with error parameter
        response.sendRedirect("/login?oauth_error=true");
    }
}
