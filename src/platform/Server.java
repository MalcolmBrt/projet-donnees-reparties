import java.io.*;
import java.lang.reflect.Constructor;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.nio.charset.StandardCharsets;

public class Server {
    private int port;
    private Hashtable<String, Object> services = new Hashtable<>();

    public Server(int port) {
        this.port = port;
    }

    public Server(int port, String serviceKey, Object service) {
        this.port = port;
        this.services.put(serviceKey, service);
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("SERVEUR : Serveur démarré sur le port " + port);

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
            System.out.println("SERVEUR : Réception d'un agent");

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

            agent.setNameServer(services);

            // Step H : Restart of the agent
            System.out.println("SERVEUR : Lancement de l'agent");
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
        System.out.println("SERVEUR : Code agent sauvegardé -> " + fileJar.getName());
        return fileJar;
    }

    private byte[] receiveAgentData(DataInputStream dis) throws IOException {
        long dataSize = dis.readLong();
        byte[] agentData = new byte[(int) dataSize];
        dis.readFully(agentData);
        System.out.println("SERVEUR : Données de l'agent reçues (" + dataSize + " bytes).");
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
        // cas serveur initial (client)
        List<String> agentsAccepted = List.of("TestAgent.jar", "GourmetAgent.jar", "CompressAgent.jar");
        if (args.length == 2 && agentsAccepted.contains(args[1])) {
            int port = Integer.parseInt(args[0]);
            String jarPath = args[1];

            // Définition de l'itinéraire
            Queue<Node> itinerary = new LinkedList<>();
            itinerary.add(new Node("localhost", 8081));
            if (!jarPath.equals("CompressAgent.jar")) {
                itinerary.add(new Node("localhost", 8082));
            }
            itinerary.add(new Node("localhost", port));

            // Configuration de l'Agent
            System.out.println("CLIENT : Création de l'agent");
            AgentImpl agent;
            try {
                String className = jarPath.replace(".jar", "");
                // Création du classloader agent
                AgentLoader agentLoader = new AgentLoader(Server.class.getClassLoader());
                agentLoader.loadJar(jarPath);
                // Chargement de la classe via le loader
                Class<?> clazz = Class.forName(className, true, agentLoader);
                Constructor<?> ctor = clazz.getConstructor(Queue.class, String.class);
                agent = (AgentImpl) ctor.newInstance(itinerary, jarPath);
            } catch (Exception e) {
                System.err.println("CLIENT : Impossible de charger l'agent " + jarPath);
                e.printStackTrace();
                return;
            }

            // envoi initial
            Node firstDestination = itinerary.poll();
            System.out.println("CLIENT : Migration vers serveur " + firstDestination.getPort());
            try {
                agent.move(firstDestination);
            } catch (Exception e) {
                e.printStackTrace();
            }
            new Server(port).start();

            // cas serveur receveur
        } else if (args.length == 2) {
            int port = Integer.parseInt(args[0]);
            String serviceType = args[1];
            switch (serviceType) {
                case "ServiceGuide":
                    new Server(port, "ServiceGuide", new ServiceGuideImpl()).start();
                    break;
                case "ServiceTarif":
                    new Server(port, "ServiceTarif", new ServiceTarifImpl()).start();
                    break;
                case "ServiceFile":
                    new Server(port, "ServiceFile", new ServiceFileImpl()).start();
                    break;
                default:
                    System.out.println("Type de service inconnu, démarrage serveur vide.");
                    new Server(port).start();
            }

        } else {
            System.out.println("Usage: java Server <port> <agent.jar|ServiceGuide|ServiceTarif>");
        }
    }
}