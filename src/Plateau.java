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
        this.couleurCourante = 'R'; // on commence par Rouge
    }

    public void afficher() {
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 7; j++) {
                plateau.get(i).get(j).afficher();
            }
            System.out.println();
        }
        System.out.println("-----------------");
    }

    public char getEtat(int i, int j) {
        return plateau.get(i).get(j).getEtat();
    }

    public boolean jouer(int colonne) {
        // On insère dans la case vide la plus basse
        for (int i = 5; i >= 0; i--) {
            if (this.getEtat(i, colonne) == 'V') {
                plateau.get(i).get(colonne).jouer(this.couleurCourante);
                this.changerCouleurCourante();
                return true;
            }
        }
        return false;
    }

    public void changerCouleurCourante() {
        this.couleurCourante = (this.couleurCourante == 'R') ? 'J' : 'R';
    }

    public boolean estGagne() {
        // Vérification horizontale, verticale, diagonales...
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 7; j++) {
                char couleur = this.getEtat(i, j);
                if (couleur != 'V') {
                    // Horizontale
                    if (j + 3 < 7 && couleur == this.getEtat(i, j + 1)
                                  && couleur == this.getEtat(i, j + 2)
                                  && couleur == this.getEtat(i, j + 3))
                        return true;
                    // Verticale
                    if (i + 3 < 6 && couleur == this.getEtat(i + 1, j)
                                  && couleur == this.getEtat(i + 2, j)
                                  && couleur == this.getEtat(i + 3, j))
                        return true;
                    // Diagonale montante
                    if (i + 3 < 6 && j + 3 < 7 && couleur == this.getEtat(i + 1, j + 1)
                                             && couleur == this.getEtat(i + 2, j + 2)
                                             && couleur == this.getEtat(i + 3, j + 3))
                        return true;
                    // Diagonale descendante
                    if (i + 3 < 6 && j - 3 >= 0 && couleur == this.getEtat(i + 1, j - 1)
                                              && couleur == this.getEtat(i + 2, j - 2)
                                              && couleur == this.getEtat(i + 3, j - 3))
                        return true;
                }
            }
        }
        return false;
    }

    public char getCouleurCourante() {
        return this.couleurCourante;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 7; j++) {
                sb.append(plateau.get(i).get(j).toString());
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
