import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

public class Server {
    private int port;

    public Server(int port) {
        this.port = port;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Serveur démarré sur le port " + port);

            while (true) {
                Socket client = serverSocket.accept();
                // Step E : Creation of a new thread for the execution of the agent
                new Thread(() -> handleConnection(client)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleConnection(Socket socket) {
        try (DataInputStream dis = new DataInputStream(socket.getInputStream())) {
            // Step D : Reception of the message in the destination agent server
            System.out.println("Réception d'un agent");
            // lecture du nom du JAR envoyé par le client (longueur + UTF-8)
            int nameLen = dis.readInt();
            byte[] nameBytes = new byte[nameLen];
            dis.readFully(nameBytes);
            String originalJarName = new String(nameBytes, StandardCharsets.UTF_8);
            // lecture de la taille et du contenu du JAR
            long jarSize = dis.readLong();
            File fileJar = new File(originalJarName);
            try (FileOutputStream fos = new FileOutputStream(fileJar)) {
                byte[] buffer = new byte[4096];
                long count = jarSize;
                while (count > 0) {
                    // on met dans le buffer les 4096 premiers octets reçus
                    // ou le nombre d'octet restant si plus petit
                    int read = dis.read(buffer, 0, (int) Math.min(buffer.length, count));
                    // dis.read() retourne -1 quand fin de flux, on sort donc de la boucle
                    if (read == -1)
                        break;
                    fos.write(buffer, 0, read);
                    count -= read;
                }
            }
            System.out.println("Code agent sauvegardé : " + fileJar.getName());
            // lecture des données sérialisées
            long dataSize = dis.readLong();
            byte[] agentData = new byte[(int) dataSize];
            dis.readFully(agentData);

            // Step G : De-serialisation of the agent data
            ByteArrayInputStream bis = new ByteArrayInputStream(agentData);
            ObjectInputStream ois = new ObjectInputStream(bis);
            // pas de class loader custom pour l'instant
            AgentImpl agent = (AgentImpl) ois.readObject();

            // indique à l'agent où se trouve son fichier JAR sur ce serveur
            // pour qu'il puisse le lire s'il veut faire un move() vers ailleurs
            agent.setJarPath(fileJar.getAbsolutePath());

            // Injection service (vide)
            agent.setNameServer(new Hashtable<>());

            // Step H : Restart of the agent
            System.out.println("Lancement de l'agent");
            agent.main();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length == 1) {
            int port = Integer.parseInt(args[0]);
            new Server(port).start();
        } else if (args.length == 2) {
            int port = Integer.parseInt(args[0]);
            String clientJarPath = args[1];

            // Définition de l'itinéraire
            Queue<Node> itinerary = new LinkedList<>();
            itinerary.add(new Node("localhost", 8081));
            itinerary.add(new Node("localhost", 8082));
            itinerary.add(new Node("localhost", port));

            // Configuration de l'Agent
            System.out.println("Création de l'agent");
            TestAgent agent = new TestAgent(itinerary);
            agent.setJarPath(clientJarPath);

            // envoi initial
            Node firstDestination = itinerary.poll();
            System.out.println("CLIENT: Migration vers serveur" + firstDestination.getPort());
            try {
                agent.move(firstDestination);
                System.out.println("CLIENT: Agent envoyé avec succès. Le programme continue d'écouter le retour.");
            } catch (Exception e) {
                e.printStackTrace();
            }
            new Server(port).start();

        } else {
            System.out.println("Usage: java Server <port> <agent>");
        }

    }
}