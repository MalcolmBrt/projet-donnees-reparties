import java.io.*;
import java.lang.reflect.Constructor;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Queue;

public class Server {
    private int port;
    private Hashtable<String, Object> services = new Hashtable<>();
    
    // Chronomètre global
    public static long startTime = 0;

    public Server(int port) {
        this.port = port;
    }

    public Server(int port, String serviceKey, Object service) {
        this.port = port;
        this.services.put(serviceKey, service);
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("SERVEUR : Démarré sur le port " + port);

            while (true) {
                Socket client = serverSocket.accept();
                new Thread(() -> handleConnection(client)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleConnection(Socket socket) {
        try (DataInputStream dis = new DataInputStream(socket.getInputStream())) {
            
            // 1. Réception du JAR
            File fileJar = receiveJarFile(dis);
            
            // 2. Chargement de la classe
            AgentLoader agentLoader = new AgentLoader(this.getClass().getClassLoader());
            agentLoader.loadJar(fileJar.getAbsolutePath());

            // 3. Réception et Désérialisation de l'agent
            byte[] agentData = receiveAgentData(dis);
            AgentImpl agent = deserializeAgent(agentData, agentLoader);

            // 4. Configuration de l'agent sur ce serveur
            agent.setJarPath(fileJar.getAbsolutePath());
            agent.setServices(services);

            // 5. Exécution
            System.out.println("SERVEUR : Exécution de l'agent...");
            agent.main();

            // 6. FIN DU TEST (Si l'agent est revenu au point de départ)
            if (agent.getItinerary().isEmpty()) {
                long endTime = System.currentTimeMillis();
                if (startTime != 0) { // Si c'est nous qui avons lancé le chrono (Client)
                    long duration = endTime - startTime;
                    System.out.println("\n==================================================");
                    System.out.println(" RÉSULTAT AGENT MOBILE");
                    System.out.println(" Temps Total (Aller-Retour + Compression) : " + duration + " ms");
                    System.out.println("==================================================\n");
                    System.exit(0); // Arrêt propre du programme
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private File receiveJarFile(DataInputStream dis) throws IOException {
        int nameLen = dis.readInt();
        byte[] nameBytes = new byte[nameLen];
        dis.readFully(nameBytes);
        String jarName = new String(nameBytes, StandardCharsets.UTF_8);
        long jarSize = dis.readLong();
        
        File fileJar = new File(jarName);
        if (fileJar.exists()) fileJar.delete();
        
        try (FileOutputStream fos = new FileOutputStream(fileJar)) {
            byte[] buffer = new byte[4096];
            long remaining = jarSize;
            while (remaining > 0) {
                int read = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (read == -1) break;
                fos.write(buffer, 0, read);
                remaining -= read;
            }
        }
        return fileJar;
    }

    private byte[] receiveAgentData(DataInputStream dis) throws IOException {
        long dataSize = dis.readLong();
        byte[] agentData = new byte[(int) dataSize];
        dis.readFully(agentData);
        return agentData;
    }

    private AgentImpl deserializeAgent(byte[] agentData, AgentLoader loader) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(agentData);
        try (ObjectInputStream ois = new ObjectInputStream(bis) {
            @Override
            protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
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
        if (args.length < 2) {
            System.out.println("Usage: java Server <IP:PORT> <Service|Dest...>");
            return;
        }

        String localAddressArg = args[0];
        String myIp = localAddressArg.split(":")[0];
        int myPort = Integer.parseInt(localAddressArg.split(":")[1]);
        String lastArg = args[args.length - 1];

        // --- MODE CLIENT (Lanceur d'agent) ---
        if (lastArg.endsWith(".jar")) {
            try {
                String jarPath = lastArg;
                Queue<Node> itinerary = new LinkedList<>();
                for (int i = 1; i < args.length - 1; i++) {
                    String[] parts = args[i].split(":");
                    itinerary.add(new Node(parts[0], Integer.parseInt(parts[1])));
                }
                // Ajouter le retour à la maison à la fin de l'itinéraire
                itinerary.add(new Node(myIp, myPort));

                // Chargement dynamique de l'agent pour le lancer
                AgentLoader agentLoader = new AgentLoader(Server.class.getClassLoader());
                agentLoader.loadJar(jarPath);
                String className = jarPath.replace(".jar", "");
                Class<?> clazz = Class.forName(className, true, agentLoader);
                Constructor<?> ctor = clazz.getConstructor(Queue.class, String.class);
                AgentImpl agent = (AgentImpl) ctor.newInstance(itinerary, jarPath);

                Node firstDestination = itinerary.poll();
                if (firstDestination != null) {
                    System.out.println("CLIENT : Démarrage du Chronomètre.");
                    System.out.println("CLIENT : Envoi de l'agent vers " + firstDestination.getPort());
                    
                    // START CHRONO
                    startTime = System.currentTimeMillis();
                    
                    agent.move(firstDestination);
                }

                // Le client devient serveur pour attendre le retour
                new Server(myPort).start();
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        } 
        // --- MODE SERVEUR DE SERVICES ---
        else {
            String serviceType = args[1];
            Object service = null;
            
            // Instanciation des services (versions "flat")
            if (serviceType.equals("ServiceGuide")) service = new ServiceGuideImpl();
            else if (serviceType.equals("ServiceTarif")) service = new ServiceTarifImpl();
            else if (serviceType.equals("ServiceFile")) service = new ServiceFileImpl();

            if (service != null) {
                new Server(myPort, serviceType, service).start();
            } else {
                new Server(myPort).start();
            }
        }
    }
}