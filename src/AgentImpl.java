import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.Hashtable;

public class AgentImpl implements Agent {
    private String name;
    private Node origin;
    private String jarPath;
    private Hashtable<String, Object> ns;

    public AgentImpl() {
    }

    public void init(String name, Node origin) {
        this.name = name;
        this.origin = origin;
    }

    public void setNameServer(Hashtable<String, Object> ns) {
        this.ns = ns;
    }

    public Hashtable<String, Object> getNameServer() {
        return this.ns;
    }

    public void move(Node target) throws MoveException {
        File jar = new File(this.jarPath);
        // Step A : Agent serialisation
        byte[] agentData;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(this);
            oos.flush();
            agentData = bos.toByteArray();
        } catch (IOException e) {
            throw new MoveException("Erreur de sérialisation de l'état : " + e.getMessage(), e);
        }

        // Step B : Sending of the above message to the target server
        try (
                Socket socket = new Socket(target.getAddress(), target.getPort());
                // lit le fichier JAR en bytes
                FileInputStream fis = new FileInputStream(jar);
                // pour envoyer la taille du fichier
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());) {
            // Envoi de la taille du fichier JAR
            dos.writeLong(jar.length());
            dos.flush();
            // usage d'un buffer car le code de l'agent compilé peut être volumineux
            byte[] buffer = new byte[4096];
            int bytesRead;
            // On lit le fichier .jar et on envoie son contenu au serveur
            while ((bytesRead = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }
            // 2. Envoi des données sérialisés
            dos.writeLong(agentData.length);
            dos.write(agentData);
            dos.flush();
            System.out.println("Fichier et état envoyés !");
        } catch (Exception e) {
            throw new MoveException("Erreur lors de l'envoi du fichier : " + e.getMessage(), e);
        }
    }

    public void back() throws MoveException {
        System.out.println("Retour vers l'origine : " + this.origin.getAddress() + ":" + this.origin.getPort());
        this.move(this.origin);
    }

    public void main() throws MoveException {
        // on ne fait rien, méthode main reecrite par Agent fils
    }

}
