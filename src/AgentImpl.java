import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Queue;

public class AgentImpl implements Agent {
    private Queue<Node> itinerary;
    private String jarPath;
    private Hashtable<String, Object> ns;

    public AgentImpl(Queue<Node> itinerary) {
        this.itinerary = itinerary;
    }
    
    public Queue<Node> getItinerary() {
        return itinerary;
    }

    public void setItinerary(Queue<Node> itinerary) {
        this.itinerary = itinerary;
    }

    public String getJarPath() {
        return jarPath;
    }

    public void setJarPath(String jarPath) {
        this.jarPath = jarPath;
    }

    public Hashtable<String, Object> getNameServer() {
        return ns;
    }

    public void setNameServer(Hashtable<String, Object> ns) {
        this.ns = ns;
    }

    public void move(Node target) throws MoveException {
        File jarFile = new File(this.jarPath);
        // Step A : Serialisation of the agent data
        byte[] agentData;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(this);
            oos.flush();
            agentData = bos.toByteArray();
        } catch (IOException e) {
            throw new MoveException("Erreur de sérialisation de l'état : " + e.getMessage(), e);
        }
        // Step A : Serialisation of the agent code
        try (
                Socket socket = new Socket(target.getAddress(), target.getPort());
                // lit le fichier JAR en bytes
                FileInputStream fis = new FileInputStream(jarFile);
                // pour envoyer la taille du fichier
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());) {
            
            // Step B : Sending of the above message to the target server
            // Envoi du nom du fichier JAR (jarPath) afin que le récepteur sache comment le nommer
            byte[] nameBytes = jarFile.getName().getBytes(StandardCharsets.UTF_8);
            dos.writeInt(nameBytes.length);
            dos.write(nameBytes);
            dos.flush();

            // Envoi de la taille du fichier JAR
            dos.writeLong(jarFile.length());
            dos.flush();
            // usage d'un buffer car le code de l'agent compilé peut être volumineux
            byte[] buffer = new byte[4096];
            int bytesRead;
            // On lit le fichier .jar et on envoie son contenu au serveur
            while ((bytesRead = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }
            // Envoi des données sérialisés
            dos.writeLong(agentData.length);
            dos.write(agentData);
            dos.flush();
            System.out.println("Fichier et état envoyés !");
        } catch (Exception e) {
            throw new MoveException("Erreur lors de l'envoi du fichier : " + e.getMessage(), e);
        }
    }

    public void main() throws MoveException {
        // on ne fait rien, méthode main reecrite par Agent fils
    }

}
