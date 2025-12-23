import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Queue;
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
            System.out.println("Réception d'un agent");

            // Step D : Reception of the message in the destination agent server
            File fileJar = receiveJarFile(dis);

            // Step F : Creation of a class loader associated with the incoming agent
            AgentLoader agentLoader = new AgentLoader(this.getClass().getClassLoader());
            agentLoader.loadJar(fileJar.getAbsolutePath());

            byte[] agentData = receiveAgentData(dis);

            // Step G : De-serialisation of the agent data
            AgentImpl agent = deserializeAgent(agentData, agentLoader);

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

    private File receiveJarFile(DataInputStream dis) throws IOException {
        // Lecture de la longueur du nom et du nom
        int nameLen = dis.readInt();
        byte[] nameBytes = new byte[nameLen];
        dis.readFully(nameBytes);
        String jarName = new String(nameBytes, StandardCharsets.UTF_8);

        // Lecture de la taille du fichier
        long jarSize = dis.readLong();

        File fileJar = new File(jarName);
        if (fileJar.exists()) {
            fileJar.delete();
        }

        // Écriture du contenu sur le disque
        try (FileOutputStream fos = new FileOutputStream(fileJar)) {
            byte[] buffer = new byte[4096];
            long remaining = jarSize;
            while (remaining > 0) {
                int read = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (read == -1)
                    break;
                fos.write(buffer, 0, read);
                remaining -= read;
            }
        }
        System.out.println("SERVEUR: Code agent sauvegardé -> " + fileJar.getName());
        return fileJar;
    }

    private byte[] receiveAgentData(DataInputStream dis) throws IOException {
        long dataSize = dis.readLong();
        byte[] agentData = new byte[(int) dataSize];
        dis.readFully(agentData);
        System.out.println("SERVEUR: Données de l'agent reçues (" + dataSize + " bytes).");
        return agentData;
    }

    private AgentImpl deserializeAgent(byte[] agentData, AgentLoader loader)
            throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(agentData);

        // Surcharge de resolveClass pour utiliser notre loader
        try (ObjectInputStream ois = new ObjectInputStream(bis) {
            @Override
            protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                // Utilisation de Class.forName pour gérer les tableaux et types complexes
                // comme recommandé précédemment
                try {
                    return Class.forName(desc.getName(), false, loader);
                } catch (ClassNotFoundException e) {
                    return super.resolveClass(desc);
                }
            }
        }) {
            return (AgentImpl) ois.readObject();
        }
    }

    public static void main(String[] args) {
        if (args.length == 1) {
            // cas serveur receveur
            int port = Integer.parseInt(args[0]);
            new Server(port).start();
        } else if (args.length == 2) {
            // cas serveur emetteur initial
            int port = Integer.parseInt(args[0]);
            String jarPath = args[1];

            // Définition de l'itinéraire
            Queue<Node> itinerary = new LinkedList<>();
            itinerary.add(new Node("localhost", 8081));
            itinerary.add(new Node("localhost", 8082));
            itinerary.add(new Node("localhost", port));

            // Configuration de l'Agent
            System.out.println("Création de l'agent");
            TestAgent agent = new TestAgent(itinerary, jarPath);

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