import java.io.Serializable;

public class Restaurant implements Serializable {
    private String nom;
    private double note; // Note sur 20
    private double prixMoyen; // Prix en euros

    public Restaurant(String nom, double note) {
        this.nom = nom;
        this.note = note;
        this.prixMoyen = -1; // -1 signifie "prix inconnu"
    }

    // Getters / Setters
    public String getNom() {
        return nom;
    }

    public double getNote() {
        return note;
    }

    public double getPrixMoyen() {
        return prixMoyen;
    }

    public void setPrixMoyen(double prix) {
        this.prixMoyen = prix;
    }

    @Override
    public String toString() {
        return String.format("%-20s | Note: %4.1f/20 | Prix: %s",
                nom,
                note,
                (prixMoyen > 0 ? prixMoyen + "€" : "N/A"));
    }
}