package sn.esmt.isi.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "app_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppConfig {

    @Id
    private Long id = 1L; // Toujours 1 pour singleton

    private String siteName = "Plateforme Cartographie";
    private String contactEmail = "admin@esmt.sn";
    private Boolean maintenanceMode = false;
    private Boolean registrationOpen = true;

    // Métadonnées
    private String version = "1.0.0";
}
