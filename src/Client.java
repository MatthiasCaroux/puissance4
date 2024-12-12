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

            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.println("Entrez la colonne (1-7) ou QUIT pour quitter :");
                String command = scanner.nextLine().trim();
                writer.println(command);
                if ("QUIT".equalsIgnoreCase(command)) {
                    break;
                }
            }

            listener.join(); 
        } catch (IOException | InterruptedException e) {
            System.err.println("Erreur de connexion : " + e.getMessage());
        }
    }
}
