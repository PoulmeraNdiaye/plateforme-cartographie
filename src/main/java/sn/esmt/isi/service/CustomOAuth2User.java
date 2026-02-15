package sn.esmt.isi.service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import sn.esmt.isi.model.User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Wrapper class that combines OAuth2User with our User entity
 * This allows us to access both OAuth2 attributes and our custom User data
 */
public class CustomOAuth2User implements OAuth2User {

    private final OAuth2User oauth2User;
    private final User user;

    public CustomOAuth2User(OAuth2User oauth2User, User user) {
        this.oauth2User = oauth2User;
        this.user = user;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oauth2User.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Return authorities based on our User's role
        return Collections.singletonList(new SimpleGrantedAuthority(user.getRole()));
    }

    @Override
    public String getName() {
        return oauth2User.getName();
    }

    /**
     * Get the User entity associated with this OAuth2 user
     */
    public User getUser() {
        return user;
    }

    /**
     * Get the email from OAuth2 attributes
     */
    public String getEmail() {
        return oauth2User.getAttribute("email");
    }
}
