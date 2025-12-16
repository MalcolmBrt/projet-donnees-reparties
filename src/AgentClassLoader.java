import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Hashtable;

public class AgentClassLoader extends ClassLoader {
    
    // Chemin vers le JAR (optionnel si tout est en mémoire)
    private final String jarPath;
    
    // Cache pour stocker le bytecode reçu directement (NomClasse -> byte[])
    private Hashtable<String, byte[]> bytecodeCache;

    public AgentClassLoader(String jarPath) {
        // Délégation au chargeur système
        super(AgentClassLoader.class.getClassLoader());
        this.jarPath = jarPath;
        this.bytecodeCache = new Hashtable<>();
    }

    /**
     * Permet d'injecter manuellement du bytecode dans le chargeur.
     * Utile quand l'agent arrive par le réseau et qu'on a les octets en mémoire.
     */
    public void integrateCode(String className, byte[] code) {
        this.bytecodeCache.put(className, code);
        System.out.println("ClassLoader : Code intégré pour " + className);
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

        // 1. D'abord, on regarde si le code est déjà dans notre cache (via integrateCode)
        if (bytecodeCache.containsKey(name)) {
            classBytes = bytecodeCache.get(name);
        } 
        // 2. Sinon, on va chercher dans le fichier JAR
        else if (jarPath != null) {
            try {
                classBytes = loadBytesFromJar(name);
                // On met en cache pour la prochaine fois
                if (classBytes != null) {
                    bytecodeCache.put(name, classBytes);
                }
            } catch (IOException e) {
                // Ignore l'erreur ici, on lancera ClassNotFoundException à la fin si null
            }
        }

        if (classBytes == null) {
            throw new ClassNotFoundException("Classe introuvable (ni cache, ni JAR) : " + name);
        }

        // 3. Transformation des octets en Classe Java
        return defineClass(name, classBytes, 0, classBytes.length);
    }

    /**
     * Méthode utilitaire privée pour extraire les octets du JAR.
     */
    private byte[] loadBytesFromJar(String name) throws IOException {
        String classFile = name.replace('.', '/') + ".class";
        File file = new File(jarPath);
        URL url = new URL("jar:file:" + file.getAbsolutePath() + "!/" + classFile);

        try (InputStream is = url.openStream();
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            
            int data;
            while ((data = is.read()) != -1) {
                buffer.write(data);
            }
            return buffer.toByteArray();
        }
    }
}