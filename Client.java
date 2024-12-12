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

            System.out.println("Connexion réussie au serveur : " + socket.getRemoteSocketAddress());
            
            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.print("Entrez une commande : ");
                String command = scanner.nextLine().trim();

                writer.println(command);
                String response = reader.readLine();
                System.out.println(response);

                if (command.equalsIgnoreCase("QUIT")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur de connexion au serveur : " + e.getMessage());
        }
    }
}
