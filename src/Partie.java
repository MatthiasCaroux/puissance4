import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Partie {
    private final List<PrintWriter> clients = new ArrayList<>();
    private final Plateau plateau = new Plateau();
    private final ReentrantLock lock = new ReentrantLock();
    private int currentPlayerIndex = 0;
    private final int idPartie;
    private boolean fini;

    public Partie(PrintWriter creator, int idPartie) {
        this.clients.add(creator);
        this.idPartie = idPartie;
        this.fini = false;
    }

    public void addJoueur(PrintWriter writer) {
        if (clients.size() < 2) {
            this.clients.add(writer);
            broadcastMessage("Un joueur a rejoint la partie " + idPartie + ".");
            if (clients.size() == 2) {
                broadcastMessage("Les deux joueurs sont prêts. La partie commence !");
                broadcastPlateau(); // Affiche le plateau initial pour les deux joueurs.
                broadcastMessage("C'est au joueur 1 de commencer.");
            }
        } else {
            writer.println("La partie " + idPartie + " est déjà pleine.");
        }
    }

    public boolean isFull() {
        return this.clients.size() == 2;
    }

    /**
     * Gère un coup du joueur (un nombre de colonne). Retourne true si la partie est terminée
     * (quelqu'un a gagné ou la partie doit être coupée), sinon false si on continue.
     */
    public boolean handleMove(PrintWriter writer, String input) {
        lock.lock();
        try {
            int playerIndex = clients.indexOf(writer);

            // Vérifie si c'est bien le tour du joueur
            if (playerIndex != currentPlayerIndex) {
                writer.println("Ce n'est pas votre tour. Attendez votre tour.");
                return false;  // On ne sort pas de la partie
            }

            try {
                int colonne = Integer.parseInt(input);
                if (colonne < 1 || colonne > 7) {
                    writer.println("Colonne invalide. Choisissez une colonne entre 1 et 7.");
                    return false;
                }

                if (!plateau.jouer(colonne - 1)) {
                    writer.println("Cette colonne est pleine. Choisissez une autre colonne.");
                    return false;
                }

                // Affiche à tous le nouveau plateau
                broadcastPlateau();

                // Vérifie s’il y a un gagnant
                if (plateau.estGagne()) {
                    // Gagnant
                    broadcastMessage("Le joueur " + (currentPlayerIndex + 1) + " a gagné la partie !");
                    // Perdant
                    int losingPlayerIndex = (currentPlayerIndex + 1) % clients.size();
                    broadcastMessage("Le joueur " + (losingPlayerIndex + 1) + " a malheureusement perdu...");

                    // Retour au lobby pour tout le monde
                    resetPartie();
                    this.terminerPartie();
                    return true;  // Important : signaler la fin de la partie
                }

                // Sinon, on continue : on passe au joueur suivant
                currentPlayerIndex = (currentPlayerIndex + 1) % clients.size();
                broadcastMessage("C'est au joueur " + (currentPlayerIndex + 1) + " de jouer.");

            } catch (NumberFormatException e) {
                writer.println("Commande invalide. Entrez un nombre (1-7).");
            }
        } finally {
            lock.unlock();
        }
        return false;  // La partie continue
    }

    /**
     * Remet tous les joueurs au lobby en leur demandant de choisir
     * soit 'nouvelle' soit un ID de partie ou 'QUIT'.
     */
    private void resetPartie() {
        broadcastMessage("La partie est terminée. Vous êtes maintenant de retour au lobby.");
        for (PrintWriter client : clients) {
            client.println("Entrez 'nouvelle' pour créer une partie, un ID pour rejoindre une partie, ou 'QUIT' pour quitter.");
        }
    }

    /**
     * Envoie un message à tous les joueurs de cette partie.
     */
    private void broadcastMessage(String message) {
        for (PrintWriter client : clients) {
            client.println(message);
        }
    }

    /**
     * Envoie l'état actuel du plateau à tous les joueurs.
     */
    private void broadcastPlateau() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n----- État du Plateau -----\n");
        sb.append(plateau.toString());
        sb.append("\n---------------------------\n");
        broadcastMessage(sb.toString());
    }
    
    public boolean getFini(){
        return this.fini;
    }
    public void terminerPartie(){
        this.fini = true;
    }

    public String getJoueurs() {
        for (PrintWriter client : clients) {
            return client.toString();
        }
        return "";
    }
}
