package io.github.pushkindesu.beercraft;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType;

import java.util.Optional;

public class BeerItem {

    public static final String SWILL_TAG = "__swill__";
    private static final Color SWILL_COLOR = Color.fromRGB(0x6B6B47);

    private final BeerCraftPlugin plugin;

    public BeerItem(BeerCraftPlugin plugin) {
        this.plugin = plugin;
    }

    public ItemStack create(Recipe recipe) {
        ItemStack item = new ItemStack(Material.POTION);
        item.editMeta(PotionMeta.class, meta -> {
            meta.setBasePotionType(PotionType.WATER);
            meta.setColor(recipe.color);
            if (recipe.customModelData != 0) {
                meta.setCustomModelData(recipe.customModelData);
            }
            meta.displayName(recipe.displayName);
            meta.lore(recipe.lore);
            meta.getPersistentDataContainer().set(
                    recipeKey(), PersistentDataType.STRING, recipe.name);
        });
        return item;
    }

    public ItemStack createSwill() {
        ItemStack item = new ItemStack(Material.POTION);
        item.editMeta(PotionMeta.class, meta -> {
            meta.setBasePotionType(PotionType.WATER);
            meta.setColor(SWILL_COLOR);
            meta.displayName(Component.text(plugin.msg.get("swill_name"))
                    .color(NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            meta.getPersistentDataContainer().set(
                    recipeKey(), PersistentDataType.STRING, SWILL_TAG);
        });
        return item;
    }

    public Optional<String> getRecipeTag(ItemStack item) {
        if (item == null || item.getType() != Material.POTION) return Optional.empty();
        if (!(item.getItemMeta() instanceof PotionMeta meta)) return Optional.empty();
        String tag = meta.getPersistentDataContainer().get(recipeKey(), PersistentDataType.STRING);
        return Optional.ofNullable(tag);
    }

    private NamespacedKey recipeKey() {
        return new NamespacedKey(plugin, "recipe");
    }
}
