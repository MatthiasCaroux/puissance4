import java.util.ArrayList;
import java.util.List;

public class Plateau {
    private List<List<Case>> plateau;

    public Plateau() {
        plateau = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            List<Case> ligne = new ArrayList<>();
            for (int j = 0; j < 7; j++) {
                ligne.add(new Case());
            }
            plateau.add(ligne);
        }
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

    public void jouer(char couleur, int colonne) {
        for (int i = 5; i >= 0; i--) {
            for (int j = 0; j < 7; j++) {
                if (j == colonne && this.getEtat(i, j) == 'V') {
                    plateau.get(i).get(j).jouer(couleur);
                    return;
                }
            }
        }
    }
}
