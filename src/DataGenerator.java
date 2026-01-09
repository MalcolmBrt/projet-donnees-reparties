import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

public class DataGenerator {
    public static void generate(String fileName, int sizeInMb) throws IOException {
        File f = new File(fileName);
        try (FileOutputStream fos = new FileOutputStream(f)) {
            byte[] buffer = new byte[1024]; 
            new Random().nextBytes(buffer); 
            
            // Écrit 'sizeInMb' fois 1024 blocs de 1KB = sizeInMb Mo
            for (int i = 0; i < sizeInMb * 1024; i++) {
                fos.write(buffer);
            }
        }
        System.out.println("Fichier généré : " + fileName + " (" + sizeInMb + " MB)");
    }

    public static void main(String[] args) throws IOException {
        int size = 10; // Valeur par défaut
        
        // C'est cette partie qui manquait ou n'était pas à jour chez vous :
        if (args.length > 0) {
            size = Integer.parseInt(args[0]);
        }
        
        generate("bigdata.log", size);
    }
}