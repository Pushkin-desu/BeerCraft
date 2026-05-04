package io.github.pushkindesu.beercraft;

import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;

import java.util.List;
import java.util.Map;

public class Recipe {

    public final String name;
    public final Component displayName;
    public final List<Component> lore;
    public final int customModelData;
    public final Color color;
    public final Map<Material, Integer> ingredients;
    public final int boilSeconds;
    public final List<PotionEffect> effects;

    public Recipe(
            String name,
            Component displayName,
            List<Component> lore,
            int customModelData,
            Color color,
            Map<Material, Integer> ingredients,
            int boilSeconds,
            List<PotionEffect> effects
    ) {
        this.name = name;
        this.displayName = displayName;
        this.lore = lore;
        this.customModelData = customModelData;
        this.color = color;
        this.ingredients = ingredients;
        this.boilSeconds = boilSeconds;
        this.effects = effects;
    }
}
