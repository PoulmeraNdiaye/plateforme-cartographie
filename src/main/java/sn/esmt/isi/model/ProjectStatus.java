package sn.esmt.isi.model;

public enum ProjectStatus {
    EN_COURS("En cours"),
    TERMINE("Terminé"),
    SUSPENDU("Suspendu");

    private final String label;
    ProjectStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}