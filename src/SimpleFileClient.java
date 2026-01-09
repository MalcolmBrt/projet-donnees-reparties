import java.io.InputStream;
import java.net.Socket;

public class SimpleFileClient {
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        try (Socket socket = new Socket("127.0.0.1", 9090);
             InputStream is = socket.getInputStream()) {
            
            byte[] buffer = new byte[4096];
            int totalRead = 0;
            while (is.read(buffer) != -1) {
                totalRead += buffer.length; // On lit juste, on ne stocke pas pour ne pas fausser la RAM
            }
            long end = System.currentTimeMillis();
            System.out.println("BASELINE TIME : " + (end - start) + " ms (Taille reçue : " + totalRead + " bytes)");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}