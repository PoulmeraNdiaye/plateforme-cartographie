package sn.esmt.isi.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import sn.esmt.isi.model.User;
import sn.esmt.isi.repository.UserRepository;
import sn.esmt.isi.service.CustomOAuth2User;

/**
 * Controller for completing user profile after OAuth2 registration
 */
@Controller
public class CompleteProfileController {

    private final UserRepository userRepository;

    public CompleteProfileController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/candidate/complete-profile")
    public String showCompleteProfileForm(Model model, Authentication authentication) {
        User user = null;

        // Check if this is OAuth2 authentication
        if (authentication != null && authentication.getPrincipal() instanceof CustomOAuth2User) {
            CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();
            user = oauth2User.getUser();
        }
        // If not OAuth2, user must be authenticated with form login
        else if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        } else {
            return "redirect:/login";
        }

        model.addAttribute("user", user);
        return "candidate/complete-profile";
    }

    @PostMapping({ "/candidate/complete-profile", "/candidate/profile/update" })
    public String completeProfile(@RequestParam(required = false) String institution,
            @RequestParam(required = false) String departement,
            @RequestParam(required = false) String specialite,
            @RequestParam(required = false) String niveauEtude,
            @RequestParam(required = false) String telephone,
            @RequestParam(required = false) String bio,
            Authentication authentication) {

        if (authentication == null || !(authentication.getPrincipal() instanceof CustomOAuth2User)) {
            return "redirect:/login";
        }

        CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();
        User user = oauth2User.getUser();

        // Update user profile
        user.setInstitution(institution);
        user.setDepartement(departement);
        user.setSpecialite(specialite);
        user.setNiveauEtude(niveauEtude);
        user.setTelephone(telephone);
        user.setBio(bio);
        user.setProfileCompleted(true);

        userRepository.save(user);

        // Redirect based on role
        return switch (user.getRole()) {
            case "ROLE_ADMIN" -> "redirect:/admin/dashboard";
            case "ROLE_GESTIONNAIRE" -> "redirect:/manager/dashboard";
            default -> "redirect:/candidate/dashboard";
        };
    }
}
