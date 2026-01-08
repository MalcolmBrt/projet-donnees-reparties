package services.restaurants;

import java.util.List;

public interface ServiceGuide {
    // Retourne la liste des restaurants pour une région donnée
    List<Restaurant> getRestaurants(int n);
}