package services.restaurants;

import java.util.ArrayList;
import java.util.List;

public class ServiceGuideImpl implements ServiceGuide {
    @Override
    public List<Restaurant> getRestaurants(int n) {
        List<Restaurant> list = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            list.add(new Restaurant("Chez Luigi " + i, 10 + (Math.random() * 10)));
        }
        return list;
    }
}