package io.github.pushkindesu.beercraft;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class DrinkListener implements Listener {

    private final BeerCraftPlugin plugin;

    public DrinkListener(BeerCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDrink(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        Optional<String> tagOpt = plugin.beerItem.getRecipeTag(item);
        if (tagOpt.isEmpty()) return;

        String tag = tagOpt.get();
        if (BeerItem.SWILL_TAG.equals(tag)) {
            // Swill — no effects, let vanilla return empty bottle
            return;
        }

        Optional<Recipe> recipeOpt = plugin.registry.get(tag);
        if (recipeOpt.isEmpty()) {
            // Recipe no longer known (e.g. after reload) — treat as swill
            return;
        }

        Recipe recipe = recipeOpt.get();
        Player p = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin,
                () -> recipe.effects.forEach(p::addPotionEffect));
    }
}
