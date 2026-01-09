import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;

public class SimpleFileServer {
    public static void main(String[] args) throws IOException {
        int port = 9090; // Port différent de celui des agents
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("BASELINE SERVEUR : Écoute sur le port " + port);

        while (true) {
            try (Socket client = serverSocket.accept();
                 OutputStream os = client.getOutputStream()) {
                
                File file = new File("bigdata.log");
                if(file.exists()) {
                    byte[] bytes = Files.readAllBytes(file.toPath());
                    os.write(bytes);
                    os.flush();
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
    }
}