package sn.esmt.isi.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.constants.StandardFonts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sn.esmt.isi.model.ResearchProject;
import sn.esmt.isi.repository.ProjectRepository;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class PdfExportService {

        @Autowired
        private ProjectRepository projectRepository;

        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        private static final DeviceRgb PRIMARY_COLOR = new DeviceRgb(13, 110, 253);
        private static final DeviceRgb HEADER_BG = new DeviceRgb(240, 242, 245);

        /**
         * Export a research project to PDF format
         * 
         * @param projectId The ID of the project to export
         * @return PDF file as byte array
         */
        public byte[] exportProjectToPdf(Long projectId) {
                ResearchProject project = projectRepository.findById(projectId)
                                .orElseThrow(() -> new RuntimeException("Projet non trouvé"));

                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                try {
                        PdfWriter writer = new PdfWriter(baos);
                        PdfDocument pdfDoc = new PdfDocument(writer);
                        Document document = new Document(pdfDoc);

                        // Add title
                        Paragraph title = new Paragraph("DÉTAILS DU PROJET DE RECHERCHE")
                                        .setFontSize(20)
                                        .setBold()
                                        .setFontColor(PRIMARY_COLOR)
                                        .setTextAlignment(TextAlignment.CENTER)
                                        .setMarginBottom(20);
                        document.add(title);

                        // Project title
                        Paragraph projectTitle = new Paragraph(
                                        project.getTitreProjet() != null ? project.getTitreProjet() : "Sans titre")
                                        .setFontSize(16)
                                        .setBold()
                                        .setTextAlignment(TextAlignment.CENTER)
                                        .setMarginBottom(30);
                        document.add(projectTitle);

                        // Create information table
                        Table table = new Table(UnitValue.createPercentArray(new float[] { 30, 70 }))
                                        .useAllAvailableWidth()
                                        .setMarginBottom(20);

                        // Add rows
                        addTableRow(table, "Propriétaire",
                                        project.getProprietaire() != null
                                                        ? project.getProprietaire().getPrenom() + " "
                                                                        + project.getProprietaire().getNom() +
                                                                        " (" + project.getProprietaire().getEmail()
                                                                        + ")"
                                                        : "—");

                        addTableRow(table, "Domaine de recherche",
                                        project.getDomaineRecherche() != null ? project.getDomaineRecherche() : "—");

                        addTableRow(table, "Institution",
                                        project.getInstitution() != null ? project.getInstitution() : "—");

                        addTableRow(table, "Responsable / Encadrant",
                                        project.getResponsableProjet() != null ? project.getResponsableProjet() : "—");

                        addTableRow(table, "Statut",
                                        project.getStatutProjet() != null
                                                        ? formatStatut(project.getStatutProjet().toString())
                                                        : "—");

                        addTableRow(table, "Avancement",
                                        project.getNiveauAvancement() != null ? project.getNiveauAvancement() + " %"
                                                        : "—");

                        addTableRow(table, "Budget estimé",
                                        project.getBudgetEstime() != null
                                                        ? String.format("%,d FCFA",
                                                                        project.getBudgetEstime().longValue())
                                                        : "—");

                        addTableRow(table, "Date de début",
                                        project.getDateDebut() != null ? project.getDateDebut().format(DATE_FORMATTER)
                                                        : "—");

                        addTableRow(table, "Date de fin prévue",
                                        project.getDateFin() != null ? project.getDateFin().format(DATE_FORMATTER)
                                                        : "—");

                        addTableRow(table, "Date de création",
                                        project.getDateCreation() != null
                                                        ? project.getDateCreation().format(
                                                                        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                                                        : "—");

                        document.add(table);

                        // Participants section
                        if (project.getListeParticipants() != null && !project.getListeParticipants().isEmpty()) {
                                Paragraph participantsTitle = new Paragraph("Participants (Internes)")
                                                .setFontSize(14)
                                                .setBold()
                                                .setFontColor(PRIMARY_COLOR)
                                                .setMarginTop(20)
                                                .setMarginBottom(10);
                                document.add(participantsTitle);

                                Paragraph participants = new Paragraph(project.getListeParticipants())
                                                .setFontSize(11)
                                                .setMarginBottom(15);
                                document.add(participants);
                        }

                        // External participants section
                        if (project.getAutresParticipants() != null && !project.getAutresParticipants().isEmpty()) {
                                Paragraph externalTitle = new Paragraph("Autres Participants (Externes)")
                                                .setFontSize(14)
                                                .setBold()
                                                .setFontColor(PRIMARY_COLOR)
                                                .setMarginTop(20)
                                                .setMarginBottom(10);
                                document.add(externalTitle);

                                Paragraph external = new Paragraph(project.getAutresParticipants())
                                                .setFontSize(11)
                                                .setMarginBottom(15);
                                document.add(external);
                        }

                        // Description section
                        if (project.getDescription() != null && !project.getDescription().isEmpty()) {
                                Paragraph descTitle = new Paragraph("Description")
                                                .setFontSize(14)
                                                .setBold()
                                                .setFontColor(PRIMARY_COLOR)
                                                .setMarginTop(20)
                                                .setMarginBottom(10);
                                document.add(descTitle);

                                Paragraph description = new Paragraph(project.getDescription())
                                                .setFontSize(11)
                                                .setTextAlignment(TextAlignment.JUSTIFIED);
                                document.add(description);
                        }

                        // Footer
                        Paragraph footer = new Paragraph(
                                        "Document généré automatiquement - Cartographie des Projets de Recherche")
                                        .setFontSize(9)
                                        .setFontColor(ColorConstants.GRAY)
                                        .setTextAlignment(TextAlignment.CENTER)
                                        .setMarginTop(30);
                        document.add(footer);

                        document.close();
                        return baos.toByteArray();

                } catch (Exception e) {
                        throw new RuntimeException("Erreur lors de la génération du PDF", e);
                }
        }

        /**
         * Add a row to the information table
         */
        private void addTableRow(Table table, String label, String value) {
                Cell labelCell = new Cell()
                                .add(new Paragraph(label).setBold())
                                .setBackgroundColor(HEADER_BG)
                                .setPadding(8);

                Cell valueCell = new Cell()
                                .add(new Paragraph(value))
                                .setPadding(8);

                table.addCell(labelCell);
                table.addCell(valueCell);
        }

        /**
         * Format status for display
         */
        private String formatStatut(String statut) {
                switch (statut) {
                        case "EN_COURS":
                                return "En cours";
                        case "TERMINE":
                                return "Terminé";
                        case "SUSPENDU":
                                return "Suspendu";
                        default:
                                return statut;
                }
        }

        /**
         * Generate a PDF document from statistics data
         */
        public byte[] generateStatisticsPdf(Map<String, Object> stats, String title) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                try {
                        PdfWriter writer = new PdfWriter(baos);
                        PdfDocument pdfDoc = new PdfDocument(writer);
                        Document document = new Document(pdfDoc);

                        // Title
                        Paragraph titleParagraph = new Paragraph(title)
                                        .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                                        .setFontSize(20)
                                        .setFontColor(PRIMARY_COLOR)
                                        .setTextAlignment(TextAlignment.CENTER)
                                        .setMarginBottom(20);
                        document.add(titleParagraph);

                        // Date
                        Paragraph dateParagraph = new Paragraph(
                                        "Généré le : " + java.time.LocalDate.now().format(DATE_FORMATTER))
                                        .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                                        .setFontSize(10)
                                        .setFontColor(ColorConstants.GRAY)
                                        .setTextAlignment(TextAlignment.CENTER)
                                        .setMarginBottom(20);
                        document.add(dateParagraph);

                        // Main statistics
                        Paragraph sectionTitle = new Paragraph("Statistiques Principales")
                                        .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                                        .setFontSize(14)
                                        .setFontColor(PRIMARY_COLOR)
                                        .setMarginTop(10)
                                        .setMarginBottom(10);
                        document.add(sectionTitle);

                        Table mainStatsTable = new Table(new float[] { 3, 1 });
                        mainStatsTable.setWidth(UnitValue.createPercentValue(100));

                        addTableRow(mainStatsTable, "Total Projets",
                                        String.valueOf(stats.getOrDefault("totalProjets", 0)));
                        addTableRow(mainStatsTable, "Projets en Cours",
                                        String.valueOf(stats.getOrDefault("projetsEnCours", 0)));
                        addTableRow(mainStatsTable, "Projets Terminés",
                                        String.valueOf(stats.getOrDefault("projetsTermines", 0)));
                        addTableRow(mainStatsTable, "Projets Suspendus",
                                        String.valueOf(stats.getOrDefault("projetsSuspendus", 0)));
                        addTableRow(mainStatsTable, "Avancement Moyen",
                                        stats.getOrDefault("avancementMoyen", 0) + "%");

                        Object budgetTotal = stats.get("budgetTotal");
                        String budgetStr = budgetTotal != null
                                        ? String.format("%,.0f F CFA", ((Number) budgetTotal).doubleValue())
                                        : "0 F CFA";
                        addTableRow(mainStatsTable, "Budget Total", budgetStr);

                        document.add(mainStatsTable);

                        // User statistics
                        sectionTitle = new Paragraph("Statistiques Utilisateurs")
                                        .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                                        .setFontSize(14)
                                        .setFontColor(PRIMARY_COLOR)
                                        .setMarginTop(20)
                                        .setMarginBottom(10);
                        document.add(sectionTitle);

                        Table userStatsTable = new Table(new float[] { 3, 1 });
                        userStatsTable.setWidth(UnitValue.createPercentValue(100));

                        addTableRow(userStatsTable, "Total Utilisateurs",
                                        String.valueOf(stats.getOrDefault("totalUtilisateurs", 0)));
                        addTableRow(userStatsTable, "Candidats",
                                        String.valueOf(stats.getOrDefault("candidats", 0)));
                        addTableRow(userStatsTable, "Gestionnaires",
                                        String.valueOf(stats.getOrDefault("gestionnaires", 0)));
                        addTableRow(userStatsTable, "Administrateurs",
                                        String.valueOf(stats.getOrDefault("admins", 0)));

                        document.add(userStatsTable);

                        // Section: Répartition par Domaine
                        @SuppressWarnings("unchecked")
                        Map<String, Long> repartitionDomaines = (Map<String, Long>) stats.get("repartitionDomaines");
                        if (repartitionDomaines != null && !repartitionDomaines.isEmpty()) {
                                Paragraph domaineSectionTitle = new Paragraph("Répartition par Domaine de Recherche")
                                                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                                                .setFontSize(14)
                                                .setFontColor(PRIMARY_COLOR)
                                                .setMarginTop(20)
                                                .setMarginBottom(10);
                                document.add(domaineSectionTitle);

                                Table domaineTable = new Table(new float[] { 3, 1, 1 });
                                domaineTable.setWidth(UnitValue.createPercentValue(100));

                                // En-têtes
                                addTableHeader(domaineTable, "Domaine");
                                addTableHeader(domaineTable, "Projets");
                                addTableHeader(domaineTable, "Pourcentage");

                                long totalProjets = ((Number) stats.getOrDefault("totalProjets", 0)).longValue();

                                for (Map.Entry<String, Long> entry : repartitionDomaines.entrySet()) {
                                        long count = entry.getValue();
                                        double percentage = totalProjets > 0 ? (count * 100.0 / totalProjets) : 0;

                                        domaineTable.addCell(createCell(entry.getKey()));
                                        domaineTable.addCell(createCell(String.valueOf(count)));
                                        domaineTable.addCell(createCell(String.format("%.1f%%", percentage)));
                                }

                                document.add(domaineTable);
                        }

                        // Section: Répartition par Statut
                        @SuppressWarnings("unchecked")
                        Map<String, Long> repartitionStatuts = (Map<String, Long>) stats
                                        .get("repartitionStatuts");
                        if (repartitionStatuts != null && !repartitionStatuts.isEmpty()) {
                                Paragraph statutSectionTitle = new Paragraph("Répartition par Statut")
                                                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                                                .setFontSize(14)
                                                .setFontColor(PRIMARY_COLOR)
                                                .setMarginTop(20)
                                                .setMarginBottom(10);
                                document.add(statutSectionTitle);

                                Table statutTable = new Table(new float[] { 2, 1, 1 });
                                statutTable.setWidth(UnitValue.createPercentValue(100));

                                // En-têtes
                                addTableHeader(statutTable, "Statut");
                                addTableHeader(statutTable, "Nombre");
                                addTableHeader(statutTable, "Pourcentage");

                                long totalProjets = ((Number) stats.getOrDefault("totalProjets", 0)).longValue();

                                for (Map.Entry<String, Long> entry : repartitionStatuts.entrySet()) {
                                        long count = entry.getValue();
                                        double percentage = totalProjets > 0 ? (count * 100.0 / totalProjets) : 0;

                                        statutTable.addCell(createCell(formatStatut(entry.getKey())));
                                        statutTable.addCell(createCell(String.valueOf(count)));
                                        statutTable.addCell(createCell(String.format("%.1f%%", percentage)));
                                }

                                document.add(statutTable);
                        }

                        // Section: Budget par Domaine
                        @SuppressWarnings("unchecked")
                        Map<String, Double> budgetParDomaine = (Map<String, Double>) stats.get("budgetParDomaine");
                        if (budgetParDomaine != null && !budgetParDomaine.isEmpty()) {
                                Paragraph budgetSectionTitle = new Paragraph("Budget par Domaine")
                                                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                                                .setFontSize(14)
                                                .setFontColor(PRIMARY_COLOR)
                                                .setMarginTop(20)
                                                .setMarginBottom(10);
                                document.add(budgetSectionTitle);

                                Table budgetTable = new Table(new float[] { 3, 2 });
                                budgetTable.setWidth(UnitValue.createPercentValue(100));

                                // En-têtes
                                addTableHeader(budgetTable, "Domaine");
                                addTableHeader(budgetTable, "Budget Total");

                                for (Map.Entry<String, Double> entry : budgetParDomaine.entrySet()) {
                                        budgetTable.addCell(createCell(entry.getKey()));
                                        budgetTable.addCell(
                                                        createCell(String.format("%,.0f F CFA", entry.getValue())));
                                }

                                document.add(budgetTable);
                        }

                        // Section: Évolution Mensuelle
                        @SuppressWarnings("unchecked")
                        java.util.List<String> moisLabels = (java.util.List<String>) stats.get("moisLabels");
                        @SuppressWarnings("unchecked")
                        java.util.List<Long> projectsGrowthData = (java.util.List<Long>) stats
                                        .get("projectsGrowthData");

                        if (moisLabels != null && projectsGrowthData != null && !moisLabels.isEmpty()) {
                                Paragraph evolutionSectionTitle = new Paragraph(
                                                "Évolution Mensuelle des Projets (12 derniers mois)")
                                                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                                                .setFontSize(14)
                                                .setFontColor(PRIMARY_COLOR)
                                                .setMarginTop(20)
                                                .setMarginBottom(10);
                                document.add(evolutionSectionTitle);

                                Table evolutionTable = new Table(new float[] { 2, 1 });
                                evolutionTable.setWidth(UnitValue.createPercentValue(100));

                                // En-têtes
                                addTableHeader(evolutionTable, "Mois");
                                addTableHeader(evolutionTable, "Nouveaux Projets");

                                for (int i = 0; i < moisLabels.size(); i++) {
                                        evolutionTable.addCell(createCell(moisLabels.get(i)));
                                        evolutionTable.addCell(createCell(String.valueOf(projectsGrowthData.get(i))));
                                }

                                document.add(evolutionTable);
                        }

                        // Section: Projets par Participant (Top 10)
                        @SuppressWarnings("unchecked")
                        Map<String, Long> projetsParParticipant = (Map<String, Long>) stats
                                        .get("projetsParParticipant");
                        if (projetsParParticipant != null && !projetsParParticipant.isEmpty()) {
                                Paragraph participantSectionTitle = new Paragraph("Top 10 Participants")
                                                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                                                .setFontSize(14)
                                                .setFontColor(PRIMARY_COLOR)
                                                .setMarginTop(20)
                                                .setMarginBottom(10);
                                document.add(participantSectionTitle);

                                Table participantTable = new Table(new float[] { 3, 1 });
                                participantTable.setWidth(UnitValue.createPercentValue(100));

                                // En-têtes
                                addTableHeader(participantTable, "Participant");
                                addTableHeader(participantTable, "Nombre de Projets");

                                // Limiter à 10 participants
                                int count = 0;
                                for (Map.Entry<String, Long> entry : projetsParParticipant.entrySet()) {
                                        if (count >= 10)
                                                break;
                                        participantTable.addCell(createCell(entry.getKey()));
                                        participantTable.addCell(createCell(String.valueOf(entry.getValue())));
                                        count++;
                                }

                                document.add(participantTable);
                        }

                        // Footer
                        Paragraph footer = new Paragraph(
                                        "Plateforme de Cartographie des Projets de Recherche - ESMT")
                                        .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                                        .setFontSize(8)
                                        .setFontColor(ColorConstants.GRAY)
                                        .setTextAlignment(TextAlignment.CENTER)
                                        .setMarginTop(30);
                        document.add(footer);

                        document.close();
                        return baos.toByteArray();

                } catch (Exception e) {
                        throw new RuntimeException("Erreur lors de la génération du PDF des statistiques", e);
                }
        }

        /**
         * Add a table header cell
         */
        private void addTableHeader(Table table, String headerText) throws java.io.IOException {
                Cell headerCell = new Cell()
                                .add(new Paragraph(headerText)
                                                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                                                .setFontSize(10))
                                .setBackgroundColor(HEADER_BG)
                                .setTextAlignment(TextAlignment.CENTER)
                                .setPadding(8);
                table.addHeaderCell(headerCell);
        }

        /**
         * Create a standard table cell
         */
        private Cell createCell(String text) throws java.io.IOException {
                return new Cell()
                                .add(new Paragraph(text)
                                                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                                                .setFontSize(9))
                                .setPadding(6);
        }
}
