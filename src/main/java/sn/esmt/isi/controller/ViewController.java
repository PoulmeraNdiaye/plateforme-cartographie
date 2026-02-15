package sn.esmt.isi.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import sn.esmt.isi.model.User;
import sn.esmt.isi.service.ProjectService;

@Controller
public class ViewController {

    private final ProjectService projectService;

    public ViewController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping({"/", "/dashboard", ""})
    public String homeOrDashboard(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        User user = projectService.getCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }

        // Profil incomplet → on force la complétion
        if (!isProfileComplete(user)) {
            model.addAttribute("user", user);
            return "candidate/complete-profile";
        }

        // Redirection selon rôle
        return switch (user.getRole()) {
            case "ADMIN"       -> "redirect:/admin/dashboard";
            case "GESTIONNAIRE" -> "redirect:/gestion/dashboard";
            case "CANDIDAT"    -> "redirect:/candidate/dashboard";
            default            -> "redirect:/login";
        };
    }

    private boolean isProfileComplete(User user) {
        return user != null &&
                user.getTelephone() != null && !user.getTelephone().isBlank() &&
                user.getInstitution() != null && !user.getInstitution().isBlank();
    }
}