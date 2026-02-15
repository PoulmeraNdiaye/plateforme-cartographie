package sn.esmt.isi.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "domaines")
@Getter
@Setter
@NoArgsConstructor
public class Domaine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Transient field to hold project count for display, not persisted
    @Transient
    private long nbProjets;
}
