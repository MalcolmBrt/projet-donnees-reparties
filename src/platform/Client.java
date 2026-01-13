package platform;

import java.lang.reflect.Constructor;
import java.util.LinkedList;
import java.util.Queue;

import common.AgentImpl;
import common.Node;

public class Client {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("USAGE CLIENT : java Client <LocalIP:PORT> <DestIP:PORT> ... <Agent.jar>");
            return;
        }

        String localAddressArg = args[0];
        String lastArg = args[args.length - 1];

        if (!localAddressArg.contains(":")) {
            System.err.println("Erreur : L'adresse locale doit être au format IP:PORT");
            return;
        }
        
        String myIp = localAddressArg.split(":")[0];
        int myPort = Integer.parseInt(localAddressArg.split(":")[1]);

        if (!lastArg.endsWith(".jar")) {
            System.err.println("Erreur : Le dernier argument doit être le fichier .jar de l'agent.");
            return;
        }

        try {
            String jarPath = lastArg;
            Queue<Node> itinerary = new LinkedList<>();
            // On commence à i=1 (destinations) et on s'arrête avant le dernier arg (le jar)
            for (int i = 1; i < args.length - 1; i++) {
                String dest = args[i];
                String[] parts = dest.split(":");
                if (parts.length != 2) {
                    System.err.println("Erreur format destination (attendu IP:PORT) : " + dest);
                    return;
                }
                itinerary.add(new Node(parts[0], Integer.parseInt(parts[1])));
            }

            // Ajout du retour à la maison (ce client)
            System.out.println("CLIENT : Mon adresse de retour est " + myIp + ":" + myPort);
            itinerary.add(new Node(myIp, myPort));

            System.out.println("CLIENT : Création de l'agent depuis " + jarPath);
            AgentImpl agent;
            
            String className = "agents." + jarPath.replace(".jar", "");
            
            // Création du classloader pour charger le jar localement une première fois
            AgentLoader agentLoader = new AgentLoader(Client.class.getClassLoader());
            agentLoader.loadJar(jarPath);
            
            Class<?> clazz = Class.forName(className, true, agentLoader);
            Constructor<?> ctor = clazz.getConstructor(Queue.class, String.class);
            agent = (AgentImpl) ctor.newInstance(itinerary, jarPath);

            // Départ de l'agent
            Node firstDestination = itinerary.poll();
            System.out.println("CLIENT : Envoi de l'agent vers " + firstDestination.getAddress() + ":" + firstDestination.getPort());
            long start = System.currentTimeMillis();
            agent.move(firstDestination);

            // Le client devient Serveur pour attendre le retour de l'agent
            System.out.println("CLIENT : Je passe en mode écoute pour le retour...");

            new Server(myPort, start).start();

        } catch (Exception e) {
            System.err.println("CLIENT : Erreur critique");
            e.printStackTrace();
        }
    }
}