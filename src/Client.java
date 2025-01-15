import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        String host = "localhost"; 
        int port = 5555;

        try (Socket socket = new Socket(host, port);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println("Connecté au serveur : " + socket.getRemoteSocketAddress());

            // On crée un thread qui écoute tout ce qui vient du serveur
            Thread listener = new Thread(() -> {
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (IOException e) {
                    System.err.println("Erreur de lecture du serveur : " + e.getMessage());
                }
            });
            listener.start();

            // On lit la console locale pour envoyer les commandes (le nom et/ou les coups)
            Scanner scanner = new Scanner(System.in);
            while (true) {
                // Tapez votre nom (le serveur aura envoyé "Entrez votre nom :"),
                // puis tapez un chiffre (1-7) pour la colonne ou "QUIT"
                String command = scanner.nextLine().trim();
                writer.println(command);
                if ("QUIT".equalsIgnoreCase(command)) {
                    break;
                }
            }

            // On ferme le scanner local et on attend que le thread listener finisse
            scanner.close();
            listener.join();

        } catch (IOException | InterruptedException e) {
            System.err.println("Erreur de connexion : " + e.getMessage());
        }
    }
}
