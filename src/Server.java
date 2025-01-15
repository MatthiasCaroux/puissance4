import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private final List<Partie> parties = new ArrayList<>();
    private final List<PrintWriter> clients = new CopyOnWriteArrayList<>();

    public void startServer(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Serveur lancé sur le port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nouvelle connexion client : " + clientSocket.getRemoteSocketAddress());
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
                writer = new PrintWriter(clientSocket.getOutputStream(), true);
                clients.add(writer);

                writer.println("Bienvenue sur le serveur de jeu !");
                writer.println("Entrez 'nouvelle' pour créer une partie, un ID pour rejoindre une partie, ou 'QUIT' pour quitter.");

                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();

                    if (line.equalsIgnoreCase("QUIT")) {
                        writer.println("Au revoir !");
                        break;
                    }

                    if (line.equalsIgnoreCase("nouvelle")) {
                        currentPartie = new Partie(writer, parties.size());
                        parties.add(currentPartie);
                        writer.println("Nouvelle partie créée avec l'ID " + (parties.size() - 1));
                        break;
                    } else if (line.matches("[0-9]+")) {
                        int partieId = Integer.parseInt(line);
                        if (partieId >= 0 && partieId < parties.size()) {
                            currentPartie = parties.get(partieId);
                            currentPartie.addJoueur(writer);
                            if (currentPartie.isFull()) {
                                writer.println("Vous avez rejoint la partie " + partieId + ".");
                            }
                            break;
                        } else {
                            writer.println("ID de partie invalide.");
                        }
                    } else {
                        writer.println("Commande invalide. Entrez 'nouvelle' ou un ID pour rejoindre une partie.");
                    }
                }

                while ((line = reader.readLine()) != null) {
                    if (line.equalsIgnoreCase("QUIT")) {
                        writer.println("Au revoir !");
                        break;
                    }

                    if (currentPartie != null) {
                        currentPartie.handleMove(writer, line);
                    } else {
                        writer.println("Vous devez rejoindre ou créer une partie d'abord.");
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
