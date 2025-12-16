import java.lang.reflect.Constructor;

public class Starter {
    
    private AgentClassLoader loader;

    public Starter(AgentClassLoader loader) {
        this.loader = loader;
    }

    /**
     * Crée une nouvelle instance d'une classe en utilisant le ClassLoader associé.
     * Cette méthode utilise la réflexion Java.
     * * @param className Le nom complet de la classe à instancier (ex: "TestAgent")
     * @param args Les arguments à passer au constructeur (peut être vide)
     * @return L'objet instancié (Object)
     */
    public Object newInstance(String className, Object... args) throws Exception {
        // 1. Chargement de la classe via notre ClassLoader personnalisé
        // Cela va déclencher la méthode findClass() du AgentClassLoader
        Class<?> clazz = loader.loadClass(className);

        // 2. Recherche du constructeur approprié
        // Pour simplifier, on cherche ici un constructeur qui correspond aux types des arguments passés
        Class<?>[] paramTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = args[i].getClass();
            // Note : La gestion des types primitifs ou interfaces peut nécessiter plus de logique
            // Si vous utilisez TestAgent(Queue, String), assurez-vous de passer les bons types ici.
        }

        // Si aucun argument n'est passé, on utilise le constructeur par défaut
        if (args.length == 0) {
            return clazz.newInstance();
        } else {
            // Recherche du constructeur spécifique
            // NOTE : C'est une simplification. Dans votre projet réel, comme TestAgent
            // prend (Queue, String), il faudra peut-être hardcoder les types ici ou 
            // parcourir les constructeurs disponibles.
            Constructor<?> constructor = clazz.getConstructors()[0]; // Prend le 1er constructeur public dispo
            return constructor.newInstance(args);
        }
    }
}