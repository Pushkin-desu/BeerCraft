package io.github.pushkindesu.beercraft;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Random;

public class DrunkennessTracker implements Listener {

    private static final String KEY_DRINK_COUNT  = "drink_count";
    private static final String KEY_LAST_DRINK   = "last_drink_at";
    private static final String KEY_DRUNK_LEVEL  = "drunk_level";

    private final BeerCraftPlugin plugin;
    private final NamespacedKey keyDrinkCount;
    private final NamespacedKey keyLastDrink;
    private final NamespacedKey keyDrunkLevel;
    private final Random random = new Random();

    public DrunkennessTracker(BeerCraftPlugin plugin) {
        this.plugin       = plugin;
        this.keyDrinkCount = new NamespacedKey(plugin, KEY_DRINK_COUNT);
        this.keyLastDrink  = new NamespacedKey(plugin, KEY_LAST_DRINK);
        this.keyDrunkLevel = new NamespacedKey(plugin, KEY_DRUNK_LEVEL);
    }

    // ---- Public API ----

    public void recordDrink(Player p) {
        if (!plugin.cfg.drunkEnabled) return;
        if (p.hasPermission("beercraft.sober.bypass")) return;

        PersistentDataContainer pdc = p.getPersistentDataContainer();
        int count = getCount(pdc) + 1;
        pdc.set(keyDrinkCount, PersistentDataType.INTEGER, count);
        pdc.set(keyLastDrink, PersistentDataType.LONG, System.currentTimeMillis());

        int oldLevel = getLevel(pdc);
        int newLevel = calcLevel(count);
        if (newLevel > oldLevel) {
            pdc.set(keyDrunkLevel, PersistentDataType.INTEGER, newLevel);
            applyEffectsForLevel(p, newLevel);
        }
    }

    public void sober(Player p) {
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        pdc.set(keyDrinkCount, PersistentDataType.INTEGER, 0);
        pdc.set(keyDrunkLevel, PersistentDataType.INTEGER, 0);
        removeAllDrunkEffects(p);
    }

    public int getDrinkCount(Player p) {
        return getCount(p.getPersistentDataContainer());
    }

    public int getDrunkLevel(Player p) {
        return getLevel(p.getPersistentDataContainer());
    }

    /** Called by the scheduler every 60s (1200 ticks). */
    public void tick() {
        long now = System.currentTimeMillis();
        long soberWindowMs = (long) plugin.cfg.drunkSoberRateMinutes * 60_000L;

        for (Player p : plugin.getServer().getOnlinePlayers()) {
            PersistentDataContainer pdc = p.getPersistentDataContainer();
            long lastDrink = pdc.getOrDefault(keyLastDrink, PersistentDataType.LONG, 0L);

            // Sober up if no recent drinks
            if ((now - lastDrink) > soberWindowMs) {
                int count = getCount(pdc);
                if (count > 0) {
                    int newCount = count - 1;
                    pdc.set(keyDrinkCount, PersistentDataType.INTEGER, newCount);

                    int oldLevel = getLevel(pdc);
                    int newLevel = calcLevel(newCount);
                    if (newLevel < oldLevel) {
                        pdc.set(keyDrunkLevel, PersistentDataType.INTEGER, newLevel);
                        removeAllDrunkEffects(p);
                        if (newLevel > 0) applyEffectsForLevel(p, newLevel);
                    }
                }
            }

            // Blackout stumble
            if (getLevel(pdc) >= 3 && random.nextDouble() < plugin.cfg.drunkStumbleChance) {
                double dx = (random.nextDouble() - 0.5) * 0.6;
                double dz = (random.nextDouble() - 0.5) * 0.6;
                p.setVelocity(p.getVelocity().add(new Vector(dx, 0, dz)));
            }
        }
    }

    // ---- PlayerJoinEvent: restore effects after re-login ----

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.cfg.drunkEnabled) return;
        Player p = event.getPlayer();
        int level = getLevel(p.getPersistentDataContainer());
        if (level > 0) {
            applyEffectsForLevel(p, level);
        }
    }

    // ---- Internals ----

    private void applyEffectsForLevel(Player p, int level) {
        removeAllDrunkEffects(p);
        List<PluginConfig.DrunkEffect> effects = switch (level) {
            case 1 -> plugin.cfg.drunkEffectsTipsy;
            case 2 -> plugin.cfg.drunkEffectsDrunk;
            case 3 -> plugin.cfg.drunkEffectsBlackout;
            default -> List.of();
        };
        for (PluginConfig.DrunkEffect e : effects) {
            p.addPotionEffect(new PotionEffect(e.type(), e.durationTicks(), e.amplifier(), true, true));
        }
    }

    private void removeAllDrunkEffects(Player p) {
        // Remove every effect type that appears in any drunk level config
        for (PluginConfig.DrunkEffect e : plugin.cfg.drunkEffectsTipsy)    p.removePotionEffect(e.type());
        for (PluginConfig.DrunkEffect e : plugin.cfg.drunkEffectsDrunk)    p.removePotionEffect(e.type());
        for (PluginConfig.DrunkEffect e : plugin.cfg.drunkEffectsBlackout) p.removePotionEffect(e.type());
    }

    private int calcLevel(int count) {
        if (count >= plugin.cfg.drunkThresholdBlackout) return 3;
        if (count >= plugin.cfg.drunkThresholdDrunk)    return 2;
        if (count >= plugin.cfg.drunkThresholdTipsy)    return 1;
        return 0;
    }

    private int getCount(PersistentDataContainer pdc) {
        return pdc.getOrDefault(keyDrinkCount, PersistentDataType.INTEGER, 0);
    }

    private int getLevel(PersistentDataContainer pdc) {
        return pdc.getOrDefault(keyDrunkLevel, PersistentDataType.INTEGER, 0);
    }
}
