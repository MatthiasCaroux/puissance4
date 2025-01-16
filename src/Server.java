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


    private class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private PrintWriter writer;
        private String playerName;   // <-- Pour stocker le nom du joueur
        private Partie currentPartie;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                // Flux pour écrire vers le client
                writer = new PrintWriter(clientSocket.getOutputStream(), true);
                // On demande d’abord le nom du joueur (ou on le lit directement si déjà envoyé)
                playerName = reader.readLine(); 
                if (playerName == null || playerName.trim().isEmpty()) {
                    playerName = "Joueur" + (clients.size() + 1); // fallback
                }

                writer.println("Bonjour " + playerName + " !");
                writer.println("Bienvenue sur le serveur de Puissance 4.");
                writer.println("Vous pouvez taper :");
                writer.println(" - 'nouvelle' pour créer une partie");
                writer.println(" - un numéro de partie pour la rejoindre");
                writer.println(" - 'LISTE' pour voir les parties existantes");
                writer.println(" - 'QUIT' pour vous déconnecter");

                // Ajoute le flux à la liste globale 
                clients.add(writer);


                while (true) {
                    showLobbyMenu();  // Montre le petit menu du lobby

                    String line;
                    // On lit ce que le client envoie
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();

                        if (line.equalsIgnoreCase("QUIT")) {
                            writer.println("Au revoir, " + playerName + " !");
                            clients.remove(writer);
                            clientSocket.close();
                            return; // Quitte complètement le handler
                        }
                        else if (line.equalsIgnoreCase("LISTE")) {
                            afficherListeParties();
                            // On ne sort pas du lobby, le client peut retaper autre chose
                        }
                        else if (line.equalsIgnoreCase("nouvelle")) {
                            // Le client crée une nouvelle partie
                            currentPartie = new Partie(writer, parties.size(), playerName);
                            parties.add(currentPartie);
                            writer.println("Nouvelle partie créée avec l'ID " + (parties.size() - 1) + ".");
                            writer.println("En attente d'un autre joueur...");
                            break; // Sort du menu du lobby pour passer à la boucle de la partie
                        }
                        else if (line.matches("[0-9]+")) {
                            // Le client saisit un ID de partie
                            int partieId = Integer.parseInt(line);
                            if (partieId >= 0 && partieId < parties.size()) {
                                currentPartie = parties.get(partieId);
                                if (currentPartie.getFini()) {
                                    writer.println("Désolé, la partie #" + partieId + " est déjà terminée.");
                                } else if (currentPartie.isFull()) {
                                    writer.println("Désolé, la partie #" + partieId + " est déjà complète.");
                                } else {
                                    currentPartie.addJoueur(writer, playerName);
                                    writer.println("Vous avez rejoint la partie #" + partieId + ".");
                                    writer.println("En attente que la partie commence (ou que le créateur lance un coup)...");
                                    break; // Sort du menu pour aller à la boucle de partie
                                }
                            } else {
                                writer.println("ID de partie invalide. Tapez LISTE pour voir les parties.");
                            }
                        }
                        else {
                            writer.println("Commande invalide. Tapez 'nouvelle', 'LISTE', un ID de partie, ou 'QUIT'.");
                        }
                    }

                    // Ici, on sort de la boucle du lobby parce qu'on a créé/rejoint une partie

                    
                    while (currentPartie != null && !currentPartie.getFini() && (line = reader.readLine()) != null) {
                        line = line.trim();
                        writer.println("Entrez la colonne (1-7) ou QUIT pour quitter :"); 
                        // Si le joueur tape QUIT en pleine partie
                        if (line.equalsIgnoreCase("QUIT")) {
                            writer.println("Au revoir, " + playerName + " !");
                            clients.remove(writer);
                            clientSocket.close();
                            return; // On arrête complètement ce client
                        }

                        // On tente de jouer un coup
                        boolean partieTerminee = currentPartie.handleMove(writer, playerName, line);
                        if (partieTerminee) {
                            // La partie est finie => tout le monde retourne au lobby
                            currentPartie = null;
                            break; // On quitte la boucle de partie => on revient au while(true) du lobby
                        }


                    }
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                
                clients.remove(writer);
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        private void showLobbyMenu() {
            writer.println("\n--- Menu du Lobby ---");
            writer.println("Tapez 'nouvelle' pour créer une partie, 'LISTE' pour voir les parties, un ID pour rejoindre, ou 'QUIT' pour quitter.");
            writer.println("-----------------------");
        }


        private void afficherListeParties() {
            writer.println("--- Liste des parties ---");
            if (parties.isEmpty()) {
                writer.println("Aucune partie n'a encore été créée. Tapez 'nouvelle' pour en créer une.");
                return;
            }
            for (int i = 0; i < parties.size(); i++) {
                Partie p = parties.get(i);
                String etat = p.getFini() ? "terminée" : (p.isFull() ? "complète" : "en attente");
                writer.println("  -> Partie #" + i + " : " + etat);
            }

            writer.println("-------------------------");
        }
    }

    public static void main(String[] args) {
        new Server().startServer(5555);
    }
}
