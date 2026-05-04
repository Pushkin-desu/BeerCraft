package io.github.pushkindesu.beercraft;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

public class CauldronStateStore implements Listener {

    private static final String BOILED_TICKS_KEY = "boiled_ticks";
    private static final String INGREDIENTS_KEY  = "ingredients";

    // hot-cache: chunk -> (location -> state)
    private final Map<Chunk, Map<Location, CauldronState>> cache = new HashMap<>();
    private final BeerCraftPlugin plugin;

    public CauldronStateStore(BeerCraftPlugin plugin) {
        this.plugin = plugin;
    }

    // ---- Public API ----

    public CauldronState get(Location loc) {
        Chunk chunk = loc.getChunk();
        Map<Location, CauldronState> map = cache.get(chunk);
        if (map == null) return null;
        return map.get(normalise(loc));
    }

    public void set(Location loc, CauldronState state) {
        Chunk chunk = loc.getChunk();
        cache.computeIfAbsent(chunk, k -> new HashMap<>())
                .put(normalise(loc), state);
    }

    public void remove(Location loc) {
        Chunk chunk = loc.getChunk();
        Map<Location, CauldronState> map = cache.get(chunk);
        if (map == null) return;
        map.remove(normalise(loc));
        if (map.isEmpty()) cache.remove(chunk);
        // Also remove from PDC
        removePdc(chunk, normalise(loc));
    }

    public void forEachActiveCauldron(BiConsumer<Location, CauldronState> consumer) {
        for (var chunkEntry : cache.entrySet()) {
            Chunk chunk = chunkEntry.getKey();
            if (!chunk.isLoaded()) continue;
            for (var entry : chunkEntry.getValue().entrySet()) {
                consumer.accept(entry.getKey(), entry.getValue());
            }
        }
    }

    public int getActiveCauldronCount() {
        int sum = 0;
        for (var inner : cache.values()) sum += inner.size();
        return sum;
    }

    // ---- Flush ----

    public void periodicFlush() {
        for (var chunkEntry : cache.entrySet()) {
            flushChunk(chunkEntry.getKey(), chunkEntry.getValue());
        }
    }

    public void onDisableFlush() {
        periodicFlush();
    }

    // ---- Chunk Events ----

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        Map<Location, CauldronState> loaded = loadFromPdc(chunk);
        if (!loaded.isEmpty()) {
            cache.put(chunk, new HashMap<>(loaded));
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        Map<Location, CauldronState> map = cache.remove(chunk);
        if (map != null) {
            flushChunk(chunk, map);
        }
    }

    // ---- PDC Serialization ----

    private String pdcKey(Location loc) {
        int localX = (loc.getBlockX() & 0xF);
        int localZ = (loc.getBlockZ() & 0xF);
        return "cauldron_" + localX + "_" + loc.getBlockY() + "_" + localZ;
    }

    private void flushChunk(Chunk chunk, Map<Location, CauldronState> map) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        for (var entry : map.entrySet()) {
            Location loc = entry.getKey();
            CauldronState state = entry.getValue();
            String keyStr = pdcKey(loc);
            NamespacedKey key = new NamespacedKey(plugin, keyStr);

            PersistentDataContainer container = pdc.getAdapterContext().newPersistentDataContainer();
            container.set(
                    new NamespacedKey(plugin, BOILED_TICKS_KEY),
                    PersistentDataType.INTEGER,
                    state.boiledTicks
            );

            PersistentDataContainer ingContainer = pdc.getAdapterContext().newPersistentDataContainer();
            for (var ing : state.ingredients.entrySet()) {
                ingContainer.set(
                        new NamespacedKey(plugin, ing.getKey().name().toLowerCase(Locale.ROOT)),
                        PersistentDataType.INTEGER,
                        ing.getValue()
                );
            }
            container.set(
                    new NamespacedKey(plugin, INGREDIENTS_KEY),
                    PersistentDataType.TAG_CONTAINER,
                    ingContainer
            );

            pdc.set(key, PersistentDataType.TAG_CONTAINER, container);
        }
    }

    private Map<Location, CauldronState> loadFromPdc(Chunk chunk) {
        Map<Location, CauldronState> result = new HashMap<>();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        String pluginNamespace = plugin.getName().toLowerCase(Locale.ROOT);

        for (NamespacedKey key : pdc.getKeys()) {
            if (!key.getNamespace().equals(pluginNamespace)) continue;
            String keyStr = key.getKey();
            if (!keyStr.startsWith("cauldron_")) continue;

            String[] parts = keyStr.split("_");
            // cauldron_<localX>_<y>_<localZ>
            if (parts.length != 4) continue;
            try {
                int localX = Integer.parseInt(parts[1]);
                int y      = Integer.parseInt(parts[2]);
                int localZ = Integer.parseInt(parts[3]);
                int worldX = chunk.getX() * 16 + localX;
                int worldZ = chunk.getZ() * 16 + localZ;

                PersistentDataContainer container = pdc.get(key, PersistentDataType.TAG_CONTAINER);
                if (container == null) continue;

                Integer boiledTicks = container.get(
                        new NamespacedKey(plugin, BOILED_TICKS_KEY),
                        PersistentDataType.INTEGER);
                if (boiledTicks == null) boiledTicks = 0;

                Map<Material, Integer> ingredients = new HashMap<>();
                PersistentDataContainer ingContainer = container.get(
                        new NamespacedKey(plugin, INGREDIENTS_KEY),
                        PersistentDataType.TAG_CONTAINER);
                if (ingContainer != null) {
                    for (NamespacedKey ingKey : ingContainer.getKeys()) {
                        String matName = ingKey.getKey().toUpperCase();
                        Integer amount = ingContainer.get(ingKey, PersistentDataType.INTEGER);
                        if (amount == null || amount <= 0) continue;
                        try {
                            ingredients.put(Material.valueOf(matName), amount);
                        } catch (IllegalArgumentException ignored) {}
                    }
                }

                Location loc = new Location(chunk.getWorld(), worldX, y, worldZ);
                result.put(loc, new CauldronState(boiledTicks, ingredients));
            } catch (NumberFormatException ignored) {}
        }
        return result;
    }

    private void removePdc(Chunk chunk, Location loc) {
        String keyStr = pdcKey(loc);
        NamespacedKey key = new NamespacedKey(plugin, keyStr);
        chunk.getPersistentDataContainer().remove(key);
    }

    private static Location normalise(Location loc) {
        return new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
}
