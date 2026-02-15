package sn.esmt.isi.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import sn.esmt.isi.model.User;
import sn.esmt.isi.repository.UserRepository;

import java.util.Collections;
import java.util.List;

@Component
public class JwtRoleConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Autowired
    private UserRepository userRepository;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String email = jwt.getClaimAsString("email"); // L'email est plus fiable que le sub
        
        User user = userRepository.findByEmail(email) // Utiliser findByEmail
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setId(jwt.getSubject()); // Définit l'ID issu du token
                    newUser.setEmail(email);
                    newUser.setNom(jwt.getClaimAsString("name"));
                    newUser.setRole("CANDIDAT"); // Rôle par défaut
                    return userRepository.save(newUser);
                });

        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole())
        );

        return new UsernamePasswordAuthenticationToken(user, jwt, authorities);
    }
}