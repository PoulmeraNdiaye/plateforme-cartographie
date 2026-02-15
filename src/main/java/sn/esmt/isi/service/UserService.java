package sn.esmt.isi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.esmt.isi.model.User;
import sn.esmt.isi.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Récupère tous les utilisateurs
     */
    public List<User> findAll() {
        return userRepository.findAll();
    }

    /**
     * Récupère un utilisateur par son ID
     */
    public User findById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé avec l'ID : " + id));
    }

    /**
     * Récupère un utilisateur par son email
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Sauvegarde ou met à jour un utilisateur
     */
    @Transactional
    public User save(User user) {
        // Validation simple (à renforcer selon tes besoins)
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("L'email est obligatoire");
        }

        // Vérification unicité email lors de la création
        if (user.getId() == null) {
            if (userRepository.findByEmail(user.getEmail()).isPresent()) {
                throw new IllegalArgumentException("Cet email est déjà utilisé");
            }
        } else {
            // Lors de la mise à jour : vérifier que l'email n'est pas déjà pris par un autre utilisateur
            Optional<User> existing = userRepository.findByEmail(user.getEmail());
            if (existing.isPresent() && !existing.get().getId().equals(user.getId())) {
                throw new IllegalArgumentException("Cet email est déjà utilisé par un autre compte");
            }
        }

        return userRepository.save(user);
    }

    /**
     * Met à jour les informations de base d'un utilisateur
     * (utilisé depuis le formulaire admin/users/edit)
     */
    @Transactional
    public User updateUser(String id, User updatedUser) {
        User existing = findById(id);

        // Mise à jour des champs autorisés
        existing.setPrenom(updatedUser.getPrenom());
        existing.setNom(updatedUser.getNom());
        existing.setTelephone(updatedUser.getTelephone());
        existing.setInstitution(updatedUser.getInstitution());
        existing.setDepartement(updatedUser.getDepartement());
        existing.setSpecialite(updatedUser.getSpecialite());
        existing.setNiveauEtude(updatedUser.getNiveauEtude());
        existing.setBio(updatedUser.getBio());
        existing.setRole(updatedUser.getRole());
        existing.setActive(updatedUser.getActive());

        return userRepository.save(existing);
    }

    /**
     * Active ou désactive un utilisateur
     */
    @Transactional
    public void toggleActive(String id) {
        User user = findById(id);
        user.setActive(!user.getActive());
        userRepository.save(user);
    }

    /**
     * Supprime un utilisateur
     * Attention : à utiliser avec prudence (peut casser des relations)
     */
    @Transactional
    public void deleteById(String id) {
        User user = findById(id);

        // Optionnel : vérifier s'il est propriétaire de projets
        // Si oui, tu peux soit interdire, soit transférer/supprimer les projets

        userRepository.delete(user);
    }

    /**
     * Compte le nombre total d'utilisateurs
     */
    public long countAll() {
        return userRepository.count();
    }

    /**
     * Compte par rôle
     */
    public long countByRole(String role) {
        return userRepository.countByRole(role);
    }

    /**
     * Liste des utilisateurs par rôle
     */
    public List<User> findByRole(String role) {
        return userRepository.findByRole(role);
    }

    /**
     * Recherche par nom/prénom/email (pour recherche admin)
     */
    public List<User> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        keyword = "%" + keyword.trim().toLowerCase() + "%";
        return userRepository.findBySearchTerm(keyword);
    }
}