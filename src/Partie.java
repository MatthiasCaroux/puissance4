import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Partie {
    private final List<PrintWriter> clients = new ArrayList<>();
    private final List<String> playerNames = new ArrayList<>();  //  stocke les noms
    private final Plateau plateau = new Plateau();
    private final ReentrantLock lock = new ReentrantLock();
    
    private final int idPartie;
    private int currentPlayerIndex = 0;
    private boolean fini;  // indique si la partie est terminée


    public Partie(PrintWriter creatorWriter, int idPartie, String creatorName) {
        this.idPartie = idPartie;
        this.fini = false;

        // Ajout du premier joueur
        this.clients.add(creatorWriter);
        this.playerNames.add(creatorName);

        broadcastMessage("Partie #" + idPartie + " créée par " + creatorName + ". En attente d'un autre joueur...");
    }

    public void addJoueur(PrintWriter writer, String playerName) {
        if (clients.size() < 2) {
            this.clients.add(writer);
            this.playerNames.add(playerName);
            broadcastMessage(playerName + " a rejoint la partie #" + idPartie + " !");

            if (clients.size() == 2) {
                broadcastMessage("Les deux joueurs (" + playerNames.get(0) + " et " + playerNames.get(1) + ") sont prêts !");
                broadcastPlateau();
                broadcastMessage("C'est au tour de " + playerNames.get(currentPlayerIndex) + " de commencer.");
            }
        } else {
            writer.println("La partie #" + idPartie + " est déjà pleine.");
        }
    }

  
    public boolean isFull() {
        return this.clients.size() == 2;
    }


    public boolean getFini() {
        return this.fini;
    }


    public void terminerPartie() {
        this.fini = true;
    }

    
    public boolean handleMove(PrintWriter writer, String playerName, String input) {
        lock.lock();
        try {
            // On retrouve l'index du joueur qui joue
            int playerIndex = clients.indexOf(writer);
            if (playerIndex != currentPlayerIndex) {
                writer.println("Ce n'est pas votre tour, " + playerName + ". Patientez !");
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
                    String winnerName = playerNames.get(currentPlayerIndex);
                    broadcastMessage("Bravo, " + winnerName + " a réussi un Puissance 4 !");
                    
                    // Trouve le perdant
                    if (playerNames.size() == 2) {
                        int losingPlayerIndex = (currentPlayerIndex + 1) % 2;
                        String loserName = playerNames.get(losingPlayerIndex);
                        broadcastMessage("Dommage pour " + loserName + " ...");
                        
                        // On peut aussi envoyer un message direct au perdant
                        clients.get(losingPlayerIndex)
                               .println("Vous avez perdu la partie. Vous êtes renvoyé au lobby !");
                    }

                    // On marque la partie terminée
                    terminerPartie();
                    broadcastMessage("La partie #" + idPartie + " est terminée. Vous revenez tous au lobby...");
                    return true; // on signale la fin de la partie
                }

                
                currentPlayerIndex = (currentPlayerIndex + 1) % clients.size();
                broadcastMessage("C'est maintenant à " + playerNames.get(currentPlayerIndex) + " de jouer.");

            } catch (NumberFormatException e) {
                writer.println("Commande invalide, " + playerName + ". Entrez un nombre (1-7).");
            }
        } finally {
            lock.unlock();
        }
        return false;  
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
        sb.append("---------------------------\n");
        broadcastMessage(sb.toString());
    }
    

    public String getJoueurs() {
        for (PrintWriter client : clients) {
            return client.toString();
        }
        return "";
    }
}
