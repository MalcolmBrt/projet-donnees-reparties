import java.lang.reflect.Constructor;

public class Starter {
    private AgentClassLoader loader;

    public Starter(AgentClassLoader loader) {
        this.loader = loader;
    }

    public Object newInstance(String className, Object... args) throws Exception {
        Class<?> clazz = loader.loadClass(className);

        if (args == null || args.length == 0) {
            return clazz.getDeclaredConstructor().newInstance();
        }

        // Recherche d'un constructeur compatible avec les arguments
        for (Constructor<?> c : clazz.getConstructors()) {
            if (c.getParameterCount() == args.length) {
                // NOTE: Pour une vraie robustesse, il faudrait vérifier les types exacts des paramètres.
                // Ici on suppose que le nombre d'arguments suffit pour ce projet.
                return c.newInstance(args);
            }
        }
        throw new NoSuchMethodException("Aucun constructeur trouvé pour " + className + " avec ces arguments.");
    }
}