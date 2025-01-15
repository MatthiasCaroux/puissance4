import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class Server {
    private Plateau plateau;
    private final ReentrantLock lock = new ReentrantLock();
    private final List<PrintWriter> clients = new CopyOnWriteArrayList<>();
    private final Map<Integer, String> playerNames = new HashMap<>();
    private int currentPlayerIndex = 0;
    private boolean gameStarted = false;

    public Server() {
        resetGame();
    }

    public void startServer(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Serveur lancé sur le port " + port);

            while (true) {
                if (clients.size() < 2) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Nouvelle connexion client : " + clientSocket.getRemoteSocketAddress());
                    new Thread(new ClientHandler(clientSocket)).start();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void broadcastPlateau() {
        lock.lock();
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("\n----- État du Plateau -----\n");
            sb.append(plateau.toString());
            sb.append("\n---------------------------\n");
            for (PrintWriter client : clients) {
                client.println(sb.toString());
            }
        } finally {
            lock.unlock();
        }
    }

    private void broadcastMessage(String message) {
        for (PrintWriter client : clients) {
            client.println(message);
        }
    }


    private void sendToClient(PrintWriter client, String message) {
        client.println(message);
    }

    private void resetGame() {
        lock.lock();
        try {
            plateau = new Plateau();
            currentPlayerIndex = 0;
            gameStarted = false;
        } finally {
            lock.unlock();
        }
    }

    private void handlePlayerQuit(int quittingPlayerIndex) {
        lock.lock();
        try {
            int otherPlayerIndex = (quittingPlayerIndex == 0) ? 1 : 0;

            // Vérifie si deux joueurs sont présents
            if (clients.size() == 2) {
                // Message pour le joueur qui a quitté
                sendToClient(clients.get(quittingPlayerIndex), "Vous avez quitté la partie. Vous avez perdu !");
                
                // Message pour l'autre joueur
                sendToClient(clients.get(otherPlayerIndex), "L'autre joueur a quitté la partie. Vous avez gagné !");
            }

            // Déconnecte tous les clients et envoie le message de fin
            disconnectAllClients("Partie terminée. Merci d'avoir joué !");
            resetGame(); // Réinitialise le jeu
        } finally {
            lock.unlock();
        }
    }

    private void disconnectAllClients(String message) {
        for (PrintWriter client : clients) {
            client.println(message);
            client.close();
        }
        clients.clear();
    }

    private class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private PrintWriter writer;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                writer = new PrintWriter(clientSocket.getOutputStream(), true);
                clients.add(writer);

                writer.println("Entrez votre nom :");
                String playerName = reader.readLine();
                int playerIndex = clients.size() - 1;

                writer.println("Bienvenue sur le serveur de jeu ! Vous êtes le joueur " + (playerIndex + 1));

                // Si deux joueurs sont connectés, démarrer la partie
                if (clients.size() == 2) {
                    gameStarted = true;
                    broadcastPlateau();
                    broadcastMessage("La partie commence ! C'est au joueur 1 de jouer.");
                    sendToClient(clients.get(0), "C'est à vous de jouer. Entrez la colonne (1-7) ou QUIT pour quitter :");
                } else {
                    writer.println("En attente d'un autre joueur...");
                }

                playerNames.put(playerIndex, playerName);

                broadcastMessage(playerName + " a rejoint la partie.");
                broadcastPlateau();


                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();

                    // Case for QUIT command
                    if (line.equalsIgnoreCase("QUIT")) {

                        handlePlayerQuit(playerIndex);

                        writer.println("Au revoir, " + playerName + "!");

                        break;
                    }

                    if (!gameStarted) {
                        writer.println("Attendez que le deuxième joueur se connecte.");
                        continue;
                    }

                    if (playerIndex != currentPlayerIndex) {

                        writer.println("Ce n'est pas votre tour. Attendez le joueur " + (currentPlayerIndex + 1) + ".");

                        writer.println("Ce n'est pas votre tour. Attendez le joueur " +
                                       playerNames.get(currentPlayerIndex) + ".");

                        continue;
                    }

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

                            broadcastPlateau();

                            if (plateau.estGagne()) {

                                for (int i = 0; i < clients.size(); i++) {
                                    if (i == currentPlayerIndex) {
                                        sendToClient(clients.get(i), "Félicitations ! Vous avez gagné !");
                                    } else {
                                        sendToClient(clients.get(i), "Dommage, vous avez perdu.");
                                    }
                                }

                                disconnectAllClients("Partie terminée. Merci d'avoir joué !");
                                resetGame();

                                String message = playerName + " a gagné !";
                                broadcastMessage(message);

                                break;
                            }

                            currentPlayerIndex = (currentPlayerIndex + 1) % clients.size();

                            broadcastMessage("C'est au joueur " + (currentPlayerIndex + 1) + " de jouer.");
                            sendToClient(clients.get(currentPlayerIndex), "C'est à vous de jouer. Entrez la colonne (1-7) ou QUIT pour quitter :");

                            broadcastMessage("C'est au joueur " + playerNames.get(currentPlayerIndex) + " de jouer.");

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

                // In case of sudden disconnect (Ctrl+C)
                if (!clientSocket.isClosed()) {
                    clients.remove(writer);
                }


                clients.remove(writer);
                playerNames.remove(currentPlayerIndex);

                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        new Server().startServer(5555);
    }
}
