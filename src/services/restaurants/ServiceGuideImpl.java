package services.restaurants;

import java.util.ArrayList;
import java.util.List;

public class ServiceGuideImpl implements ServiceGuide {
    @Override
    public List<Restaurant> getRestaurants() {
        List<Restaurant> list = new ArrayList<>();
        // Génération de fausses données propre
        for (int i = 1; i <= 20; i++) {
            list.add(new Restaurant("Chez Luigi " + i, 10 + (Math.random() * 10)));
        }
        return list;
    }
}