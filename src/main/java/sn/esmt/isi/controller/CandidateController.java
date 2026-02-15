package sn.esmt.isi.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sn.esmt.isi.model.ResearchProject;
import sn.esmt.isi.model.User;
import sn.esmt.isi.repository.UserRepository;
import sn.esmt.isi.service.ProjectService;
import sn.esmt.isi.service.StatisticsService;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/candidate")
@PreAuthorize("hasRole('CANDIDAT')")
public class CandidateController {

    private final ProjectService projectService;
    private final UserRepository userRepository;
    private final StatisticsService statisticsService;
    private final sn.esmt.isi.repository.DomaineRepository domaineRepository;

    public CandidateController(ProjectService projectService,
            UserRepository userRepository,
            StatisticsService statisticsService,
            sn.esmt.isi.repository.DomaineRepository domaineRepository) {
        this.projectService = projectService;
        this.userRepository = userRepository;
        this.statisticsService = statisticsService;
        this.domaineRepository = domaineRepository;
    }

    // ✅ UNE SEULE méthode dashboard (suppression du doublon)
    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (user == null) {
            return "redirect:/login";
        }

        if (!isProfileComplete(user)) {
            return "redirect:/candidate/profile/complete";
        }

        List<ResearchProject> mesProjets = projectService.getMyProjects();

        // ✅ stats ajouté pour le template
        Map<String, Object> stats = statisticsService.getGlobalStats();

        model.addAttribute("user", user);
        model.addAttribute("mesProjets", mesProjets);
        model.addAttribute("projectCount", mesProjets.size());
        model.addAttribute("stats", stats);

        return "candidate/dashboard";
    }

    @GetMapping("/projects/new")
    public String newProjectForm(Model model, Authentication auth) {
        User user = getCurrentUser(auth);
        if (user == null) {
            return "redirect:/login";
        }
        ResearchProject project = new ResearchProject();
        model.addAttribute("project", project);
        model.addAttribute("domaines",
                domaineRepository.findAll().stream().map(sn.esmt.isi.model.Domaine::getNom).toList());
        return "candidate/project-form";
    }

    @GetMapping("/projects/edit/{id}")
    public String editProjectForm(@PathVariable Long id, Model model,
            Authentication auth, RedirectAttributes ra) {
        User user = getCurrentUser(auth);
        if (user == null) {
            return "redirect:/login";
        }

        try {
            ResearchProject project = projectService.getProjectIfOwner(id, user.getEmail());
            if (!project.isModifiable()) {
                ra.addFlashAttribute("error", "Ce projet ne peut plus être modifié.");
                return "redirect:/candidate/dashboard";
            }
            model.addAttribute("project", project);
            model.addAttribute("domaines",
                    domaineRepository.findAll().stream().map(sn.esmt.isi.model.Domaine::getNom).toList());
            return "candidate/project-form";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur : " + e.getMessage());
            return "redirect:/candidate/dashboard";
        }
    }

    @PostMapping("/projects/save")
    public String saveProject(@Valid @ModelAttribute("project") ResearchProject project,
            BindingResult result,
            Authentication auth,
            RedirectAttributes ra,
            Model model) {
        User user = getCurrentUser(auth);
        if (user == null) {
            return "redirect:/login";
        }

        project.setProprietaire(user);

        if (result.hasErrors()) {
            model.addAttribute("project", project);
            // Recharger la liste des domaines en cas d'erreur
            model.addAttribute("domaines",
                    domaineRepository.findAll().stream().map(sn.esmt.isi.model.Domaine::getNom).toList());
            model.addAttribute("org.springframework.validation.BindingResult.project", result);
            return "candidate/project-form";
        }

        try {
            if (project.getId() == null) {
                projectService.createProject(project);
                ra.addFlashAttribute("success", "Projet créé avec succès !");
            } else {
                projectService.updateProject(project.getId(), project);
                ra.addFlashAttribute("success", "Projet modifié avec succès !");
            }
            return "redirect:/candidate/dashboard";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur lors de l'enregistrement : " + e.getMessage());
            ra.addFlashAttribute("error", "Erreur lors de l'enregistrement : " + e.getMessage());
            model.addAttribute("project", project);
            // Recharger la liste des domaines en cas d'erreur
            model.addAttribute("domaines",
                    domaineRepository.findAll().stream().map(sn.esmt.isi.model.Domaine::getNom).toList());
            return "candidate/project-form";
        }
    }

    @GetMapping("/projects/view/{id}")
    public String viewProject(@PathVariable Long id, Model model,
            Authentication auth, RedirectAttributes ra) {
        User user = getCurrentUser(auth);
        if (user == null) {
            return "redirect:/login";
        }

        try {
            ResearchProject project = projectService.getProjectIfOwner(id, user.getEmail());
            model.addAttribute("project", project);
            model.addAttribute("user", user);
            return "candidate/project-detail";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/candidate/dashboard";
        }
    }

    @GetMapping("/profile/complete")
    public String completeProfileForm(Model model, Authentication auth) {
        User user = getCurrentUser(auth);
        if (user == null)
            return "redirect:/login";
        model.addAttribute("user", user);
        return "candidate/complete-profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@Valid @ModelAttribute("user") User updatedUser,
            BindingResult result,
            Authentication auth,
            RedirectAttributes ra,
            Model model) {

        User current = getCurrentUser(auth);
        if (current == null) {
            ra.addFlashAttribute("error", "Session expirée. Veuillez vous reconnecter.");
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            model.addAttribute("user", updatedUser);
            model.addAttribute("org.springframework.validation.BindingResult.user", result);
            ra.addFlashAttribute("error", "Veuillez corriger les erreurs dans le formulaire");
            return "candidate/complete-profile";
        }

        try {
            current.setPrenom(updatedUser.getPrenom());
            current.setNom(updatedUser.getNom());
            current.setTelephone(updatedUser.getTelephone());
            current.setInstitution(updatedUser.getInstitution());
            current.setDepartement(updatedUser.getDepartement());
            current.setSpecialite(updatedUser.getSpecialite());
            current.setNiveauEtude(updatedUser.getNiveauEtude());
            current.setBio(updatedUser.getBio());

            userRepository.save(current);

            ra.addFlashAttribute("success",
                    "Profil complété avec succès ! Vous pouvez maintenant déclarer vos projets.");
            return "redirect:/candidate/dashboard";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur lors de la sauvegarde : " + e.getMessage());
            model.addAttribute("user", updatedUser);
            return "candidate/complete-profile";
        }
    }

    // ===== UTILITAIRES =====
    private User getCurrentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated())
            return null;
        return projectService.getCurrentUser();
    }

    private boolean isProfileComplete(User user) {
        return user != null &&
                user.getTelephone() != null && !user.getTelephone().isBlank() &&
                user.getInstitution() != null && !user.getInstitution().isBlank();
    }
}