package platform;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;
import java.util.jar.JarFile;

public class AgentLoader extends ClassLoader {
    private final Map<String, byte[]> classes = new HashMap<>();

    public AgentLoader(ClassLoader parent) {
        super(parent);
    }

    public void addClass(String name, byte[] bytes) {
        classes.put(name, bytes);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytes = classes.get(name);
        if (bytes == null) {
            throw new ClassNotFoundException(name);
        }
        return defineClass(name, bytes, 0, bytes.length);
    }

    public void loadJar(String path) throws Exception {
        try (JarFile jar = new JarFile(path)) {
            jar.stream().forEach(entry -> {
                if (entry.getName().endsWith(".class")) {
                    try (InputStream is = jar.getInputStream(entry)) {
                        // Lecture des octets
                        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                        byte[] data = new byte[4096];
                        int nRead;
                        while ((nRead = is.read(data, 0, data.length)) != -1) {
                            buffer.write(data, 0, nRead);
                        }

                        // Conversion "dossier/Fichier.class" -> "dossier.Fichier"
                        String className = entry.getName()
                                .replace('/', '.')
                                .replace(".class", "");

                        this.addClass(className, buffer.toByteArray());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}
