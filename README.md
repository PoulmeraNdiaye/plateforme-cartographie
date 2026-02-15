# 🗺️ Plateforme de Cartographie des Projets de Recherche

Plateforme web de gestion et de visualisation des projets de recherche pour l'ESMT (École Supérieure Multinationale des Télécommunications).

## 📋 Description

Cette application Spring Boot permet de :
- ✅ Gérer les projets de recherche (création, modification, suivi)
- ✅ Gérer les utilisateurs (candidats, gestionnaires, administrateurs)
- ✅ Visualiser des statistiques et graphiques interactifs
- ✅ Générer des rapports détaillés
- ✅ Exporter et imprimer les données

## 🚀 Technologies Utilisées

- **Backend** : Java 17, Spring Boot 3.x
- **Frontend** : Thymeleaf, Bootstrap 5, Chart.js
- **Base de données** : MySQL
- **PDF** : iText 7
- **Build** : Maven

## 📦 Installation

### Prérequis

- Java 17 ou supérieur
- MySQL 8.0 ou supérieur
- Maven 3.6+

### Configuration

1. **Cloner le projet** :
```bash
git clone https://github.com/VOTRE_USERNAME/plateforme-cartographie.git
cd plateforme-cartographie
```

2. **Configurer la base de données** :
   - Créer une base de données MySQL nommée `cartographie_projets`
   - Modifier `src/main/resources/application.properties` avec vos paramètres :
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/cartographie_projets
spring.datasource.username=VOTRE_USERNAME
spring.datasource.password=VOTRE_PASSWORD
```

3. **Compiler et lancer** :
```bash
mvn clean install
mvn spring-boot:run
```

4. **Accéder à l'application** :
   - URL : `http://localhost:8080`
   - Login admin par défaut : `admin@esmt.sn` / `admin123`

## 👥 Rôles Utilisateurs

- **👨‍💼 Administrateur** : Gestion complète (utilisateurs, projets, domaines, statistiques)
- **📊 Gestionnaire** : Gestion des projets et visualisation des statistiques
- **🎓 Candidat** : Création et suivi de ses propres projets

## 📊 Fonctionnalités Principales

### Administration
- Gestion des utilisateurs et rôles
- Gestion des domaines de recherche
- Statistiques globales et rapports détaillés
- Graphiques interactifs (Chart.js)
- Export et impression PDF

### Gestion de Projets
- Création et modification de projets
- Ajout de participants (internes/externes)
- Suivi de l'avancement
- Gestion des budgets
- Historique des modifications

### Visualisation
- Tableaux de bord personnalisés par rôle
- Graphiques interactifs (domaines, statuts, évolution)
- Statistiques en temps réel
- Rapports exportables

## 🛠️ Structure du Projet

```
plateforme_cartographie/
├── src/main/java/sn/esmt/isi/
│   ├── controller/     # Contrôleurs Spring MVC
│   ├── model/          # Entités JPA
│   ├── repository/     # Repositories Spring Data
│   ├── service/        # Logique métier
│   └── config/         # Configuration Spring Security
├── src/main/resources/
│   ├── templates/      # Templates Thymeleaf
│   │   ├── admin/
│   │   ├── manager/
│   │   └── candidate/
│   ├── static/         # CSS, JS, images
│   └── application.properties
└── pom.xml
```

## 📝 Licence

Ce projet est développé pour l'ESMT (École Supérieure Multinationale des Télécommunications).

## 👨‍💻 Auteur

Développé pour l'ESMT - 2026

## 🤝 Contribution

Les contributions sont les bienvenues ! N'hésitez pas à ouvrir une issue ou une pull request.

## 📧 Contact

Pour toute question : contact@esmt.sn
