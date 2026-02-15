package sn.esmt.isi.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sn.esmt.isi.model.ResearchProject;
import sn.esmt.isi.model.User;
import sn.esmt.isi.service.ProjectService;
import sn.esmt.isi.service.StatisticsService;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/manager")
@PreAuthorize("hasRole('GESTIONNAIRE')")
public class ManagerController {

    private final ProjectService projectService;
    private final StatisticsService statisticsService;
    private final sn.esmt.isi.repository.DomaineRepository domaineRepository;
    private final sn.esmt.isi.repository.UserRepository userRepository;
    private final sn.esmt.isi.service.PdfExportService pdfExportService;

    public ManagerController(ProjectService projectService,
            StatisticsService statisticsService,
            sn.esmt.isi.repository.DomaineRepository domaineRepository,
            sn.esmt.isi.repository.UserRepository userRepository,
            sn.esmt.isi.service.PdfExportService pdfExportService) {
        this.projectService = projectService;
        this.statisticsService = statisticsService;
        this.domaineRepository = domaineRepository;
        this.userRepository = userRepository;
        this.pdfExportService = pdfExportService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) String keyword, Model model) {
        User user = projectService.getCurrentUser();

        List<ResearchProject> projets;
        if (keyword != null && !keyword.isEmpty()) {
            projets = projectService.searchProjects(keyword);
        } else {
            projets = projectService.getAllProjects();
        }

        Map<String, Object> stats = statisticsService.getGlobalStats();

        model.addAttribute("user", user);
        model.addAttribute("projets", projets);
        model.addAttribute("stats", stats);
        model.addAttribute("keyword", keyword);

        return "manager/dashboard";
    }

    // ==================== GESTION DES PROJETS ====================

    @GetMapping("/projects/new")
    public String newProjectForm(Model model) {
        model.addAttribute("project", new ResearchProject());
        model.addAttribute("domaines",
                domaineRepository.findAll().stream().map(sn.esmt.isi.model.Domaine::getNom).toList());
        return "manager/project-form";
    }

    @GetMapping("/projects/edit/{id}")
    public String editProjectForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            ResearchProject project = projectService.findById(id);
            model.addAttribute("project", project);
            model.addAttribute("domaines",
                    domaineRepository.findAll().stream().map(sn.esmt.isi.model.Domaine::getNom).toList());
            return "manager/project-form";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Projet introuvable");
            return "redirect:/manager/dashboard";
        }
    }

    @PostMapping("/projects/save")
    public String saveProject(@Valid @ModelAttribute("project") ResearchProject project,
            BindingResult result,
            RedirectAttributes ra) {
        if (result.hasErrors()) {
            return "manager/project-form";
        }
        try {
            if (project.getId() == null) {
                projectService.createProject(project);
                ra.addFlashAttribute("success", "Projet créé avec succès !");
            } else {
                // ✅ updateProject gère maintenant le rôle GESTIONNAIRE sans vérif propriété
                projectService.updateProject(project.getId(), project);
                ra.addFlashAttribute("success", "Projet modifié avec succès !");
            }
            return "redirect:/manager/dashboard";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur : " + e.getMessage());
            return "redirect:/manager/dashboard";
        }
    }

    @GetMapping("/projects/view/{id}")
    public String viewProject(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            ResearchProject project = projectService.findById(id);
            model.addAttribute("project", project);
            model.addAttribute("user", projectService.getCurrentUser());
            return "manager/project-detail";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/manager/dashboard";
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
        return "redirect:/manager/dashboard";
    }

    // ==================== GESTION PARTICIPANTS ====================

    @GetMapping("/projects/participants/{id}")
    public String manageParticipants(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            ResearchProject project = projectService.findById(id);
            List<User> candidates = userRepository.findByRole("ROLE_CANDIDAT");

            model.addAttribute("project", project);
            model.addAttribute("candidates", candidates);
            model.addAttribute("user", projectService.getCurrentUser());
            return "manager/project-participants";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Projet introuvable");
            return "redirect:/manager/dashboard";
        }
    }

    @PostMapping("/projects/participants/{id}")
    public String saveParticipants(@PathVariable Long id,
            @RequestParam(required = false) List<String> selectedUserIds,
            RedirectAttributes ra) {
        try {
            ResearchProject project = projectService.findById(id);

            // Récupérer les utilisateurs sélectionnés
            // Récupérer les membres existants
            java.util.Set<User> currentMembers = project.getMembers();

            if (selectedUserIds != null && !selectedUserIds.isEmpty()) {
                currentMembers.addAll(userRepository.findAllById(selectedUserIds));
            }

            // Mettre à jour la liste (ajout seulement)
            project.setMembers(currentMembers);

            project.setMembers(currentMembers);

            // Mise à jour de la liste formatée (Internes + Externes)
            projectService.updateFormattedParticipantsList(project);

            projectService.updateProject(id, project);

            ra.addFlashAttribute("success", "Participants mis à jour avec succès");
            return "redirect:/manager/projects/view/" + id;
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur lors de la mise à jour : " + e.getMessage());
            return "redirect:/manager/dashboard";
        }
    }

    @GetMapping("/projects/participants-external/{id}")
    public String manageExternalParticipants(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            ResearchProject project = projectService.findById(id);
            model.addAttribute("project", project);
            model.addAttribute("user", projectService.getCurrentUser());
            return "manager/project-participants-external";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Projet introuvable");
            return "redirect:/manager/dashboard";
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
            return "redirect:/manager/projects/view/" + id;
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur : " + e.getMessage());
            return "redirect:/manager/dashboard";
        }
    }

    // ===================== STATISTIQUES & GRAPHIQUES =====================

    @GetMapping("/statistics")
    public String statistics(Model model) {
        User user = projectService.getCurrentUser();
        // Utiliser les statistiques globales (tous les projets) pour cohérence avec le
        // dashboard
        Map<String, Object> stats = statisticsService.getGlobalStats();
        Map<String, Object> advancedStats = statisticsService.getAdvancedStats();

        model.addAttribute("user", user);
        model.addAttribute("stats", stats);
        model.addAttribute("advancedStats", advancedStats);
        return "manager/statistics";
    }

    @GetMapping("/charts")
    public String charts(Model model) {
        User user = projectService.getCurrentUser();
        // Utiliser les statistiques avancées pour avoir toutes les données des
        // graphiques
        Map<String, Object> stats = statisticsService.getAdvancedStats();

        model.addAttribute("user", user);
        model.addAttribute("stats", stats);
        return "manager/charts";
    }

    // Export PDF endpoints
    @GetMapping("/charts/export-pdf")
    public ResponseEntity<byte[]> exportChartsPdf() {
        try {
            Map<String, Object> stats = statisticsService.getAdvancedStats();
            byte[] pdfBytes = pdfExportService.generateStatisticsPdf(stats, "Graphiques de Gestion");

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
            byte[] pdfBytes = pdfExportService.generateStatisticsPdf(stats, "Statistiques de Gestion");

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