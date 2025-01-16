import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    // Liste de toutes les parties existantes
    private final List<Partie> parties = new ArrayList<>();
    // Liste globale des flux de tous les clients connectés
    private final List<PrintWriter> clients = new CopyOnWriteArrayList<>();

    public void startServer(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Serveur lancé sur le port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nouvelle connexion client : " + clientSocket.getRemoteSocketAddress());
                // Un nouveau thread par client
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Thread gérant la conversation avec UN client précis.
     */
    private class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private PrintWriter writer;
        private Partie currentPartie;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                // Flux pour écrire vers le client
                writer = new PrintWriter(clientSocket.getOutputStream(), true);
                // Ajoute le flux à la liste globale
                clients.add(writer);

                // Boucle "infinie" du lobby : tant que le client n'a pas
                // rejoint une partie en cours ou n'a pas créé de partie
                while (true) {
                    showLobbyMenu();

                    String line;
                    // On lit ce que le client envoie
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();

                        if (line.equalsIgnoreCase("QUIT")) {
                            writer.println("Au revoir !");
                            clients.remove(writer);
                            clientSocket.close();
                            return; // Quitte complètement le handler
                        }

                        if (line.equalsIgnoreCase("nouvelle")) {
                            // Le client crée une nouvelle partie
                            currentPartie = new Partie(writer, parties.size());
                            parties.add(currentPartie);
                            writer.println("Nouvelle partie créée avec l'ID " + (parties.size() - 1));
                            break; // Sort du menu du lobby pour passer à la boucle de la partie
                        } else if (line.matches("[0-9]+")) {
                            // Le client saisit un ID de partie
                            int partieId = Integer.parseInt(line);
                            if (partieId >= 0 && partieId < parties.size()) {
                                currentPartie = parties.get(partieId);
                                currentPartie.addJoueur(writer);
                                if (currentPartie.isFull()) {
                                    writer.println("Vous avez rejoint la partie " + partieId + ".");
                                }
                                break; // Sort du menu pour aller à la boucle de partie
                            } else {
                                writer.println("ID de partie invalide.");
                            }
                        } else {
                            writer.println("Commande invalide. Entrez 'nouvelle' ou un ID pour rejoindre une partie.");
                        }
                    }

                    // Ici, on sort de la boucle du lobby parce qu'on a créé/rejoint une partie
                    // => on gère maintenant la boucle "de partie"
                    while (!currentPartie.getFini() && (line = reader.readLine()) != null) {
                        line = line.trim();

                        // Si le joueur tape QUIT en pleine partie
                        if (line.equalsIgnoreCase("QUIT")) {
                            writer.println("Au revoir !");
                            clients.remove(writer);
                            clientSocket.close();
                            return; // On arrête complètement ce client
                        }

                        // Tenter de jouer un coup
                        boolean partieTerminee = currentPartie.handleMove(writer, line);
                        if (partieTerminee) {
                            // Si handleMove() renvoie true, c'est que la partie est finie :
                            // on remet la variable currentPartie à null => retour au lobby
                            currentPartie = null;
                            // Supprimer les parties terminées
                            supprimerPartieTerminee();
                            break; // On retourne au while (true) pour re-proposer le lobby
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                // Nettoyage
                clients.remove(writer);
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Affiche le menu de lobby à un client.
         */
        private void showLobbyMenu() {
            writer.println("\n--- Menu Principal ---");
            writer.println("Entrez 'nouvelle' pour créer une partie, un ID pour rejoindre une partie, ou 'QUIT' pour quitter.");
        }
    }

    /**
     * Méthode pour supprimer les parties terminées de la liste
     */
    private void supprimerPartieTerminee() {
        Iterator<Partie> iterator = parties.iterator();
        while (iterator.hasNext()) {
            Partie partie = iterator.next();
            if (partie.getFini()) { // Vérifie si la partie est terminée
                iterator.remove(); // Supprime la partie de la liste
                System.out.println("La partie a été supprimée.");
            }
        }
    }

    public static void main(String[] args) {
        new Server().startServer(5555);
    }
}
