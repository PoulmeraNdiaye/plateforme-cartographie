package sn.esmt.isi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sn.esmt.isi.repository.ProjectRepository;
import sn.esmt.isi.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class StatisticsService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Statistiques globales pour les pages statistics et charts
     * Retourne toutes les données nécessaires aux graphiques et tableaux
     */
    public Map<String, Object> getGlobalStats() {
        Map<String, Object> stats = new HashMap<>();

        // ═══════════════════════════════════════════════════════════════
        // 1. STATISTIQUES DES PROJETS
        // ═══════════════════════════════════════════════════════════════

        long totalProjets = projectRepository.count();
        long projetsEnCours = projectRepository.countByStatutProjet("EN_COURS");
        long projetsSuspendus = projectRepository.countByStatutProjet("SUSPENDU");
        long projetsTermines = projectRepository.countByStatutProjet("TERMINE");

        stats.put("totalProjets", totalProjets);
        stats.put("projetsEnCours", projetsEnCours);
        stats.put("projetsSuspendus", projetsSuspendus);
        stats.put("projetsTermines", projetsTermines);

        // Avancement moyen
        Double moyenneAvancement = projectRepository.findAverageNiveauAvancement();
        stats.put("avancementMoyen", moyenneAvancement != null ? Math.round(moyenneAvancement) : 0);

        // Budget total
        Double budgetTotal = projectRepository.sumBudgetEstime();
        stats.put("budgetTotal", budgetTotal != null ? budgetTotal : 0.0);

        // ═══════════════════════════════════════════════════════════════
        // 2. RÉPARTITION PAR DOMAINE DE RECHERCHE (pour graphiques et tableau)
        // ═══════════════════════════════════════════════════════════════

        List<Object[]> domaineStats = projectRepository.countProjectsByDomaine();
        Map<String, Long> repartitionDomaines = new LinkedHashMap<>();

        if (domaineStats != null && !domaineStats.isEmpty()) {
            for (Object[] row : domaineStats) {
                if (row[0] != null && row[1] != null) {
                    String domaine = (String) row[0];
                    Long count = ((Number) row[1]).longValue();
                    repartitionDomaines.put(domaine, count);
                }
            }
        } else {
            // Valeurs par défaut si aucune donnée
            repartitionDomaines.put("Intelligence Artificielle", 0L);
            repartitionDomaines.put("Sécurité Informatique", 0L);
            repartitionDomaines.put("Réseaux et Télécommunications", 0L);
        }

        stats.put("repartitionDomaines", repartitionDomaines);

        // ═══════════════════════════════════════════════════════════════
        // 3. STATISTIQUES DES UTILISATEURS (par rôle)
        // ═══════════════════════════════════════════════════════════════

        long totalUtilisateurs = userRepository.count();
        long candidats = userRepository.countByRole("ROLE_CANDIDAT");
        long gestionnaires = userRepository.countByRole("ROLE_GESTIONNAIRE");
        long admins = userRepository.countByRole("ROLE_ADMIN");

        stats.put("totalUtilisateurs", totalUtilisateurs);
        stats.put("candidats", candidats);
        stats.put("gestionnaires", gestionnaires);
        stats.put("admins", admins);

        // ═══════════════════════════════════════════════════════════════
        // 4. NOMBRE DE PROJETS PAR PARTICIPANT
        // ═══════════════════════════════════════════════════════════════

        List<Object[]> participantStats = projectRepository.countProjectsByParticipant();
        Map<String, Object> projectsByParticipant = new LinkedHashMap<>();

        if (participantStats != null && !participantStats.isEmpty()) {
            for (Object[] row : participantStats) {
                if (row[0] != null && row[1] != null && row[2] != null) {
                    String nomComplet = (String) row[0];
                    String email = (String) row[1];
                    Long count = ((Number) row[2]).longValue();

                    Map<String, Object> participantInfo = new HashMap<>();
                    participantInfo.put("nom", nomComplet);
                    participantInfo.put("email", email);
                    participantInfo.put("count", count);

                    projectsByParticipant.put(email, participantInfo);
                }
            }
        }

        stats.put("projectsByParticipant", projectsByParticipant);

        // ═══════════════════════════════════════════════════════════════
        // 5. BUDGET TOTAL PAR DOMAINE
        // ═══════════════════════════════════════════════════════════════

        List<Object[]> budgetStats = projectRepository.sumBudgetByDomaine();
        Map<String, Double> budgetByDomaine = new LinkedHashMap<>();

        if (budgetStats != null && !budgetStats.isEmpty()) {
            for (Object[] row : budgetStats) {
                if (row[0] != null && row[1] != null) {
                    String domaine = (String) row[0];
                    Double budget = ((Number) row[1]).doubleValue();
                    budgetByDomaine.put(domaine, budget);
                }
            }
        } else {
            // Valeurs par défaut si aucune donnée
            budgetByDomaine.put("Intelligence Artificielle", 0.0);
            budgetByDomaine.put("Sécurité Informatique", 0.0);
            budgetByDomaine.put("Réseaux et Télécommunications", 0.0);
        }

        stats.put("budgetByDomaine", budgetByDomaine);

        // ═══════════════════════════════════════════════════════════════
        // 6. DONNÉES FORMATÉES POUR LES GRAPHIQUES
        // ═══════════════════════════════════════════════════════════════

        // Données pour graphique des domaines (pie chart)
        List<String> domainesLabels = new ArrayList<>(repartitionDomaines.keySet());
        List<Long> domainesData = new ArrayList<>(repartitionDomaines.values());
        stats.put("domainesLabels", domainesLabels);
        stats.put("domainesData", domainesData);

        // Données pour graphique des statuts (doughnut chart)
        List<Long> statutsData = Arrays.asList(projetsEnCours, projetsTermines, projetsSuspendus);
        stats.put("statutsData", statutsData);

        // Données pour graphique des participants (bar chart)
        List<String> participantsLabels = new ArrayList<>();
        List<Long> participantsData = new ArrayList<>();
        projectsByParticipant.forEach((email, info) -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> participantMap = (Map<String, Object>) info;
            participantsLabels.add((String) participantMap.get("nom"));
            participantsData.add((Long) participantMap.get("count"));
        });
        stats.put("participantsLabels", participantsLabels);
        stats.put("participantsData", participantsData);

        // Données pour graphique du budget par domaine (bar chart)
        List<String> budgetDomainesLabels = new ArrayList<>(budgetByDomaine.keySet());
        List<Double> budgetDomainesData = new ArrayList<>(budgetByDomaine.values());
        stats.put("budgetDomainesLabels", budgetDomainesLabels);
        stats.put("budgetDomainesData", budgetDomainesData);

        return stats;
    }

    /**
     * Statistiques avancées pour la page Rapports
     */
    public Map<String, Object> getAdvancedStats() {
        Map<String, Object> stats = getGlobalStats();

        // Projets en retard
        long projetsEnRetard = projectRepository.countOverdueProjects();
        stats.put("projetsEnRetard", projetsEnRetard);

        // Calculer l'évolution mensuelle des projets (12 derniers mois)
        // Noms des mois en français
        String[] moisNoms = { "Jan", "Fév", "Mar", "Avr", "Mai", "Juin",
                "Juil", "Aoû", "Sep", "Oct", "Nov", "Déc" };

        List<String> moisLabels = new ArrayList<>();
        List<Long> projectsGrowthData = new ArrayList<>();

        try {
            List<Object[]> monthlyData = projectRepository.countProjectsByMonth();

            // Créer une map pour les données mensuelles
            Map<String, Long> monthlyMap = new HashMap<>();
            for (Object[] row : monthlyData) {
                Integer mois = ((Number) row[0]).intValue();
                Integer annee = ((Number) row[1]).intValue();
                Long count = ((Number) row[2]).longValue();
                String key = annee + "-" + String.format("%02d", mois);
                monthlyMap.put(key, count);
            }

            // Remplir les 12 derniers mois (même si certains mois n'ont pas de projets)
            LocalDateTime current = LocalDateTime.now();
            for (int i = 11; i >= 0; i--) {
                LocalDateTime month = current.minusMonths(i);
                String key = month.getYear() + "-" + String.format("%02d", month.getMonthValue());
                String label = moisNoms[month.getMonthValue() - 1] + " " + month.getYear();

                moisLabels.add(label);
                projectsGrowthData.add(monthlyMap.getOrDefault(key, 0L));
            }
        } catch (Exception e) {
            // En cas d'erreur, créer des données par défaut (12 mois avec 0 projets)
            System.err.println("Erreur lors du calcul de l'évolution mensuelle: " + e.getMessage());
            LocalDateTime current = LocalDateTime.now();
            for (int i = 11; i >= 0; i--) {
                LocalDateTime month = current.minusMonths(i);
                String label = moisNoms[month.getMonthValue() - 1] + " " + month.getYear();
                moisLabels.add(label);
                projectsGrowthData.add(0L);
            }
        }

        stats.put("moisLabels", moisLabels);
        stats.put("projectsGrowthData", projectsGrowthData);

        // Statistiques par institution (si disponible)
        List<Object[]> usersByInstitution = userRepository.countUsersByInstitution();
        Map<String, Long> repartitionInstitutions = new LinkedHashMap<>();

        if (usersByInstitution != null) {
            for (Object[] row : usersByInstitution) {
                if (row[0] != null && row[1] != null) {
                    String institution = (String) row[0];
                    Long count = ((Number) row[1]).longValue();
                    repartitionInstitutions.put(institution, count);
                }
            }
        }

        stats.put("repartitionInstitutions", repartitionInstitutions);

        return stats;
    }

    /**
     * Statistiques spécifiques pour un gestionnaire
     * 
     * @param gestionnaireEmail Email du gestionnaire
     */
    public Map<String, Object> getManagerStats(String gestionnaireEmail) {
        Map<String, Object> stats = new HashMap<>();

        // Projets du gestionnaire
        List<sn.esmt.isi.model.ResearchProject> projetsGestionnaire = projectRepository
                .findByProprietaireEmail(gestionnaireEmail);

        // ═══════════════════════════════════════════════════════════════
        // 1. STATISTIQUES DE BASE
        // ═══════════════════════════════════════════════════════════════

        long totalProjets = projetsGestionnaire.size();
        long projetsEnCours = projetsGestionnaire.stream()
                .filter(p -> "EN_COURS".equals(p.getStatutProjet()))
                .count();
        long projetsTermines = projetsGestionnaire.stream()
                .filter(p -> "TERMINE".equals(p.getStatutProjet()))
                .count();
        long projetsSuspendus = projetsGestionnaire.stream()
                .filter(p -> "SUSPENDU".equals(p.getStatutProjet()))
                .count();

        stats.put("totalProjets", totalProjets);
        stats.put("projetsEnCours", projetsEnCours);
        stats.put("projetsTermines", projetsTermines);
        stats.put("projetsSuspendus", projetsSuspendus);

        // Avancement moyen des projets du gestionnaire
        double avancementMoyen = projetsGestionnaire.stream()
                .filter(p -> p.getNiveauAvancement() != null)
                .mapToInt(sn.esmt.isi.model.ResearchProject::getNiveauAvancement)
                .average()
                .orElse(0.0);
        stats.put("avancementMoyen", Math.round(avancementMoyen));

        // Budget total des projets du gestionnaire
        double budgetTotal = projetsGestionnaire.stream()
                .filter(p -> p.getBudgetEstime() != null)
                .mapToDouble(sn.esmt.isi.model.ResearchProject::getBudgetEstime)
                .sum();
        stats.put("budgetTotal", budgetTotal);

        // ═══════════════════════════════════════════════════════════════
        // 2. RÉPARTITION PAR DOMAINE
        // ═══════════════════════════════════════════════════════════════

        Map<String, Long> repartitionDomaines = new LinkedHashMap<>();
        projetsGestionnaire.stream()
                .filter(p -> p.getDomaineRecherche() != null)
                .forEach(p -> {
                    String domaine = p.getDomaineRecherche();
                    repartitionDomaines.put(domaine,
                            repartitionDomaines.getOrDefault(domaine, 0L) + 1);
                });
        stats.put("repartitionDomaines", repartitionDomaines);

        // ═══════════════════════════════════════════════════════════════
        // 3. PROJETS PAR PARTICIPANT (pour les projets du gestionnaire)
        // ═══════════════════════════════════════════════════════════════

        Map<String, Object> projectsByParticipant = new LinkedHashMap<>();

        // Grouper les projets par propriétaire
        Map<String, List<sn.esmt.isi.model.ResearchProject>> projetsByOwner = new LinkedHashMap<>();
        projetsGestionnaire.forEach(p -> {
            if (p.getProprietaire() != null) {
                String email = p.getProprietaire().getEmail();
                projetsByOwner.computeIfAbsent(email, k -> new ArrayList<>()).add(p);
            }
        });

        // Créer les statistiques par participant
        projetsByOwner.forEach((email, projets) -> {
            if (!projets.isEmpty()) {
                sn.esmt.isi.model.User user = projets.get(0).getProprietaire();
                Map<String, Object> participantInfo = new HashMap<>();
                participantInfo.put("nom", user.getPrenom() + " " + user.getNom());
                participantInfo.put("email", email);
                participantInfo.put("count", (long) projets.size());
                projectsByParticipant.put(email, participantInfo);
            }
        });

        stats.put("projectsByParticipant", projectsByParticipant);

        // ═══════════════════════════════════════════════════════════════
        // 4. BUDGET PAR DOMAINE (pour les projets du gestionnaire)
        // ═══════════════════════════════════════════════════════════════

        Map<String, Double> budgetByDomaine = new LinkedHashMap<>();
        projetsGestionnaire.stream()
                .filter(p -> p.getDomaineRecherche() != null && p.getBudgetEstime() != null)
                .forEach(p -> {
                    String domaine = p.getDomaineRecherche();
                    budgetByDomaine.put(domaine,
                            budgetByDomaine.getOrDefault(domaine, 0.0) + p.getBudgetEstime());
                });

        // Valeurs par défaut si aucune donnée
        if (budgetByDomaine.isEmpty()) {
            budgetByDomaine.put("Intelligence Artificielle", 0.0);
            budgetByDomaine.put("Sécurité Informatique", 0.0);
            budgetByDomaine.put("Réseaux et Télécommunications", 0.0);
        }

        stats.put("budgetByDomaine", budgetByDomaine);

        return stats;
    }
}