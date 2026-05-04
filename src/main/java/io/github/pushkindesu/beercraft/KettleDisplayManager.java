package io.github.pushkindesu.beercraft;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class KettleDisplayManager {

    private final BeerCraftPlugin plugin;
    private final NamespacedKey kettleKey;

    // runtime-only: cauldron block location → ItemDisplay UUID
    private final Map<Location, UUID> displays = new HashMap<>();

    public KettleDisplayManager(BeerCraftPlugin plugin) {
        this.plugin = plugin;
        this.kettleKey = new NamespacedKey(plugin, "kettle_display");
    }

    /** Spawn a kettle ItemDisplay over the cauldron block. Returns the new UUID. */
    public UUID spawn(Location loc) {
        Location spawnLoc = loc.clone().add(0.5, 0.5, 0.5);
        ItemDisplay display = loc.getWorld().spawn(spawnLoc, ItemDisplay.class, entity -> {
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.setCustomModelData(100);
            item.setItemMeta(meta);

            entity.setItemStack(item);
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            entity.setPersistent(false);
            entity.setInvulnerable(true);
            entity.setGravity(false);
            entity.setBillboard(ItemDisplay.Billboard.FIXED);
            entity.setBrightness(new Display.Brightness(15, 15));
            entity.getPersistentDataContainer().set(kettleKey, PersistentDataType.BYTE, (byte) 1);
        });
        displays.put(normalise(loc), display.getUniqueId());
        return display.getUniqueId();
    }

    /** Remove the display entity for the given cauldron location, if any. */
    public void despawn(Location loc) {
        UUID uuid = displays.remove(normalise(loc));
        if (uuid != null) {
            Entity entity = loc.getWorld().getEntity(uuid);
            if (entity != null) entity.remove();
        }
    }

    /** Remove all tracked display entities (called on plugin disable). */
    public void despawnAll() {
        for (Map.Entry<Location, UUID> entry : displays.entrySet()) {
            Entity entity = entry.getKey().getWorld().getEntity(entry.getValue());
            if (entity != null) entity.remove();
        }
        displays.clear();
    }

    /**
     * Returns the live entity for a cauldron location, or null if it is gone.
     * Used by the tick loop to detect entities that have been removed externally.
     */
    public Entity getEntity(Location loc) {
        UUID uuid = displays.get(normalise(loc));
        if (uuid == null) return null;
        return loc.getWorld().getEntity(uuid);
    }

    /**
     * Spawn only if no live entity is currently tracked for this location.
     * Safe to call every tick — does nothing when already present.
     */
    public void spawnIfMissing(Location loc) {
        if (getEntity(loc) == null) {
            spawn(loc);
        }
    }

    /** Called after a chunk's PDC state is loaded into the hot-cache. Spawns displays for every restored cauldron. */
    public void handleChunkLoad(Chunk chunk) {
        plugin.stateStore.forEachActiveCauldron((loc, state) -> {
            if (loc.getChunk().equals(chunk)) {
                spawnIfMissing(loc);
            }
        });
    }

    /** Called before a chunk is unloaded. Removes all display entities belonging to that chunk. */
    public void handleChunkUnload(Chunk chunk) {
        Iterator<Map.Entry<Location, UUID>> it = displays.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Location, UUID> entry = it.next();
            if (entry.getKey().getChunk().equals(chunk)) {
                Entity entity = entry.getKey().getWorld().getEntity(entry.getValue());
                if (entity != null) entity.remove();
                it.remove();
            }
        }
    }

    private static Location normalise(Location loc) {
        return new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
}
