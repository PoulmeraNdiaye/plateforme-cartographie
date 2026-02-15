package sn.esmt.isi.service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import sn.esmt.isi.model.User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class CustomUserDetails implements UserDetails, OAuth2User {

    private final User user;
    private final Map<String, Object> attributes;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.user = user;
        this.attributes = null;
        // Créer les autorités correctement avec le préfixe ROLE_
        this.authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole())
        );
    }

    public CustomUserDetails(User user, Map<String, Object> attributes) {
        this.user = user;
        this.attributes = attributes;
        // Créer les autorités correctement avec le préfixe ROLE_
        this.authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole())
        );
    }

    @Override
    public String getName() {
        if (user.getNom() != null && user.getPrenom() != null) {
            return user.getPrenom() + " " + user.getNom();
        }
        return user.getNom() != null ? user.getNom() : user.getEmail();
    }

    public String getNom() {
        return user.getNom();
    }

    public String getPrenom() {
        return user.getPrenom();
    }

    public String getEmail() {
        return user.getEmail();
    }

    public User getUser() {
        return user;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes != null ? attributes : Collections.emptyMap();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}