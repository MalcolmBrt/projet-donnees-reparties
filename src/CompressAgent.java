import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;

public class CompressAgent extends AgentImpl {
    private String fileName; // Le nom du fichier à chercher
    private byte[] dataCompressed; // Le résultat compressé
    private long originalSize = 0;

    // Constructeur appelé par la réflexion du Server.java
    public CompressAgent(Queue<Node> itinerary, String jarPath) {
        super(itinerary, jarPath);
        this.fileName = "bigdata.log";
    }

    @Override
    public void main() throws MoveException {
        Hashtable<String, Object> services = getServices();

        if (services.containsKey("ServiceFile")) {
            System.out.println("AGENT : Arrivé au Service Fichier. Je demande : " + fileName);
            ServiceFile service = (ServiceFile) services.get("ServiceFile");

            try {
                // 1. Récupération locale (Gros volume de données, mais en mémoire RAM locale)
                byte[] rawData = service.getContent(fileName);
                this.originalSize = rawData.length;

                if (originalSize == 0) {
                    System.out.println("AGENT : Fichier vide ou introuvable.");
                } else {
                    // 2. Compression locale (CPU du serveur)
                    System.out.println("AGENT : Compression en cours...");

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
                        gzip.write(rawData);
                    }
                    this.dataCompressed = baos.toByteArray();

                    System.out.println("AGENT : Terminé !");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            if (dataCompressed != null) {
                System.out.println("--- MISSION ACCOMPLIE ---");
                System.out.println("AGENT : Taille originale : " + originalSize);
                System.out.println("AGENT : Taille finale : " + dataCompressed.length);

                // Sauvegarder le résultat sur le disque du client
                try (FileOutputStream fos = new FileOutputStream("resultat_" + fileName + ".gz")) {
                    fos.write(dataCompressed);
                    System.out.println("Fichier sauvegardé sous : resultat_" + fileName + ".gz");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("AGENT : Je suis rentré les mains vides.");
            }
        }

        // Migration suivante
        if (!getItinerary().isEmpty()) {
            Node nextHop = getItinerary().poll();
            move(nextHop);
        }
    }
}