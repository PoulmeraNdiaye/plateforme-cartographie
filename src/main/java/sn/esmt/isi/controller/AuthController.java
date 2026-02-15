package sn.esmt.isi.controller;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sn.esmt.isi.model.User;
import sn.esmt.isi.repository.UserRepository;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.UUID;

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("user") User user,
            BindingResult bindingResult,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirect,
            Model model) {

        if (bindingResult.hasErrors())
            return "register";

        if (!user.getPassword().equals(confirmPassword)) {
            model.addAttribute("error", "Les mots de passe ne correspondent pas.");
            return "register";
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            model.addAttribute("error", "Email déjà utilisé.");
            return "register";
        }

        try {
            user.setId(UUID.randomUUID().toString());
            user.setActive(true);
            user.setCreatedAt(LocalDateTime.now());
            user.setPassword(passwordEncoder.encode(user.getPassword()));

            // CRUCIAL : On force le format ROLE_NOMDUROLE
            String rawRole = user.getRole().toUpperCase();
            user.setRole("ROLE_" + rawRole);

            userRepository.save(user);

            // All users redirect to login after registration
            redirect.addFlashAttribute("success", "Compte créé avec succès ! Connectez-vous.");
            return "redirect:/login";

        } catch (Exception e) {
            model.addAttribute("error", "Erreur technique : " + e.getMessage());
            return "register";
        }
    }
}