package sn.esmt.isi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import sn.esmt.isi.service.CustomOAuth2UserService;
import sn.esmt.isi.service.CustomOAuth2User;

import java.util.Collection;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationFailureHandler oauth2FailureHandler;

    public SecurityConfig(@Lazy CustomOAuth2UserService customOAuth2UserService,
            OAuth2AuthenticationFailureHandler oauth2FailureHandler) {
        this.customOAuth2UserService = customOAuth2UserService;
        this.oauth2FailureHandler = oauth2FailureHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // 1. Accès public total
                        .requestMatchers("/", "/login", "/register", "/error").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/static/**").permitAll()

                        // 2. Routes OAuth2 profile completion (AVANT les restrictions de rôle)
                        .requestMatchers("/candidate/complete-profile", "/candidate/profile/update").permitAll()

                        // 3. Restrictions par rôles (L'ordre est important)
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/manager/**").hasRole("GESTIONNAIRE")
                        .requestMatchers("/candidate/**").hasRole("CANDIDAT")

                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(authenticationSuccessHandler())
                        .failureUrl("/login?error=true")
                        .permitAll())
                .oauth2Login(oauth -> oauth
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(authenticationSuccessHandler())
                        .failureHandler(oauth2FailureHandler))
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll());

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            String targetUrl = "/candidate/dashboard";

            // Check if this is an OAuth2 authentication
            if (authentication.getPrincipal() instanceof CustomOAuth2User) {
                CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();
                sn.esmt.isi.model.User user = oauth2User.getUser();

                // Only CANDIDAT role needs profile completion
                if ("ROLE_CANDIDAT".equals(user.getRole())) {
                    // If profile is not completed, redirect to profile completion page
                    if (user.getProfileCompleted() == null || !user.getProfileCompleted()) {
                        targetUrl = "/candidate/complete-profile";
                    } else {
                        targetUrl = getRoleBasedUrl(user.getRole());
                    }
                } else {
                    // Admin and Manager go directly to their dashboard
                    targetUrl = getRoleBasedUrl(user.getRole());
                }
            } else {
                // Form-based authentication
                Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
                for (GrantedAuthority authority : authorities) {
                    String role = authority.getAuthority();
                    targetUrl = getRoleBasedUrl(role);
                    break;
                }
            }

            response.sendRedirect(targetUrl);
        };
    }

    private String getRoleBasedUrl(String role) {
        return switch (role) {
            case "ROLE_ADMIN" -> "/admin/dashboard";
            case "ROLE_GESTIONNAIRE" -> "/manager/dashboard";
            case "ROLE_CANDIDAT" -> "/candidate/dashboard";
            default -> "/login?error=true";
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}