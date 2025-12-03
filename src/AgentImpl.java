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
        File jar = new File("file.jar");
        try (
                Socket socket = new Socket(target.getAddress(), target.getPort());
                // lit le fichier JAR en bytes
                FileInputStream fis = new FileInputStream(jar);
                // envoie les bytes au serveur avec un buffer
                BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
                // pour envoyer la taille du fichier
                DataOutputStream dos = new DataOutputStream(out);
            ) {
            dos.writeLong(jar.length());
            dos.flush();

            byte[] buffer = new byte[4096];
            int bytesRead;

            // On lit le fichier .jar et on envoie son contenu au serveur
            while ((bytesRead = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }

            // On vide le buffer de sortie pour que tout soit envoyé
            dos.flush();

            System.out.println("Fichier envoyé !");

        } catch (Exception e) {
            throw new MoveException("Erreur lors de l'envoi du fichier : " + e.getMessage(), e);
        }

    }

    public void back() throws MoveException {

    }

    public void main() throws MoveException {

    }

}
