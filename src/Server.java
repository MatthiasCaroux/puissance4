import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class Server {
    private final Plateau plateau = new Plateau();
    private final ReentrantLock lock = new ReentrantLock();
    private final List<PrintWriter> clients = new CopyOnWriteArrayList<>();
    private final Map<Integer, String> playerNames = new HashMap<>();
    
    // Index du joueur (dans la liste clients) qui doit jouer
    private int currentPlayerIndex = 0;

    public void startServer(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Serveur lancé sur le port " + port);
            System.out.println("État initial du plateau :\n" + plateau);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nouvelle connexion client : " + clientSocket.getRemoteSocketAddress());
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Diffuse le plateau à tous les clients
    private void broadcastPlateau() {
        lock.lock();
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("\n----- État du Plateau -----\n");
            sb.append(plateau.toString());
            sb.append("---------------------------\n");
            for (PrintWriter client : clients) {
                client.println(sb.toString());
            }
        } finally {
            lock.unlock();
        }
    }

    // Diffuse un message simple
    private void broadcastMessage(String message) {
        for (PrintWriter client : clients) {
            client.println(message);
        }
    }

    private class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private PrintWriter writer;
        private int playerIndex = -1;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (BufferedReader reader = 
                     new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                
                // On prépare le writer pour envoyer des infos au client
                writer = new PrintWriter(clientSocket.getOutputStream(), true);
                
                // On ajoute ce writer à la liste (la taille de la liste => nouvel index)
                clients.add(writer);
                playerIndex = clients.size() - 1;

                // On demande le nom côté serveur (une seule fois)
                writer.println("Entrez votre nom :");
                String playerName = reader.readLine();
                if (playerName == null || playerName.isEmpty()) {
                    playerName = "Joueur" + playerIndex;
                }
                playerNames.put(playerIndex, playerName);

                // On informe tout le monde qu'un joueur est arrivé
                broadcastMessage(playerName + " a rejoint la partie.");
                // On affiche le plateau
                broadcastPlateau();

                // Si c'est le premier joueur et que la partie n'a pas encore commencé,
                // on peut annoncer que c'est à lui de jouer
                if (playerIndex == currentPlayerIndex) {
                    broadcastMessage("C'est à " + playerName + " de jouer.");
                }

                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.equalsIgnoreCase("QUIT")) {
                        writer.println("Au revoir, " + playerName + "!");
                        break;
                    }

                    // Vérifier si c'est bien le joueur dont c'est le tour
                    if (playerIndex != currentPlayerIndex) {
                        writer.println("Ce n'est pas votre tour. Attendez le joueur " 
                                       + playerNames.get(currentPlayerIndex) + ".");
                        continue;
                    }

                    // Essayer de jouer (colonne)
                    try {
                        int colonne = Integer.parseInt(line);
                        if (colonne < 1 || colonne > 7) {
                            writer.println("Colonne invalide. Choisissez une colonne entre 1 et 7.");
                            continue;
                        }

                        lock.lock();
                        try {
                            if (!plateau.jouer(colonne - 1)) {
                                writer.println("Cette colonne est pleine. Choisissez une autre colonne.");
                                continue;
                            }

                            // Le coup a été joué, on diffuse le nouveau plateau
                            broadcastPlateau();

                            // Vérifier s'il y a un gagnant
                            if (plateau.estGagne()) {
                                String message = playerName + " a gagné !";
                                broadcastMessage(message);
                                break; // On quitte la boucle => le client se déconnecte après la victoire
                            }

                            // Personne n'a gagné, on passe au joueur suivant
                            currentPlayerIndex = (currentPlayerIndex + 1) % clients.size();
                            broadcastMessage("C'est au joueur " 
                                             + playerNames.get(currentPlayerIndex) + " de jouer.");
                        } finally {
                            lock.unlock();
                        }
                    } catch (NumberFormatException e) {
                        writer.println("Commande invalide. Entrez un nombre (1-7) ou QUIT.");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                // Retrait du client
                clients.remove(writer);
                playerNames.remove(playerIndex); // on supprime le nom du joueur qui part
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // On peut aussi informer les autres joueurs
                broadcastMessage("Un joueur s'est déconnecté.");
            }
        }
    }

    public static void main(String[] args) {
        new Server().startServer(5555);
    }
}
