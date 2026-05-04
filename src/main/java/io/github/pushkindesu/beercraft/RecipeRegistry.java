package io.github.pushkindesu.beercraft;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.Collection;
import java.util.logging.Logger;

public class RecipeRegistry {

    private final BeerCraftPlugin plugin;
    private final Logger log;
    private final Map<String, Recipe> recipes = new LinkedHashMap<>();

    public RecipeRegistry(BeerCraftPlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    public void load() {
        recipes.clear();

        // Save default recipes.yml if absent
        File file = new File(plugin.getDataFolder(), "recipes.yml");
        if (!file.exists()) {
            try (InputStream in = plugin.getResource("recipes.yml")) {
                if (in != null) {
                    plugin.getDataFolder().mkdirs();
                    Files.copy(in, file.toPath());
                }
            } catch (IOException e) {
                log.warning("Could not save default recipes.yml: " + e.getMessage());
            }
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        // Also load from jar as fallback
        try (InputStream in = plugin.getResource("recipes.yml")) {
            if (in != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(in, StandardCharsets.UTF_8));
                cfg.setDefaults(defaults);
            }
        } catch (Exception ignored) {}

        ConfigurationSection recipesSection = cfg.getConfigurationSection("recipes");
        if (recipesSection == null) {
            log.warning("recipes.yml has no 'recipes' section — no recipes loaded.");
            return;
        }

        for (String key : recipesSection.getKeys(false)) {
            ConfigurationSection sec = recipesSection.getConfigurationSection(key);
            if (sec == null) continue;

            try {
                Recipe recipe = parseRecipe(key, sec);
                recipes.put(key, recipe);
            } catch (Exception e) {
                log.warning("Skipping recipe '" + key + "': " + e.getMessage());
            }
        }

        log.info("Loaded " + recipes.size() + " recipe(s).");
    }

    private Recipe parseRecipe(String key, ConfigurationSection sec) {
        String displayNameStr = Objects.requireNonNull(
                sec.getString("display-name"), "missing display-name");

        Component displayName = LegacyComponentSerializer.legacyAmpersand()
                .deserialize(displayNameStr)
                .decoration(TextDecoration.ITALIC, false);

        List<Component> lore = new ArrayList<>();
        List<String> loreStrings = sec.getStringList("lore");
        for (String l : loreStrings) {
            lore.add(LegacyComponentSerializer.legacyAmpersand()
                    .deserialize(l)
                    .decoration(TextDecoration.ITALIC, false));
        }

        int customModelData = sec.getInt("custom-model-data", 0);

        String colorStr = sec.getString("color", "#FFFFFF");
        Color color;
        try {
            int hex = Integer.parseInt(colorStr.replace("#", ""), 16);
            color = Color.fromRGB(hex);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid color '" + colorStr + "'");
        }

        ConfigurationSection ingSection = sec.getConfigurationSection("ingredients");
        if (ingSection == null) throw new IllegalArgumentException("missing ingredients");

        Map<Material, Integer> ingredients = new LinkedHashMap<>();
        for (String matName : ingSection.getKeys(false)) {
            Material mat;
            try {
                mat = Material.valueOf(matName.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown material '" + matName + "'");
            }
            int amount = ingSection.getInt(matName);
            if (amount <= 0) throw new IllegalArgumentException("ingredient amount must be > 0 for " + matName);
            ingredients.put(mat, amount);
        }

        int boilSeconds = sec.getInt("boil-seconds", -1);
        if (boilSeconds <= 0) throw new IllegalArgumentException("missing or invalid boil-seconds");

        List<PotionEffect> effects = new ArrayList<>();
        List<?> effectList = sec.getList("effects", new ArrayList<>());
        for (Object obj : effectList) {
            if (!(obj instanceof Map<?, ?> effectMap)) continue;
            String typeName = String.valueOf(effectMap.get("type"));
            int duration = effectMap.containsKey("duration")
                    ? ((Number) effectMap.get("duration")).intValue() : 30;
            int amplifier = effectMap.containsKey("amplifier")
                    ? ((Number) effectMap.get("amplifier")).intValue() : 0;

            // Resolve via Registry
            PotionEffectType pet = Registry.EFFECT.get(NamespacedKey.minecraft(typeName.toLowerCase()));
            if (pet == null) {
                throw new IllegalArgumentException("Unknown potion effect type '" + typeName + "'");
            }
            effects.add(new PotionEffect(pet, duration * 20, amplifier));
        }

        return new Recipe(key, displayName, lore, customModelData, color, ingredients, boilSeconds, effects);
    }

    public Optional<Recipe> get(String name) {
        return Optional.ofNullable(recipes.get(name));
    }

    public Collection<Recipe> getAll() {
        return recipes.values();
    }

    public Set<String> getAllNames() {
        return recipes.keySet();
    }

    public Set<Material> getAllIngredientMaterials() {
        Set<Material> set = new HashSet<>();
        for (Recipe r : recipes.values()) set.addAll(r.ingredients.keySet());
        return set;
    }

    public int getMaxIngredientSum() {
        int max = 0;
        for (Recipe r : recipes.values()) {
            int sum = r.ingredients.values().stream().mapToInt(Integer::intValue).sum();
            if (sum > max) max = sum;
        }
        return max;
    }

    public Optional<Recipe> findMatching(Map<Material, Integer> ingredients, int boiledSeconds) {
        double minMult = plugin.cfg.boilTimeMinMultiplier;
        double maxMult = plugin.cfg.boilTimeMaxMultiplier;

        for (Recipe recipe : recipes.values()) {
            if (!recipe.ingredients.equals(ingredients)) continue;
            double minTime = recipe.boilSeconds * minMult;
            double maxTime = recipe.boilSeconds * maxMult;
            if (boiledSeconds >= minTime && boiledSeconds <= maxTime) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }
}
