package sn.esmt.isi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@lombok.Getter
@lombok.Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@lombok.EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {

    @Id
    @lombok.EqualsAndHashCode.Include
    private String id;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;
    private String nom;
    private String prenom;
    private String telephone;
    private String role;
    private String institution;
    private String departement;
    private String specialite;
    private String niveauEtude;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String oauthId;
    private String provider;
    private String picture;

    @Column(name = "profile_completed")
    private Boolean profileCompleted = false;

    // Ajoutez ces champs pour les statistiques
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column
    private Boolean active = true;

    @ManyToMany(mappedBy = "members")
    @ToString.Exclude
    private java.util.Set<ResearchProject> projects = new java.util.HashSet<>();

    // Méthode pour obtenir le nom complet
    public String getFullName() {
        if (prenom != null && nom != null) {
            return prenom + " " + nom;
        } else if (nom != null) {
            return nom;
        } else if (prenom != null) {
            return prenom;
        } else {
            return email;
        }
    }
}