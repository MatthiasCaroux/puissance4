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

    public Partie(PrintWriter creator, int idPartie) {
        this.clients.add(creator);
        this.idPartie = idPartie;
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

    public boolean handleMove(PrintWriter writer, String input) {
        lock.lock();
        try {
            int playerIndex = clients.indexOf(writer);
    
            if (playerIndex != currentPlayerIndex) {
                writer.println("Ce n'est pas votre tour. Attendez votre tour.");
                return false;
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
    
                broadcastPlateau();
    
                if (plateau.estGagne()) {
                    broadcastMessage("Le joueur " + (currentPlayerIndex + 1) + " a gagné !");
                    resetPartie(); // Retour au lobby
                    return true; // Signale la fin de la partie
                }
    
                currentPlayerIndex = (currentPlayerIndex + 1) % clients.size();
                broadcastMessage("C'est au joueur " + (currentPlayerIndex + 1) + " de jouer.");
            } catch (NumberFormatException e) {
                writer.println("Commande invalide. Entrez un nombre (1-7).");
            }
        } finally {
            lock.unlock();
        }
        return false; // La partie continue
    }
    
    private void resetPartie() {
        broadcastMessage("La partie est terminée. Vous êtes maintenant de retour au lobby.");
        for (PrintWriter client : clients) {
            client.println("Entrez 'nouvelle' pour créer une partie, un ID pour rejoindre une partie, ou 'QUIT' pour quitter.");
        }
    }
    

    private void broadcastMessage(String message) {
        for (PrintWriter client : clients) {
            client.println(message);
        }
    }

    private void broadcastPlateau() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n----- État du Plateau -----\n");
        sb.append(plateau.toString());
        sb.append("\n---------------------------\n");
        broadcastMessage(sb.toString());
    }
}
