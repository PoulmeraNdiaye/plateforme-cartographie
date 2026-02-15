package sn.esmt.isi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import sn.esmt.isi.model.ResearchProject;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<ResearchProject, Long> {

        // ──────────────────────────────────────────────────────────────
        // Recherche par propriétaire (clé pour dashboard candidat)
        // ──────────────────────────────────────────────────────────────
        List<ResearchProject> findByProprietaireEmail(String email);

        // ──────────────────────────────────────────────────────────────
        // Filtres par statut (seulement les 3 statuts autorisés)
        // ──────────────────────────────────────────────────────────────
        List<ResearchProject> findByStatutProjet(String statutProjet);

        long countByStatutProjet(String statutProjet);

        long countByDomaineRecherche(String domaineRecherche);

        // ──────────────────────────────────────────────────────────────
        // Statistiques pour dashboard candidat
        // ──────────────────────────────────────────────────────────────

        /**
         * Nombre de projets en retard (date fin passée + statut != TERMINE)
         */
        @Query("SELECT COUNT(p) FROM ResearchProject p " +
                        "WHERE p.dateFin < CURRENT_DATE AND p.statutProjet != 'TERMINE'")
        long countOverdueProjects();

        /**
         * Avancement moyen global
         */
        @Query("SELECT AVG(p.niveauAvancement) FROM ResearchProject p WHERE p.niveauAvancement IS NOT NULL")
        Double findAverageNiveauAvancement();

        // ──────────────────────────────────────────────────────────────
        // Méthodes supplémentaires pour StatisticsService
        // ──────────────────────────────────────────────────────────────

        /**
         * Somme totale des budgets estimés
         */
        @Query("SELECT SUM(p.budgetEstime) FROM ResearchProject p WHERE p.budgetEstime IS NOT NULL")
        Double sumBudgetEstime();

        /**
         * Répartition des projets par domaine de recherche
         */
        @Query("SELECT p.domaineRecherche, COUNT(p) FROM ResearchProject p WHERE p.domaineRecherche IS NOT NULL GROUP BY p.domaineRecherche ORDER BY COUNT(p) DESC")
        List<Object[]> countProjectsByDomaine();

        /**
         * Compter les projets créés par mois (pour graphique d'évolution)
         * Retourne [mois, année, count] pour les 12 derniers mois
         */
        @Query(value = "SELECT MONTH(date_creation) as mois, " +
                        "YEAR(date_creation) as annee, " +
                        "COUNT(*) as total " +
                        "FROM research_projects " +
                        "WHERE date_creation >= DATE_SUB(NOW(), INTERVAL 12 MONTH) " +
                        "GROUP BY YEAR(date_creation), MONTH(date_creation) " +
                        "ORDER BY annee ASC, mois ASC", nativeQuery = true)
        List<Object[]> countProjectsByMonth();

        /**
         * Nombre de projets par participant (propriétaire)
         * Retourne [nom complet, email, nombre de projets]
         */
        @Query("SELECT CONCAT(u.prenom, ' ', u.nom), u.email, COUNT(p) " +
                        "FROM ResearchProject p JOIN p.proprietaire u " +
                        "GROUP BY u.id, u.prenom, u.nom, u.email " +
                        "ORDER BY COUNT(p) DESC")
        List<Object[]> countProjectsByParticipant();

        /**
         * Budget total par domaine de recherche
         * Retourne [domaine, somme des budgets]
         */
        @Query("SELECT p.domaineRecherche, SUM(p.budgetEstime) " +
                        "FROM ResearchProject p " +
                        "WHERE p.domaineRecherche IS NOT NULL AND p.budgetEstime IS NOT NULL " +
                        "GROUP BY p.domaineRecherche " +
                        "ORDER BY SUM(p.budgetEstime) DESC")
        List<Object[]> sumBudgetByDomaine();

        // ──────────────────────────────────────────────────────────────
        // Méthodes supplémentaires utiles (gardées)
        // ──────────────────────────────────────────────────────────────

        List<ResearchProject> findByInstitution(String institution);

        List<ResearchProject> findByResponsableProjetContainingIgnoreCase(String responsable);

        List<ResearchProject> findByNiveauAvancementGreaterThanEqual(Integer seuil);

        List<ResearchProject> findByDateCreationAfter(LocalDateTime date);

        long countByDateCreationAfter(LocalDateTime date);

        @Query("SELECT p FROM ResearchProject p WHERE " +
                        "LOWER(p.titreProjet) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(p.domaineRecherche) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(p.institution) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(p.responsableProjet) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(p.listeParticipants) LIKE LOWER(CONCAT('%', :keyword, '%'))")
        List<ResearchProject> searchProjects(String keyword);
}