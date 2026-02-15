package sn.esmt.isi.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.esmt.isi.model.ResearchProject;
import sn.esmt.isi.model.User;
import sn.esmt.isi.repository.ProjectRepository;
import sn.esmt.isi.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    // ──────────────────────────────────────────────────────────────
    // Récupération de l'utilisateur authentifié
    // ──────────────────────────────────────────────────────────────

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        String email = extractEmail(auth.getPrincipal());
        if (email == null || email.isBlank())
            return null;
        return userRepository.findByEmail(email).orElse(null);
    }

    public User getRequiredCurrentUser() {
        User user = getCurrentUser();
        if (user == null)
            throw new AccessDeniedException("Aucun utilisateur authentifié");
        return user;
    }

    private String extractEmail(Object principal) {
        if (principal instanceof OAuth2User oauth)
            return oauth.getAttribute("email");
        if (principal instanceof UserDetails ud)
            return ud.getUsername();
        if (principal instanceof User u)
            return u.getEmail();
        String str = principal.toString();
        return str.contains("@") ? str : null;
    }

    // ──────────────────────────────────────────────────────────────
    // CANDIDAT — projets du propriétaire connecté uniquement
    // ──────────────────────────────────────────────────────────────

    public List<ResearchProject> getMyProjects() {
        User user = getRequiredCurrentUser();
        return projectRepository.findByProprietaireEmail(user.getEmail());
    }

    @Transactional
    public ResearchProject createProject(ResearchProject project) {
        User owner = getRequiredCurrentUser();
        project.setProprietaire(owner);
        applyCreationDefaults(project);
        return projectRepository.save(project);
    }

    /**
     * Mise à jour — utilisée par le CANDIDAT (vérifie la propriété)
     * et par le GESTIONNAIRE (pas de vérification propriété)
     */
    @Transactional
    public ResearchProject updateProject(Long id, ResearchProject updates) {
        User current = getRequiredCurrentUser();
        ResearchProject existing;

        // Le gestionnaire peut modifier n'importe quel projet
        if ("ROLE_GESTIONNAIRE".equals(current.getRole()) || "ROLE_ADMIN".equals(current.getRole())) {
            existing = findById(id);
        } else {
            // Le candidat ne peut modifier que ses propres projets
            existing = getProjectIfOwner(id, current.getEmail());
        }

        existing.setTitreProjet(updates.getTitreProjet());
        existing.setDomaineRecherche(updates.getDomaineRecherche());
        existing.setDescription(updates.getDescription());
        existing.setResponsableProjet(updates.getResponsableProjet());
        existing.setInstitution(updates.getInstitution());
        existing.setBudgetEstime(updates.getBudgetEstime());
        existing.setDateDebut(updates.getDateDebut());
        existing.setDateFin(updates.getDateFin());
        existing.setNiveauAvancement(updates.getNiveauAvancement());
        existing.setStatutProjet(updates.getStatutProjet());
        existing.setListeParticipants(updates.getListeParticipants());
        existing.setAutresParticipants(updates.getAutresParticipants()); // ✅ Fix: Syncing external participants
        existing.setMembers(updates.getMembers()); // ✅ Fix: Syncing ManyToMany relationship
        existing.setDateModification(LocalDateTime.now());

        return projectRepository.save(existing);
    }

    public ResearchProject getProjectIfOwner(Long id, String email) {
        ResearchProject project = projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Projet introuvable : " + id));
        if (project.getProprietaire() == null || !email.equals(project.getProprietaire().getEmail())) {
            throw new AccessDeniedException("Accès interdit : vous n'êtes pas propriétaire");
        }
        return project;
    }

    /**
     * Suppression par CANDIDAT (vérifie la propriété)
     */
    @Transactional
    public void deleteProject(Long id) {
        User current = getRequiredCurrentUser();

        // ✅ CORRECTION : le gestionnaire peut supprimer n'importe quel projet
        if ("ROLE_GESTIONNAIRE".equals(current.getRole()) || "ROLE_ADMIN".equals(current.getRole())) {
            ResearchProject project = findById(id);
            projectRepository.delete(project);
        } else {
            // Candidat : vérifie la propriété avant suppression
            ResearchProject project = getProjectIfOwner(id, current.getEmail());
            projectRepository.delete(project);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // GESTIONNAIRE — accès à tous les projets
    // ──────────────────────────────────────────────────────────────

    /**
     * ✅ Retourne TOUS les projets (tous candidats confondus)
     * Utilisé par le ManagerController pour le tableau de bord
     */
    public List<ResearchProject> getAllProjects() {
        return projectRepository.findAll();
    }

    /**
     * Retourne les projets visibles selon le rôle de l'utilisateur connecté
     */
    public List<ResearchProject> getVisibleProjects() {
        User user = getCurrentUser();
        if (user == null)
            return List.of();

        return switch (user.getRole()) {
            case "ROLE_GESTIONNAIRE", "ROLE_ADMIN" -> projectRepository.findAll(); // ✅ tous les projets
            case "ROLE_CANDIDAT" -> getMyProjects(); // ses projets seulement
            default -> List.of();
        };
    }

    public List<ResearchProject> searchProjects(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllProjects();
        }
        return projectRepository.searchProjects(keyword.trim());
    }

    // ──────────────────────────────────────────────────────────────
    // Méthodes utilitaires communes
    // ──────────────────────────────────────────────────────────────

    public ResearchProject findById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Projet non trouvé : " + id));
    }

    public List<ResearchProject> findByStatut(String status) {
        return projectRepository.findByStatutProjet(status);
    }

    /**
     * @deprecated Utiliser getAllProjects() à la place
     */
    @Deprecated
    public List<ResearchProject> findAll() {
        return getAllProjects();
    }

    // ──────────────────────────────────────────────────────────────
    // Statistiques candidat
    // ──────────────────────────────────────────────────────────────

    public Map<String, Object> getCandidateStats() {
        List<ResearchProject> projects = getMyProjects();

        long total = projects.size();
        long enCours = countByStatus(projects, "EN_COURS");
        long termines = countByStatus(projects, "TERMINE");
        long suspendus = countByStatus(projects, "SUSPENDU");

        long enRetard = projects.stream()
                .filter(p -> p.getDateFin() != null &&
                        LocalDateTime.now().isAfter(p.getDateFin().atStartOfDay().plusDays(1)) &&
                        !"TERMINE".equals(p.getStatutProjet()))
                .count();

        double avancementMoyen = projects.stream()
                .filter(p -> p.getNiveauAvancement() != null)
                .mapToInt(ResearchProject::getNiveauAvancement)
                .average()
                .orElse(0.0);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProjets", total);
        stats.put("enCours", enCours);
        stats.put("termines", termines);
        stats.put("suspendus", suspendus);
        stats.put("enRetard", enRetard);
        stats.put("avancementMoyen", Math.round(avancementMoyen * 10.0) / 10.0);

        return stats;
    }

    private long countByStatus(List<ResearchProject> projects, String status) {
        return projects.stream()
                .filter(p -> status.equals(p.getStatutProjet()))
                .count();
    }

    // ──────────────────────────────────────────────────────────────
    // Utilitaires privés
    // ──────────────────────────────────────────────────────────────

    public void updateFormattedParticipantsList(ResearchProject project) {
        StringBuilder sb = new StringBuilder();

        // 1. Membres internes (avec lien User)
        if (project.getMembers() != null) {
            for (User u : project.getMembers()) {
                sb.append(u.getPrenom()).append(" ").append(u.getNom())
                        .append(" (").append(u.getEmail()).append(")\n");
            }
        }

        // 2. Participants externes (Champ texte libre)
        if (project.getAutresParticipants() != null && !project.getAutresParticipants().isBlank()) {
            if (sb.length() > 0) {
                sb.append("\n--- Externes ---\n");
            }
            sb.append(project.getAutresParticipants());
        }

        project.setListeParticipants(sb.toString().trim());
    }

    private void applyCreationDefaults(ResearchProject p) {
        if (p.getStatutProjet() == null)
            p.setStatutProjet("EN_COURS");
        if (p.getNiveauAvancement() == null)
            p.setNiveauAvancement(0);
        if (p.getDomaineRecherche() == null || p.getDomaineRecherche().isBlank())
            p.setDomaineRecherche("Non spécifié");

        // Initial sync if members exist
        updateFormattedParticipantsList(p);

        p.setDateCreation(LocalDateTime.now());
        p.setDateModification(LocalDateTime.now());
    }
}