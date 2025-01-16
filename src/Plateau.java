import java.util.ArrayList;
import java.util.List;

public class Plateau {
    private List<List<Case>> plateau;
    private char couleurCourante;

    public Plateau() {
        plateau = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            List<Case> ligne = new ArrayList<>();
            for (int j = 0; j < 7; j++) {
                ligne.add(new Case());
            }
            plateau.add(ligne);
        }
        this.couleurCourante = 'R'; // Le joueur Rouge commence
    }

    // Affiche le plateau de jeu
    public void afficher() {
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 7; j++) {
                plateau.get(i).get(j).afficher();
                if (j < 6) System.out.print("|"); // Ajouter un séparateur pour la grille
            }
            System.out.println();
            if (i < 5) System.out.println("---------------"); // Séparateur entre les lignes
        }
        System.out.println("-----------------");
    }

    // Retourne l'état d'une case
    public char getEtat(int i, int j) {
        return plateau.get(i).get(j).getEtat();
    }

    // Permet de jouer un coup dans une colonne
    public boolean jouer(int colonne) {
        if (colonne < 0 || colonne >= 7 || this.getEtat(0, colonne) != 'V') {
            System.out.println("Colonne invalide ou pleine");
            return false; // La colonne est invalide ou pleine
        }

        for (int i = 5; i >= 0; i--) {
            if (this.getEtat(i, colonne) == 'V') {
                plateau.get(i).get(colonne).jouer(this.couleurCourante);
                this.changerCouleurCourante();
                return true;
            }
        }
        return false;
    }

    // Change la couleur du joueur actuel
    public void changerCouleurCourante() {
        this.couleurCourante = (this.couleurCourante == 'R') ? 'J' : 'R';
    }

    // Vérifie si le plateau est plein
    public boolean estPlein() {
        for (int j = 0; j < 7; j++) {
            if (this.getEtat(0, j) == 'V') { // Si la première ligne contient encore une case vide
                return false;
            }
        }
        return true;
    }

    // Vérifie s'il y a une victoire
    public boolean estGagne() {
        // Vérification horizontale, verticale et diagonale
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 7; j++) {
                char couleur = this.getEtat(i, j);
                if (couleur != 'V') {
                    // Vérification horizontale
                    if (j + 3 < 7 && couleur == this.getEtat(i, j + 1) && couleur == this.getEtat(i, j + 2) && couleur == this.getEtat(i, j + 3)) {
                        return true;
                    }
                    // Vérification verticale
                    if (i + 3 < 6 && couleur == this.getEtat(i + 1, j) && couleur == this.getEtat(i + 2, j) && couleur == this.getEtat(i + 3, j)) {
                        return true;
                    }
                    // Vérification diagonale montante
                    if (i + 3 < 6 && j + 3 < 7 && couleur == this.getEtat(i + 1, j + 1) && couleur == this.getEtat(i + 2, j + 2) && couleur == this.getEtat(i + 3, j + 3)) {
                        return true;
                    }
                    // Vérification diagonale descendante
                    if (i + 3 < 6 && j - 3 >= 0 && couleur == this.getEtat(i + 1, j - 1) && couleur == this.getEtat(i + 2, j - 2) && couleur == this.getEtat(i + 3, j - 3)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // Retourne la couleur du joueur courant
    public char getCouleurCourante() {
        return this.couleurCourante;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 7; j++) {
                sb.append(plateau.get(i).get(j).toString());
                if (j < 6) sb.append(" | "); // Séparateurs pour la lisibilité
            }
            sb.append("\n");
            if (i < 5) sb.append("---------------\n"); // Séparateur entre les lignes
        }
        return sb.toString();
    }
}
