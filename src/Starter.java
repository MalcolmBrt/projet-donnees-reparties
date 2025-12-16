import java.lang.reflect.Constructor;

public class Starter {
    
    private AgentClassLoader loader;

    public Starter(AgentClassLoader loader) {
        this.loader = loader;
    }

    public Object newInstance(String className, Object... args) throws Exception {
        // Appelera findClass -> integrateCode -> lecture du JAR
        Class<?> clazz = loader.loadClass(className);

        // Recherche simple du premier constructeur disponible
        // (Pour une vraie app, il faudrait chercher le constructeur correspondant aux types des args)
        if (args.length > 0) {
            Constructor<?> constructor = clazz.getConstructors()[0];
            return constructor.newInstance(args);
        } else {
            return clazz.newInstance();
        }
    }
}