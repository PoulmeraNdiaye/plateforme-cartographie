package sn.esmt.isi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sn.esmt.isi.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    // Recherche par email (déjà présente et correcte)
    Optional<User> findByEmail(String email);

    // Vérifie si un email existe déjà
    boolean existsByEmail(String email);

    // Recherche par rôle exact
    List<User> findByRole(String role);

    // Compte le nombre d'utilisateurs par rôle
    long countByRole(String role);

    // Recherche par institution
    List<User> findByInstitution(String institution);

    // Compte par provider (Google, GitHub, etc.)
    long countByProvider(String provider);

    // Recherche par nom ou prénom (insensible à la casse)
    List<User> findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCase(String nom, String prenom);

    // Recherche multi-critères : nom, prénom ou email (utilisée dans UserService.search)
    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.nom) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.prenom) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<User> findBySearchTerm(@Param("keyword") String keyword);

    // Statistiques : nombre d'utilisateurs par rôle
    @Query("SELECT u.role, COUNT(u) FROM User u GROUP BY u.role")
    List<Object[]> countUsersByRole();

    // Statistiques : nombre d'utilisateurs par institution
    @Query("SELECT u.institution, COUNT(u) FROM User u " +
            "WHERE u.institution IS NOT NULL GROUP BY u.institution")
    List<Object[]> countUsersByInstitution();

    // Statistiques : nouveaux utilisateurs par mois (année en cours)
    @Query("SELECT MONTH(u.createdAt), COUNT(u) " +
            "FROM User u " +
            "WHERE YEAR(u.createdAt) = YEAR(CURRENT_DATE) " +
            "GROUP BY MONTH(u.createdAt)")
    List<Object[]> countNewUsersByMonth();

    // Optionnel : tous les utilisateurs actifs
    List<User> findByActiveTrue();

    // Optionnel : utilisateurs créés après une date
    List<User> findByCreatedAtAfter(java.time.LocalDateTime date);
}