public class Main {
    public static void main(String[] args) {
        Plateau plateau = new Plateau();
        plateau.afficher();
        plateau.jouer('R', 3);
        plateau.afficher();
        plateau.jouer('J', 3);
        plateau.afficher();
    }
}
