package sn.esmt.isi.service;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import sn.esmt.isi.model.User;
import sn.esmt.isi.repository.UserRepository;

import java.util.UUID;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        String givenName = oAuth2User.getAttribute("given_name");
        String familyName = oAuth2User.getAttribute("family_name");
        String picture = oAuth2User.getAttribute("picture");
        String provider = userRequest.getClientRegistration().getRegistrationId().toUpperCase();

        // Find or create user
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setId(UUID.randomUUID().toString());
            newUser.setEmail(email);
            newUser.setPrenom(givenName);
            newUser.setNom(familyName);
            newUser.setPicture(picture);
            newUser.setOauthId(oAuth2User.getName());
            newUser.setProvider(provider);
            newUser.setRole("ROLE_CANDIDAT"); // Default role for new OAuth2 users
            newUser.setActive(true);
            // For new CANDIDAT users, profile needs completion (no institution/telephone
            // yet)
            newUser.setProfileCompleted(false);
            return userRepository.save(newUser);
        });

        // Link Google account to existing user if they log in with Google for the first
        // time
        if (user.getOauthId() == null) {
            user.setOauthId(oAuth2User.getName());
            user.setProvider(provider);
            if (user.getPicture() == null) {
                user.setPicture(picture);
            }

            // Set profileCompleted based on user role for existing users linking Google
            // account
            if ("ROLE_ADMIN".equals(user.getRole()) || "ROLE_GESTIONNAIRE".equals(user.getRole())) {
                // Admin and Manager don't need profile completion
                user.setProfileCompleted(true);
            } else if ("ROLE_CANDIDAT".equals(user.getRole())) {
                // Candidat needs to complete profile if missing institution/telephone
                boolean hasRequiredInfo = user.getInstitution() != null && user.getTelephone() != null;
                user.setProfileCompleted(hasRequiredInfo);
            }

            userRepository.save(user);
        }

        // For users who already have OAuth linked, check if profile completion status
        // needs update
        // This handles edge cases where profileCompleted might be null
        if (user.getProfileCompleted() == null) {
            if ("ROLE_CANDIDAT".equals(user.getRole())) {
                boolean hasRequiredInfo = user.getInstitution() != null && user.getTelephone() != null;
                user.setProfileCompleted(hasRequiredInfo);
            } else {
                user.setProfileCompleted(true);
            }
            userRepository.save(user);
        }

        // Return CustomOAuth2User that combines OAuth2User and our User entity
        return new CustomOAuth2User(oAuth2User, user);
    }
}