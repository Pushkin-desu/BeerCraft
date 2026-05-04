package io.github.pushkindesu.beercraft;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

public class CauldronState {

    public int boiledTicks;
    public final Map<Material, Integer> ingredients;
    /** Not persisted — resets to false on server restart, which is acceptable. */
    public transient boolean readyNotified = false;

    public CauldronState() {
        this.boiledTicks = 0;
        this.ingredients = new HashMap<>();
    }

    public CauldronState(int boiledTicks, Map<Material, Integer> ingredients) {
        this.boiledTicks = boiledTicks;
        this.ingredients = new HashMap<>(ingredients);
    }

    public int totalIngredients() {
        int sum = 0;
        for (int v : ingredients.values()) sum += v;
        return sum;
    }
}
