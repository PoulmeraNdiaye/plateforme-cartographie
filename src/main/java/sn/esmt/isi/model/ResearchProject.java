package sn.esmt.isi.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "research_projects")
@Getter
@Setter
@NoArgsConstructor
public class ResearchProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_id")
    private Long id;

    @Column(name = "titre_projet", nullable = false, length = 255)
    private String titreProjet;

    @Column(name = "description", columnDefinition = "VARCHAR(2000)")
    private String description;

    @Column(name = "liste_participants", columnDefinition = "TEXT")
    private String listeParticipants;

    @Column(name = "autres_participants", columnDefinition = "TEXT")
    private String autresParticipants;

    @Column(name = "domaine_recherche", length = 255)
    private String domaineRecherche;

    @Column(name = "statut_projet", length = 255)
    private String statutProjet = "EN_COURS"; // valeur par défaut modifiée

    @Column(name = "niveau_avancement")
    private Integer niveauAvancement = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proprietaire_id", nullable = false)
    private User proprietaire;

    @Column(name = "responsable_projet", length = 255)
    private String responsableProjet;

    @Column(name = "institution", length = 255)
    private String institution;

    @Column(name = "budget_estime")
    private Double budgetEstime;

    @Column(name = "date_debut")
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @CreationTimestamp
    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "project_members", joinColumns = @JoinColumn(name = "project_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    private java.util.Set<User> members = new java.util.HashSet<>();

    // ──────────────────────────────────────────────────────────────
    // Méthodes utilitaires – UNIQUEMENT les 3 statuts demandés
    // ──────────────────────────────────────────────────────────────

    /**
     * Libellé lisible du statut (uniquement les 3 cas)
     */
    public String getStatutLisible() {
        if (statutProjet == null)
            return "Inconnu";

        return switch (statutProjet.toUpperCase()) {
            case "EN_COURS" -> "En cours";
            case "SUSPENDU" -> "Suspendu";
            case "TERMINE" -> "Terminé";
            default -> "Inconnu";
        };
    }

    /**
     * Le projet est modifiable seulement s'il n'est pas Terminé
     */
    public boolean isModifiable() {
        return !"TERMINE".equalsIgnoreCase(statutProjet);
    }

    /**
     * En retard = date de fin passée ET pas encore Terminé
     */
    public boolean isEnRetard() {
        if (dateFin == null)
            return false;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fin = dateFin.atStartOfDay().plusDays(1); // fin de journée

        return now.isAfter(fin) && !"TERMINE".equalsIgnoreCase(statutProjet);
    }

    /**
     * Résumé court de la description
     */
    public String getResumeCourt() {
        if (description == null || description.isBlank()) {
            return "Aucune description";
        }
        if (description.length() <= 120) {
            return description;
        }
        return description.substring(0, 117) + "...";
    }

    /**
     * Couleur Bootstrap pour le badge du statut
     */
    public String getStatutCouleur() {
        if (statutProjet == null)
            return "secondary";

        return switch (statutProjet.toUpperCase()) {
            case "EN_COURS" -> "primary";
            case "SUSPENDU" -> "warning text-dark";
            case "TERMINE" -> "success";
            default -> "secondary";
        };
    }
}