package sn.esmt.isi.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sn.esmt.isi.model.User;
import sn.esmt.isi.repository.UserRepository;
import sn.esmt.isi.service.ProjectService;
import sn.esmt.isi.service.StatisticsService;

import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import sn.esmt.isi.model.ResearchProject;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ProjectService projectService;
    private final StatisticsService statisticsService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final sn.esmt.isi.repository.DomaineRepository domaineRepository;
    private final sn.esmt.isi.repository.ProjectRepository projectRepository;
    private final sn.esmt.isi.repository.AppConfigRepository appConfigRepository;
    private final sn.esmt.isi.service.PdfExportService pdfExportService;

    public AdminController(ProjectService projectService,
            StatisticsService statisticsService,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            sn.esmt.isi.repository.DomaineRepository domaineRepository,
            sn.esmt.isi.repository.ProjectRepository projectRepository,
            sn.esmt.isi.repository.AppConfigRepository appConfigRepository,
            sn.esmt.isi.service.PdfExportService pdfExportService) {
        this.projectService = projectService;
        this.statisticsService = statisticsService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.domaineRepository = domaineRepository;
        this.projectRepository = projectRepository;
        this.appConfigRepository = appConfigRepository;
        this.pdfExportService = pdfExportService;
    }

    // ======================== DASHBOARD ========================

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) String keyword, Model model) {
        User user = projectService.getCurrentUser();
        Map<String, Object> stats = statisticsService.getGlobalStats();

        List<sn.esmt.isi.model.ResearchProject> projets;
        if (keyword != null && !keyword.isEmpty()) {
            projets = projectService.searchProjects(keyword);
        } else {
            projets = projectService.getAllProjects();
        }

        model.addAttribute("user", user);
        model.addAttribute("stats", stats);
        model.addAttribute("projets", projets);
        model.addAttribute("keyword", keyword);

        return "admin/dashboard";
    }

    // ===================== GESTION UTILISATEURS =====================

    @GetMapping("/users")
    public String listUsers(@RequestParam(required = false) String keyword, Model model) {
        User currentUser = projectService.getCurrentUser();
        List<User> users;

        if (keyword != null && !keyword.isEmpty()) {
            users = userRepository.findBySearchTerm(keyword);
            model.addAttribute("keyword", keyword);
        } else {
            users = userRepository.findAll();
        }

        model.addAttribute("user", currentUser);
        model.addAttribute("users", users);
        return "admin/users";
    }

    @GetMapping("/users/edit/{id}")
    public String editUserForm(
            @PathVariable("id") String idStr,
            Model model,
            RedirectAttributes ra) {

        String id = validateId(idStr, ra, "/admin/users");
        if (id == null)
            return "redirect:/admin/users";

        return userRepository.findById(id)
                .map(u -> {
                    model.addAttribute("user", projectService.getCurrentUser());
                    model.addAttribute("userForm", u);
                    return "admin/users-edit";
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("error", "Utilisateur introuvable");
                    return "redirect:/admin/users";
                });
    }

    @PostMapping("/users/edit/{id}")
    public String updateUser(
            @PathVariable("id") String idStr,
            @ModelAttribute("userForm") User userForm,
            RedirectAttributes ra) {

        String id = validateId(idStr, ra, "/admin/users");
        if (id == null)
            return "redirect:/admin/users";

        return userRepository.findById(id)
                .map(existing -> {
                    existing.setNom(userForm.getNom());
                    existing.setPrenom(userForm.getPrenom());
                    existing.setTelephone(userForm.getTelephone());
                    existing.setInstitution(userForm.getInstitution());
                    existing.setRole(userForm.getRole());
                    existing.setActive(userForm.getActive());
                    userRepository.save(existing);
                    ra.addFlashAttribute("success", "Utilisateur modifié avec succès");
                    return "redirect:/admin/users";
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("error", "Utilisateur introuvable");
                    return "redirect:/admin/users";
                });
    }

    @PostMapping("/users/create")
    public String createUser(
            @RequestParam String prenom,
            @RequestParam String nom,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String role,
            RedirectAttributes ra) {

        if (userRepository.findByEmail(email).isPresent()) {
            ra.addFlashAttribute("error", "Un utilisateur avec cet email existe déjà");
            return "redirect:/admin/users";
        }

        User newUser = new User();
        newUser.setId(UUID.randomUUID().toString());
        newUser.setPrenom(prenom);
        newUser.setNom(nom);
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setRole("ROLE_" + role); // ← important : ajout du préfixe ROLE_
        newUser.setActive(true);
        userRepository.save(newUser);

        ra.addFlashAttribute("success", "Utilisateur créé : " + prenom + " " + nom);
        return "redirect:/admin/users";
    }

    @PostMapping("/users/change-role/{id}")
    public String changeRole(
            @PathVariable("id") String idStr,
            @RequestParam String role,
            RedirectAttributes ra) {

        String id = validateId(idStr, ra, "/admin/users");
        if (id == null)
            return "redirect:/admin/users";

        return userRepository.findById(id)
                .map(user -> {
                    user.setRole("ROLE_" + role); // ← important : ajout du préfixe ROLE_
                    userRepository.save(user);
                    ra.addFlashAttribute("success", "Rôle modifié : " + role);
                    return "redirect:/admin/users";
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("error", "Utilisateur introuvable");
                    return "redirect:/admin/users";
                });
    }

    @PostMapping("/users/toggle-active/{id}")
    public String toggleActive(
            @PathVariable("id") String idStr,
            RedirectAttributes ra) {

        String id = validateId(idStr, ra, "/admin/users");
        if (id == null)
            return "redirect:/admin/users";

        return userRepository.findById(id)
                .map(user -> {
                    user.setActive(!user.getActive());
                    userRepository.save(user);
                    ra.addFlashAttribute("success",
                            user.getActive() ? "Compte activé" : "Compte désactivé");
                    return "redirect:/admin/users";
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("error", "Utilisateur introuvable");
                    return "redirect:/admin/users";
                });
    }

    @PostMapping("/users/delete/{id}")
    public String deleteUser(
            @PathVariable("id") String idStr,
            RedirectAttributes ra) {

        String id = validateId(idStr, ra, "/admin/users");
        if (id == null)
            return "redirect:/admin/users";

        try {
            userRepository.deleteById(id);
            ra.addFlashAttribute("success", "Utilisateur supprimé");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Impossible de supprimer : " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // Méthode utilitaire simplifiée (plus besoin de Long)
    private String validateId(String idStr, RedirectAttributes ra, String redirectUrl) {
        if (idStr == null || idStr.trim().isEmpty()) {
            ra.addFlashAttribute("error", "ID manquant");
            return null;
        }
        return idStr.trim();
    }

    // ===================== GESTION DOMAINES =====================

    @GetMapping("/domaines")
    public String listDomaines(Model model) {
        User user = projectService.getCurrentUser();
        List<sn.esmt.isi.model.Domaine> domaines = domaineRepository.findAll();

        // Calculer le nombre de projets pour chaque domaine
        for (sn.esmt.isi.model.Domaine d : domaines) {
            d.setNbProjets(projectRepository.countByDomaineRecherche(d.getNom()));
        }

        model.addAttribute("user", user);
        model.addAttribute("domaines", domaines);
        return "admin/domaines";
    }

    @PostMapping("/domaines/save")
    public String saveDomaine(@RequestParam String nom, @RequestParam(required = false) String description,
            RedirectAttributes ra) {
        try {
            sn.esmt.isi.model.Domaine domaine = new sn.esmt.isi.model.Domaine();
            domaine.setNom(nom);
            domaine.setDescription(description);
            domaineRepository.save(domaine);
            ra.addFlashAttribute("success", "Domaine créé avec succès");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur lors de la création (le nom doit être unique)");
        }
        return "redirect:/admin/domaines";
    }

    @PostMapping("/domaines/update/{id}")
    public String updateDomaine(@PathVariable Long id, @RequestParam String nom,
            @RequestParam(required = false) String description, RedirectAttributes ra) {
        return domaineRepository.findById(id).map(d -> {
            d.setNom(nom);
            d.setDescription(description);
            try {
                domaineRepository.save(d);
                ra.addFlashAttribute("success", "Domaine modifié avec succès");
            } catch (Exception e) {
                ra.addFlashAttribute("error", "Erreur lors de la modification");
            }
            return "redirect:/admin/domaines";
        }).orElse("redirect:/admin/domaines");
    }

    @PostMapping("/domaines/delete/{id}")
    public String deleteDomaine(@PathVariable Long id, RedirectAttributes ra) {
        try {
            domaineRepository.deleteById(id);
            ra.addFlashAttribute("success", "Domaine supprimé");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Impossible de supprimer ce domaine");
        }
        return "redirect:/admin/domaines";
    }

    // ===================== GESTION PROJETS (ADMIN) =====================

    @GetMapping("/projects/new")
    public String newProjectForm(Model model) {
        User user = projectService.getCurrentUser();
        model.addAttribute("user", user);
        model.addAttribute("project", new ResearchProject());
        model.addAttribute("domaines",
                domaineRepository.findAll().stream().map(sn.esmt.isi.model.Domaine::getNom).toList());
        return "admin/project-form";
    }

    @GetMapping("/projects/edit/{id}")
    public String editProjectForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        User user = projectService.getCurrentUser();
        try {
            ResearchProject project = projectService.findById(id);
            model.addAttribute("user", user);
            model.addAttribute("project", project);
            model.addAttribute("domaines",
                    domaineRepository.findAll().stream().map(sn.esmt.isi.model.Domaine::getNom).toList());
            return "admin/project-form";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Projet introuvable");
            return "redirect:/admin/dashboard";
        }
    }

    @PostMapping("/projects/save")
    public String saveProject(@Valid @ModelAttribute("project") ResearchProject project,
            BindingResult result,
            RedirectAttributes ra, Model model) {

        // Si erreur de validation, on renvoie vers le formulaire avec les données
        if (result.hasErrors()) {
            User user = projectService.getCurrentUser();
            model.addAttribute("user", user);
            model.addAttribute("domaines",
                    domaineRepository.findAll().stream().map(sn.esmt.isi.model.Domaine::getNom).toList());
            return "admin/project-form";
        }

        try {
            if (project.getId() == null) {
                // Création : on associe le projet à l'admin courant ou on laisse null s'il est
                // "super-admin"
                // Ici on utilise projectService qui gère la logique
                User currentUser = projectService.getCurrentUser();
                project.setProprietaire(currentUser);
                projectService.createProject(project);
                ra.addFlashAttribute("success", "Projet créé avec succès !");
            } else {
                // Modification (Admin a tous les droits)
                projectService.updateProject(project.getId(), project);
                ra.addFlashAttribute("success", "Projet modifié avec succès !");
            }
            return "redirect:/admin/dashboard";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur : " + e.getMessage());
            return "redirect:/admin/dashboard";
        }
    }

    @GetMapping("/projects/view/{id}")
    public String viewProject(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            ResearchProject project = projectService.findById(id);
            model.addAttribute("project", project);
            model.addAttribute("user", projectService.getCurrentUser());
            return "admin/project-detail";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/dashboard";
        }
    }

    @GetMapping("/projects/export-pdf/{id}")
    public org.springframework.http.ResponseEntity<byte[]> exportProjectPdf(@PathVariable Long id,
            RedirectAttributes ra) {
        try {
            ResearchProject project = projectService.findById(id);
            byte[] pdfBytes = pdfExportService.exportProjectToPdf(id);

            String filename = "projet_"
                    + (project.getTitreProjet() != null ? project.getTitreProjet().replaceAll("[^a-zA-Z0-9]", "_")
                            : "sans_titre")
                    + ".pdf";

            return org.springframework.http.ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur lors de l'export PDF : " + e.getMessage());
            return org.springframework.http.ResponseEntity
                    .status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/projects/delete/{id}")
    public String deleteProject(@PathVariable Long id, RedirectAttributes ra) {
        try {
            projectService.deleteProject(id);
            ra.addFlashAttribute("success", "Projet supprimé avec succès");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Impossible de supprimer : " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    // ==================== GESTION PARTICIPANTS ====================

    @GetMapping("/projects/participants/{id}")
    public String manageParticipants(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            ResearchProject project = projectService.findById(id);
            // Admin peut voir tous les candidats
            List<User> candidates = userRepository.findByRole("ROLE_CANDIDAT");

            model.addAttribute("project", project);
            model.addAttribute("candidates", candidates);
            model.addAttribute("user", projectService.getCurrentUser());
            return "admin/project-participants";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Projet introuvable");
            return "redirect:/admin/dashboard";
        }
    }

    @PostMapping("/projects/participants/{id}")
    public String saveParticipants(@PathVariable Long id,
            @RequestParam(required = false) List<String> selectedUserIds,
            RedirectAttributes ra) {
        try {
            ResearchProject project = projectService.findById(id);

            java.util.Set<User> currentMembers = project.getMembers();
            if (selectedUserIds != null && !selectedUserIds.isEmpty()) {
                currentMembers.addAll(userRepository.findAllById(selectedUserIds));
            }

            project.setMembers(currentMembers);

            project.setMembers(currentMembers);

            // Mise à jour de la liste formatée (Internes + Externes)
            projectService.updateFormattedParticipantsList(project);

            // On ne sauvegarde pas ici car updateFormattedParticipantsList ne fait que
            // setter le champ
            // Mais updateProject va sauvegarder le tout
            projectService.updateProject(id, project);

            ra.addFlashAttribute("success", "Participants mis à jour avec succès");
            return "redirect:/admin/projects/view/" + id;
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur : " + e.getMessage());
            return "redirect:/admin/dashboard";
        }
    }

    @GetMapping("/projects/participants-external/{id}")
    public String manageExternalParticipants(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            ResearchProject project = projectService.findById(id);
            model.addAttribute("project", project);
            model.addAttribute("user", projectService.getCurrentUser());
            return "admin/project-participants-external";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Projet introuvable");
            return "redirect:/admin/dashboard";
        }
    }

    @PostMapping("/projects/participants-external/{id}")
    public String saveExternalParticipants(@PathVariable Long id,
            @RequestParam String autresParticipants,
            RedirectAttributes ra) {
        try {
            ResearchProject project = projectService.findById(id);
            project.setAutresParticipants(autresParticipants);

            // Mise à jour de la liste formatée (Internes + Externes)
            projectService.updateFormattedParticipantsList(project);

            projectService.updateProject(id, project);

            ra.addFlashAttribute("success", "Participants externes mis à jour");
            return "redirect:/admin/projects/view/" + id;
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur : " + e.getMessage());
            return "redirect:/admin/dashboard";
        }
    }

    // ===================== STATISTIQUES & GRAPHIQUES =====================

    @GetMapping("/statistics")
    public String statistics(Model model) {
        User user = projectService.getCurrentUser();
        Map<String, Object> stats = statisticsService.getGlobalStats();
        Map<String, Object> advancedStats = statisticsService.getAdvancedStats();

        model.addAttribute("user", user);
        model.addAttribute("stats", stats);
        model.addAttribute("advancedStats", advancedStats);
        return "admin/statistics";
    }

    @GetMapping("/charts")
    public String charts(Model model) {
        User user = projectService.getCurrentUser();
        Map<String, Object> stats = statisticsService.getGlobalStats();
        Map<String, Object> advancedStats = statisticsService.getAdvancedStats();

        model.addAttribute("user", user);
        model.addAttribute("stats", stats);
        model.addAttribute("advancedStats", advancedStats);
        return "admin/charts";
    }

    // ===================== RAPPORTS & PARAMÈTRES =====================

    @GetMapping("/reports")
    public String reports(Model model) {
        User user = projectService.getCurrentUser();
        Map<String, Object> stats = statisticsService.getGlobalStats();
        Map<String, Object> advancedStats = statisticsService.getAdvancedStats();

        model.addAttribute("user", user);
        model.addAttribute("stats", stats);
        model.addAttribute("advancedStats", advancedStats);
        return "admin/reports";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        User user = projectService.getCurrentUser();
        sn.esmt.isi.model.AppConfig config = appConfigRepository.findById(1L)
                .orElse(new sn.esmt.isi.model.AppConfig(1L, "Plateforme Cartographie", "admin@esmt.sn", false, true,
                        "1.0.0"));

        model.addAttribute("user", user);
        model.addAttribute("config", config);
        return "admin/settings";
    }

    @PostMapping("/settings/update")
    public String updateSettings(@ModelAttribute sn.esmt.isi.model.AppConfig config, RedirectAttributes ra) {
        config.setId(1L); // Toujours le même ID
        appConfigRepository.save(config);
        ra.addFlashAttribute("success", "Configuration mise à jour");
        return "redirect:/admin/settings";
    }

    // Export PDF endpoints
    @GetMapping("/charts/export-pdf")
    public ResponseEntity<byte[]> exportChartsPdf() {
        try {
            Map<String, Object> stats = statisticsService.getAdvancedStats();
            byte[] pdfBytes = pdfExportService.generateStatisticsPdf(stats, "Graphiques Interactifs");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "graphiques.pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/statistics/export-pdf")
    public ResponseEntity<byte[]> exportStatisticsPdf() {
        try {
            Map<String, Object> stats = statisticsService.getAdvancedStats();
            byte[] pdfBytes = pdfExportService.generateStatisticsPdf(stats, "Statistiques Détaillées");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "statistiques.pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}