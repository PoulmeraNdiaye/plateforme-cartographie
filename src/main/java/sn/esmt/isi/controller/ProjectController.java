package sn.esmt.isi.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.esmt.isi.model.ResearchProject;
import sn.esmt.isi.service.ProjectService;
import sn.esmt.isi.service.StatisticsService;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final StatisticsService statsService;

    public ProjectController(ProjectService projectService, StatisticsService statsService) {
        this.projectService = projectService;
        this.statsService = statsService;
    }

    /**
     * Liste les projets visibles pour l'utilisateur connecté
     * - CANDIDAT : voit seulement ses propres projets
     * - GESTIONNAIRE/ADMIN : voit tout
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ResearchProject>> getVisibleProjects() {
        return ResponseEntity.ok(projectService.getVisibleProjects());
    }

    /**
     * Récupère un projet par ID (vérification ownership pour CANDIDAT)
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResearchProject> getById(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.findById(id));
    }

    /**
     * Crée un nouveau projet (réservé aux CANDIDAT)
     */
    @PostMapping
    @PreAuthorize("hasRole('CANDIDAT')")
    public ResponseEntity<ResearchProject> create(@Valid @RequestBody ResearchProject project) {
        ResearchProject created = projectService.createProject(project);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Met à jour un projet existant (réservé aux CANDIDAT, vérification ownership)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CANDIDAT')")
    public ResponseEntity<Void> update(@PathVariable Long id, @Valid @RequestBody ResearchProject updates) {
        projectService.updateProject(id, updates);
        return ResponseEntity.ok().build();
    }

    /**
     * Supprime un projet (réservé aux CANDIDAT, vérification ownership)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CANDIDAT')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Statistiques globales du dashboard (réservé GESTIONNAIRE et ADMIN)
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE','ADMIN')")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        return ResponseEntity.ok(statsService.getGlobalStats());
    }

    /**
     * Projets par statut (réservé GESTIONNAIRE et ADMIN)
     * → uniquement EN_COURS, SUSPENDU, TERMINE
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE','ADMIN')")
    public ResponseEntity<List<ResearchProject>> getByStatus(@PathVariable String status) {
        // Sécurité supplémentaire : limiter aux 3 statuts valides
        if (!List.of("EN_COURS", "SUSPENDU", "TERMINE").contains(status.toUpperCase())) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(projectService.findByStatut(status));
    }
}