package io.github.pushkindesu.beercraft;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

public class CauldronManager implements Listener {

    private final BeerCraftPlugin plugin;

    public CauldronManager(BeerCraftPlugin plugin) {
        this.plugin = plugin;
    }

    // ---- Tick all active cauldrons ----

    public void tick() {
        List<Location> toRemove = new ArrayList<>();
        plugin.stateStore.forEachActiveCauldron((loc, state) -> {
            Block block = loc.getBlock();
            if (block.getType() != Material.WATER_CAULDRON) {
                // Block was changed externally — schedule for removal after iteration
                toRemove.add(loc);
                return;
            }
            // Re-spawn display if it was removed externally (e.g. /kill @e[type=item_display])
            if (plugin.kettleDisplayManager.getEntity(loc) == null) {
                plugin.kettleDisplayManager.spawnIfMissing(loc);
            }
            // Only tick when there are ingredients in the cauldron
            if (state.ingredients.isEmpty()) return;

            if (plugin.heatChecker.isHot(block)) {
                state.boiledTicks += plugin.cfg.cauldronTickPeriod;

                // Visual: bubble particles every tick
                Location particleLoc = block.getLocation().add(0.5, 1.0, 0.5);
                block.getWorld().spawnParticle(Particle.BUBBLE_POP, particleLoc, 5,
                        0.2, 0.05, 0.2, 0.0);

                // Audio: brewing sound every 5 seconds (100 ticks)
                if (state.boiledTicks % 100 == 0) {
                    block.getWorld().playSound(block.getLocation(),
                            Sound.BLOCK_BREWING_STAND_BREW, 0.5f, 1.0f);
                }

                // Ready / overcooked notifications
                int boiledSeconds = state.boiledTicks / 20;
                Optional<Recipe> match = plugin.registry.findMatching(state.ingredients, boiledSeconds);

                if (match.isPresent() && !state.readyNotified) {
                    state.readyNotified = true;
                    block.getWorld().playSound(block.getLocation(),
                            Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
                    block.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, particleLoc, 20,
                            0.4, 0.4, 0.4, 0.0);
                    notifyNearbyPlayers(block, plugin.msg.get("brew_ready"), NamedTextColor.GREEN);
                } else if (match.isEmpty() && state.readyNotified) {
                    state.readyNotified = false;
                    block.getWorld().playSound(block.getLocation(),
                            Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 0.7f);
                    block.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, particleLoc, 5,
                            0.3, 0.1, 0.3, 0.01);
                    notifyNearbyPlayers(block, plugin.msg.get("brew_overcooked"), NamedTextColor.DARK_GRAY);
                }
            }
        });
        toRemove.forEach(plugin.stateStore::remove);
    }

    // ---- PlayerInteractEvent ----

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        Material blockType = block.getType();
        Player player = event.getPlayer();
        ItemStack itemInHand = event.getItem();

        // Fill empty cauldron with water bucket
        if (blockType == Material.CAULDRON && itemInHand != null
                && itemInHand.getType() == Material.WATER_BUCKET) {
            // Vanilla handles this — we do nothing
            return;
        }

        // Ingredient addition to WATER_CAULDRON
        if (blockType == Material.WATER_CAULDRON) {
            if (itemInHand != null && itemInHand.getType() == Material.GLASS_BOTTLE) {
                handleBottleFill(event, block, player, itemInHand);
                return;
            }

            // Check if item is a valid ingredient
            if (itemInHand != null && itemInHand.getType() != Material.AIR
                    && plugin.registry.getAllIngredientMaterials().contains(itemInHand.getType())) {
                handleIngredientAdd(event, block, player, itemInHand);
                return;
            }

            // Empty hand (or non-ingredient item) — show cauldron status if state exists
            CauldronState peekState = plugin.stateStore.get(block.getLocation());
            if (peekState != null && !peekState.ingredients.isEmpty()) {
                String contentsStr = buildContentsString(peekState);
                int seconds = peekState.boiledTicks / 20;
                player.sendActionBar(Component.text(
                        plugin.msg.get("cauldron_status",
                                Map.of("contents", contentsStr, "seconds", String.valueOf(seconds))))
                        .color(NamedTextColor.YELLOW));
            }
        }
    }

    private void handleIngredientAdd(PlayerInteractEvent event, Block block, Player player, ItemStack item) {
        CauldronState state = plugin.stateStore.get(block.getLocation());
        boolean isNew = state == null;
        if (isNew) {
            state = new CauldronState();
        }

        int maxSum = plugin.registry.getMaxIngredientSum() * 2;
        if (state.totalIngredients() >= maxSum) {
            player.sendActionBar(Component.text(plugin.msg.get("cauldron_overfilled"))
                    .color(NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        // Add one ingredient
        Material mat = item.getType();
        state.ingredients.merge(mat, 1, Integer::sum);

        // Remove one item from hand
        if (item.getAmount() == 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            item.setAmount(item.getAmount() - 1);
        }

        plugin.stateStore.set(block.getLocation(), state);
        event.setCancelled(true);

        // Actionbar feedback
        if (isNew) {
            player.sendActionBar(Component.text(plugin.msg.get("brew_success"))
                    .color(NamedTextColor.GREEN));
        } else {
            String contentsStr = buildContentsString(state);
            int seconds = state.boiledTicks / 20;
            player.sendActionBar(Component.text(
                    plugin.msg.get("cauldron_status", Map.of("contents", contentsStr, "seconds", String.valueOf(seconds))))
                    .color(NamedTextColor.YELLOW));
        }
    }

    private void handleBottleFill(PlayerInteractEvent event, Block block, Player player, ItemStack item) {
        CauldronState state = plugin.stateStore.get(block.getLocation());
        if (state == null) {
            // No BeerCraft state — vanilla will produce a water bottle, we don't interfere
            return;
        }

        event.setCancelled(true);

        int boiledSeconds = state.boiledTicks / 20;
        Optional<Recipe> matchOpt = plugin.registry.findMatching(state.ingredients, boiledSeconds);

        ItemStack result;
        if (matchOpt.isPresent()) {
            result = plugin.beerItem.create(matchOpt.get());
        } else {
            if ("drop_swill".equals(plugin.cfg.failedBrewAction)) {
                result = plugin.beerItem.createSwill();
                player.sendActionBar(Component.text(plugin.msg.get("brew_failed_swill"))
                        .color(NamedTextColor.GRAY));
            } else {
                // Return the glass bottle so the player doesn't silently lose it
                result = new ItemStack(Material.GLASS_BOTTLE);
            }
        }

        // Always drain the cauldron completely on bottle fill
        block.setType(Material.CAULDRON);
        plugin.stateStore.remove(block.getLocation());

        // Consume the bottle
        if (item.getAmount() == 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            item.setAmount(item.getAmount() - 1);
        }

        // Give result
        if (result != null) {
            giveOrDrop(player, result, block);
        }
    }

    // ---- Block break / explosion cleanup ----

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Material mat = event.getBlock().getType();
        if (mat == Material.CAULDRON || mat == Material.WATER_CAULDRON) {
            plugin.stateStore.remove(event.getBlock().getLocation());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        for (Block b : event.blockList()) {
            Material mat = b.getType();
            if (mat == Material.CAULDRON || mat == Material.WATER_CAULDRON) {
                plugin.stateStore.remove(b.getLocation());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        for (Block b : event.blockList()) {
            Material mat = b.getType();
            if (mat == Material.CAULDRON || mat == Material.WATER_CAULDRON) {
                plugin.stateStore.remove(b.getLocation());
            }
        }
    }

    private void notifyNearbyPlayers(Block block, String message, NamedTextColor color) {
        Location center = block.getLocation().add(0.5, 0.5, 0.5);
        double radiusSq = 8 * 8;
        for (Player p : block.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(center) <= radiusSq) {
                p.sendActionBar(Component.text(message).color(color));
            }
        }
    }

    private String buildContentsString(CauldronState state) {
        StringJoiner sj = new StringJoiner(", ");
        for (var e : state.ingredients.entrySet()) {
            sj.add(e.getKey().name() + "\u00d7" + e.getValue());
        }
        return sj.toString();
    }

    private void giveOrDrop(Player player, ItemStack item, Block near) {
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
        if (!overflow.isEmpty()) {
            near.getWorld().dropItemNaturally(near.getLocation().add(0.5, 1, 0.5), item);
        }
    }
}
