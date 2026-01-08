package services.files;

import java.io.File;
import java.nio.file.Files;

public class ServiceFileImpl implements ServiceFile {
    @Override
    public byte[] getContent(String fileName) throws Exception {
        File f = new File(fileName);
        if (!f.exists()) {
            System.out.println("ERREUR SERVICE : Fichier introuvable -> " + f.getAbsolutePath());
            return new byte[0]; // Retourne vide si pas trouvé
        }
        return Files.readAllBytes(f.toPath());
    }
}