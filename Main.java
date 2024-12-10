import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Plateau plateau = new Plateau();
        plateau.afficher();

        try (Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNext()) {
                System.out.println("Entrez la colonne où vous voulez jouer (1-7)");
                int colonne = scanner.nextInt();
                plateau.jouer(colonne-1);
                plateau.afficher();
                if (plateau.estGagne()) {
                    plateau.changerCouleurCourante();//on a changé de joueur après le dernier coup donc on doit changer de joueur pour afficher le bon joueur
                    System.out.println("Le joueur " + plateau.getCouleurCourante() + " a gagné !");
                    break;
                }
            }
        }
    }
}
