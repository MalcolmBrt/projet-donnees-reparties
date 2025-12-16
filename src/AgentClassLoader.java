import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;

public class AgentClassLoader extends ClassLoader {
    private final String jarPath;
    private final Hashtable<String, byte[]> bytecodeCache;

    public AgentClassLoader(String jarPath) {
        super(AgentClassLoader.class.getClassLoader()); // Parent = System ClassLoader
        this.jarPath = jarPath;
        this.bytecodeCache = new Hashtable<>();
    }

    public Starter loadStarter() {
        return new Starter(this);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // 1. Vérifier le cache
        if (bytecodeCache.containsKey(name)) {
            byte[] b = bytecodeCache.get(name);
            return defineClass(name, b, 0, b.length);
        }

        // 2. Charger depuis le JAR via integrateCode
        byte[] b = integrateCode(name);
        if (b == null) {
            throw new ClassNotFoundException(name);
        }

        // 3. Définir la classe
        return defineClass(name, b, 0, b.length);
    }

    public byte[] integrateCode(String className) {
        String resourceName = className.replace('.', '/') + ".class";
        File file = new File(jarPath);
        
        try {
            URL url = new URL("jar:file:" + file.getAbsolutePath() + "!/" + resourceName);
            try (InputStream is = url.openStream();
                 ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                
                int data;
                while ((data = is.read()) != -1) {
                    buffer.write(data);
                }
                
                byte[] classBytes = buffer.toByteArray();
                // Mise en cache
                bytecodeCache.put(className, classBytes);
                System.out.println("ClassLoader : Classe " + className + " chargée.");
                return classBytes;
            }
        } catch (IOException e) {
            // La classe n'est pas dans ce JAR, on retourne null
            return null;
        }
    }
}