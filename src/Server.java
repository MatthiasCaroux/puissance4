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
    private int currentPlayerIndex = 0;

    public void startServer(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Serveur lancé sur le port " + port);
            System.out.println("État initial du plateau :");
            broadcastPlateau();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nouvelle connexion client : " + clientSocket.getRemoteSocketAddress());
                new Thread(new ClientHandler(clientSocket)).start();
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

                int playerIndex = clients.size() - 1;
                writer.println("Bienvenue sur le serveur de jeu ! Vous êtes le joueur " + (playerIndex + 1));
                broadcastPlateau();

                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.equalsIgnoreCase("QUIT")) {
                        writer.println("Au revoir !");
                        break;
                    }

                    if (playerIndex != currentPlayerIndex) {
                        sendToClient(writer, line);
                        writer.println("Ce n'est pas votre tour. Attendez le joueur " + (currentPlayerIndex + 1) + ".");
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
                                String message = "Le joueur " + (currentPlayerIndex + 1) + " a gagné !";
                                broadcastMessage(message);
                                break;
                            }

                            currentPlayerIndex = (currentPlayerIndex + 1) % clients.size();
                            broadcastMessage("C'est au joueur " + (currentPlayerIndex + 1) + " de jouer.");
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
                clients.remove(writer);
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
