import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Hashtable;

public class AgentClassLoader extends ClassLoader {
    
    // Chemin vers le JAR
    private final String jarPath;
    
    // Cache pour stocker le bytecode une fois lu (NomClasse -> byte[])
    private Hashtable<String, byte[]> bytecodeCache;

    public AgentClassLoader(String jarPath) {
        // Délégation au chargeur système pour les classes Java standard
        super(AgentClassLoader.class.getClassLoader());
        this.jarPath = jarPath;
        this.bytecodeCache = new Hashtable<>();
    }

    /**
     * Cette méthode charge physiquement le bytecode depuis le fichier JAR
     * et le stocke dans le cache.
     * * @param className Le nom de la classe (ex: "com.monprojet.TestAgent")
     * @return Le tableau d'octets de la classe, ou null si non trouvée/erreur.
     */
    public byte[] integrateCode(String className) {
        // 1. Calcul du chemin interne au JAR (ex: "com/monprojet/TestAgent.class")
        String classFile = className.replace('.', '/') + ".class";
        
        // 2. Construction de l'URL vers le JAR
        File file = new File(jarPath);
        URL url;
        try {
            url = new URL("jar:file:" + file.getAbsolutePath() + "!/" + classFile);
        } catch (Exception e) {
            System.err.println("Erreur URL JAR : " + e.getMessage());
            return null;
        }

        // 3. Lecture du flux d'octets
        try (InputStream is = url.openStream();
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            
            int data;
            while ((data = is.read()) != -1) {
                buffer.write(data);
            }
            byte[] classBytes = buffer.toByteArray();

            // 4. Mise en cache immédiate
            this.bytecodeCache.put(className, classBytes);
            System.out.println("ClassLoader : Classe " + className + " chargée depuis le JAR.");
            
            return classBytes;

        } catch (IOException e) {
            // La classe n'est probablement pas dans ce JAR
            // On retourne null pour laisser findClass gérer l'erreur
            return null;
        }
    }

    /**
     * Méthode usine pour créer un objet Starter lié à ce ClassLoader.
     */
    public Starter loadStarter() {
        return new Starter(this);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] classBytes = null;

        // 1. Vérification du cache mémoire
        if (bytecodeCache.containsKey(name)) {
            classBytes = bytecodeCache.get(name);
        } 
        // 2. Si pas en cache, on appelle integrateCode pour aller chercher dans le JAR
        else {
            classBytes = integrateCode(name);
        }

        // 3. Si toujours null, c'est que la classe n'existe pas
        if (classBytes == null) {
            throw new ClassNotFoundException("Classe introuvable dans le JAR : " + name);
        }

        // 4. Transformation des octets en Classe Java
        return defineClass(name, classBytes, 0, classBytes.length);
    }
}