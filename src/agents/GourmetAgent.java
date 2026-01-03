import java.util.*;

public class GourmetAgent extends AgentImpl {
    private List<Restaurant> maSelection = new ArrayList<>();

    public GourmetAgent(Queue<Node> itinerary, String jarPath) {
        super(itinerary, jarPath);
    }

    @Override
    public void main() throws MoveException {
        // On récupère l'annuaire de services local
        Hashtable<String, Object> services = getServices();

        // --- CAS 1 : On est sur le serveur "Guide" ---
        if (services.containsKey("ServiceGuide")) {
            System.out.println("AGENT : Arrivé au Guide Michelin.");
            ServiceGuide guide = (ServiceGuide) services.get("ServiceGuide");

            // 1. Récupération locale (rapide)
            List<Restaurant> resultats = guide.getRestaurants();
            
            // 2. Tri local (On garde les meilleures notes en premier)
            resultats.sort((r1, r2) -> Double.compare(r2.getNote(), r1.getNote()));

            // 3. Filtrage (On ne garde que le TOP 3 pour voyager léger)
            int top = Math.min(3, resultats.size());
            this.maSelection = new ArrayList<>(resultats.subList(0, top));
            
            System.out.println("AGENT : J'ai sélectionné les " + top + " meilleures tables.");
        }

        // --- CAS 2 : On est sur le serveur "Tarifs" ---
        else if (services.containsKey("ServiceTarif")) {
            System.out.println("AGENT : Arrivé au Service Compta. Récupération des prix...");
            ServiceTarif tarifService = (ServiceTarif) services.get("ServiceTarif");

            // Pour chaque restaurant sélectionné, on cherche son prix localement
            for (Restaurant r : this.maSelection) {
                double prix = tarifService.getPrix(r.getNom());
                r.setPrixMoyen(prix);
            }
            System.out.println("AGENT : Prix mis à jour pour la sélection.");
        }

        // --- CAS 3 : Retour à la maison (ou serveur sans service connu) ---
        else {
            System.out.println("AGENT : ");
            if (maSelection.isEmpty()) {
                System.out.println("Aucun restaurant trouvé ou parcours incomplet.");
            } else {
                for (Restaurant r : maSelection) {
                    System.out.println(r);
                }
            }
        }

        // Migration vers l'étape suivante
        if (!getItinerary().isEmpty()) {
            Node nextHop = getItinerary().poll();
            move(nextHop);
        }
    }
}