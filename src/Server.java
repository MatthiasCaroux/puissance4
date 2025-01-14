import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class Server {
    private final Plateau plateau = new Plateau();
    private final List<Partie> parties = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final List<PrintWriter> clients = new CopyOnWriteArrayList<>();
    private int currentPlayerIndex = 0;

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

    private void afficherPartiesEnCours(PrintWriter writer) {
        lock.lock();
        try {
            writer.println("\n--- Parties en cours ---");
            if (parties.isEmpty()) {
                writer.println("Aucune partie en cours.");
            } else {
                for (Partie partie : parties) {
                    writer.println("ID: " + partie.getId() + " | Joueurs: " + partie.getJoueurs().size() + "/2");
                }
            }
            writer.println("------------------------\n");
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
        private BufferedReader reader;
        private Partie partie;
        private int currentPlayerIndex = 0;
    
        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }
    
        @Override
        public void run() {
            try {
                // Initialisation des flux de communication
                reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                writer = new PrintWriter(clientSocket.getOutputStream(), true);
    
                // Afficher les parties en cours et demander au client de choisir ou de créer une nouvelle partie
                afficherPartiesEnCours(writer);
                writer.println("Entrez l'ID d'une partie à rejoindre ou 'NOUVELLE' pour créer une nouvelle partie :");
                String choix = reader.readLine().trim();
    
                if ("NOUVELLE".equalsIgnoreCase(choix)) {
                    partie = new Partie(parties.size() + 1);
                    parties.add(partie);
                    writer.println("Nouvelle partie créée avec l'ID: " + partie.getId());
                } else {
                    try {
                        int idPartie = Integer.parseInt(choix);
                        partie = parties.stream().filter(p -> p.getId() == idPartie && !p.estPleine()).findFirst().orElse(null);
    
                        if (partie == null) {
                            writer.println("Cette partie est soit pleine, soit inexistante.");
                            afficherPartiesEnCours(writer);
                            return;
                        }
                    } catch (NumberFormatException e) {
                        writer.println("ID de partie invalide.");
                        afficherPartiesEnCours(writer);
                        return;
                    }
                }
    
                // Ajouter le joueur à la partie et afficher un message
                partie.ajouterJoueur(writer);
                writer.println("Vous êtes maintenant dans la partie " + partie.getId() + ".");
    
                // Lancer la partie, en alternant les tours entre les joueurs
                while (partie.estEnCours()) {
                    // Vérifier si c'est le tour du joueur actuel
                    if (partie.getJoueurs().get(currentPlayerIndex) == writer) {
                        writer.println("Début de la partie. C'est à vous de jouer.");
                        broadcastPlateau(partie);
                        writer.println("Entrez la colonne (1-7) ou 'QUIT' pour quitter :");
                        String choixTour = reader.readLine().trim();
    
                        if ("QUIT".equalsIgnoreCase(choixTour)) {
                            partie.terminerPartie();
                            break;
                        }
    
                        try {
                            int colonne = Integer.parseInt(choixTour);
                            if (colonne < 1 || colonne > 7) {
                                writer.println("Colonne invalide.");
                                continue;
                            }
    
                            lock.lock();
                            try {
                                if (!partie.getPlateau().jouer(colonne - 1)) {
                                    writer.println("Colonne pleine, choisissez une autre colonne.");
                                    continue;
                                }
    
                                // Afficher le plateau
                                broadcastPlateau(partie);
    
                                if (partie.getPlateau().estGagne()) {
                                    writer.println("Félicitations! Vous avez gagné.");
                                    broadcastMessage(partie, "Le joueur " + (currentPlayerIndex + 1) + " a gagné!");
                                    partie.terminerPartie();
                                    break;
                                }
    
                                // Changer de tour
                                currentPlayerIndex = (currentPlayerIndex + 1) % partie.getJoueurs().size();
                                broadcastMessage(partie, "C'est au joueur " + (currentPlayerIndex + 1) + " de jouer.");
    
                            } finally {
                                lock.unlock();
                            }
    
                        } catch (NumberFormatException e) {
                            writer.println("Commande invalide.");
                        }
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
    
    private void broadcastPlateau(Partie partie) {
        lock.lock();  
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("\n----- État du Plateau -----\n");
            sb.append(partie.getPlateau().toString());
            sb.append("\n---------------------------\n");
    
            
            for (PrintWriter joueur : partie.getJoueurs()) {
                joueur.println(sb.toString());
            }
        } finally {
            lock.unlock();  
        }
    }
    

    private void broadcastMessage(Partie partie, String message) {
        for (PrintWriter joueur : partie.getJoueurs()) {
            joueur.println(message);
        }
    }

    public static void main(String[] args) {
        new Server().startServer(5555);
    }
}
