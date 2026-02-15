package sn.esmt.isi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration Spring MVC pour masquer les extensions .html et .jsp dans les
 * URLs
 * Permet d'avoir des URLs propres comme /admin/dashboard au lieu de
 * /admin/dashboard.html
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * Configure la négociation de contenu pour ignorer les extensions de fichiers
     */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
                // Ne pas utiliser l'extension du fichier pour déterminer le type de contenu
                .favorPathExtension(false)
                // Ne pas utiliser le paramètre "format" dans l'URL
                .favorParameter(false);
    }

    /**
     * Configure le matching des chemins pour ne pas utiliser les suffixes
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer
                // Ne pas utiliser les suffixes pour le pattern matching
                .setUseSuffixPatternMatch(false)
                // Ne pas utiliser les suffixes enregistrés uniquement
                .setUseRegisteredSuffixPatternMatch(false);
    }

    /**
     * Optionnel : Ajouter des redirections pour les anciennes URLs avec extensions
     * Cela permet une transition en douceur si des liens avec .html existent déjà
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Exemple : rediriger /login.html vers /login
        // registry.addRedirectViewController("/login.html", "/login");
        // Vous pouvez ajouter d'autres redirections si nécessaire
    }
}
