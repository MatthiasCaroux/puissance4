import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class Server {
    private final Plateau plateau = new Plateau();
    private final ReentrantLock lock = new ReentrantLock();

    // Liste thread-safe des flux de sortie des clients
    private final List<PrintWriter> clients = new CopyOnWriteArrayList<>();

    public void startServer(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Serveur lancé sur le port " + port);
            System.out.println("État initial du plateau :");
            broadcastPlateau(); // Envoyer l'état initial (facultatif, après la première connexion)

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nouvelle connexion client : " + clientSocket.getRemoteSocketAddress());
                // Création du thread client
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Envoie le plateau et un message optionnel à tous les clients
    private void broadcastPlateau() {
        lock.lock();
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("\n----- État du Plateau -----\n");
            sb.append(plateau.toString()); // Assurez-vous que Plateau.toString() renvoie l'état du plateau
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
                
                // Envoyer l'état du plateau à ce nouveau client
                writer.println("Bienvenue sur le serveur de jeu !");
                broadcastPlateau();

                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.equalsIgnoreCase("QUIT")) {
                        writer.println("Au revoir !");
                        break;
                    }

                    // On s'attend à recevoir un nombre (la colonne)
                    try {
                        int colonne = Integer.parseInt(line);
                        if (colonne < 1 || colonne > 7) {
                            writer.println("Colonne invalide. Choisissez une colonne entre 1 et 7.");
                            continue;
                        }

                        lock.lock();
                        try {
                            // Jouer le coup
                            plateau.jouer(colonne - 1);

                            // Affichage du plateau à tous
                            broadcastPlateau();

                            // Vérifier si gagné
                            if (plateau.estGagne()) {
                                plateau.changerCouleurCourante();
                                String message = "Le joueur " + plateau.getCouleurCourante() + " a gagné !";
                                broadcastMessage(message);
                                // Ici, on pourrait réinitialiser le plateau ou fermer la partie
                                break;
                            } else {
                                // Continuer le jeu
                            }

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
