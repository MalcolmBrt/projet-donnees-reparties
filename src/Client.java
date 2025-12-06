import java.io.FileOutputStream;
import java.util.LinkedList;
import java.util.Queue;

public class Client {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java Client <port>");
            System.out.println("Exemple: java Client 8080");
            return;
        }
        int clientListenPort;
        try {
            clientListenPort = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Erreur: Le port doit être un nombre.");
            return;
        }

        try {
            System.out.println("CLIENT : Démarré");
            // on créer un thread pour le serveur d'écoute du client
            new Thread(() -> new Server(clientListenPort).start()).start();

            // On laisse le serveur démarrer
            Thread.sleep(500);

            // fake jar file pour l'instant
            String clientJarPath = "client_agent_code.jar";
            try (FileOutputStream fos = new FileOutputStream(clientJarPath)) {
                fos.write("CODE_BINAIRE_SIMULE".getBytes());
            }

            // Définition de l'itinéraire
            Queue<Node> itinerary = new LinkedList<>();
            itinerary.add(new Node("localhost", 8081));
            itinerary.add(new Node("localhost", 8082));
            itinerary.add(new Node("localhost", clientListenPort));

            // Configuration de l'Agent
            System.out.println("Création de l'agent");
            TestAgent agent = new TestAgent(itinerary);
            agent.setJarPath(clientJarPath);

            // envoi initial
            Node firstDestination = itinerary.poll();
            System.out.println("CLIENT: Migration vers serveur" + firstDestination.getPort());
            agent.move(firstDestination);

            System.out.println("CLIENT: Agent envoyé avec succès. Le programme continue d'écouter le retour.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}