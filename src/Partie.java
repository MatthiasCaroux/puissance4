import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

class Partie {
    private final int id;
    private final Plateau plateau = new Plateau();
    private final List<PrintWriter> joueurs = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private boolean enCours = false;

    public Partie(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public boolean estPleine() {
        return joueurs.size() >= 2;
    }

    public boolean estEnCours() {
        return enCours;
    }

    public void ajouterJoueur(PrintWriter joueur) {
        if (joueurs.size() < 2) {
            joueurs.add(joueur);
            if (joueurs.size() == 2) {
                enCours = true;
            }
        }
    }

    public List<PrintWriter> getJoueurs() {
        return joueurs;
    }

    public Plateau getPlateau() {
        return plateau;
    }

    public ReentrantLock getLock() {
        return lock;
    }

    public void terminerPartie() {
        enCours = false;
        // Optionnel : Déconnecter les joueurs ou nettoyer les ressources
    }
}
