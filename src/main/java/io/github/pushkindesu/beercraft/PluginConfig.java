package io.github.pushkindesu.beercraft;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PluginConfig {

    public final int cauldronTickPeriod;
    public final double boilTimeMinMultiplier;
    public final double boilTimeMaxMultiplier;
    public final String failedBrewAction;
    public final int stateFlushPeriodSeconds;
    public final String locale;
    public final boolean bstats;
    public final boolean rpEnabled;
    public final String rpUrl;
    public final String rpHash;
    public final boolean rpRequired;
    public final String rpPromptMessage;

    // Drunkenness
    public final boolean drunkEnabled;
    public final int drunkWindowMinutes;
    public final int drunkThresholdTipsy;
    public final int drunkThresholdDrunk;
    public final int drunkThresholdBlackout;
    public final int drunkSoberRateMinutes;
    public final List<DrunkEffect> drunkEffectsTipsy;
    public final List<DrunkEffect> drunkEffectsDrunk;
    public final List<DrunkEffect> drunkEffectsBlackout;
    public final double drunkStumbleChance;

    /** Simple struct for a configured potion effect entry. */
    public record DrunkEffect(PotionEffectType type, int durationTicks, int amplifier) {}

    public PluginConfig(BeerCraftPlugin plugin) {
        plugin.saveDefaultConfig();
        var c = plugin.getConfig();
        this.cauldronTickPeriod      = c.getInt("cauldron-tick-period", 20);
        this.boilTimeMinMultiplier   = c.getDouble("boil-time-min-multiplier", 0.8);
        this.boilTimeMaxMultiplier   = c.getDouble("boil-time-max-multiplier", 1.5);
        this.failedBrewAction        = c.getString("failed-brew-action", "drop_swill");
        this.stateFlushPeriodSeconds = c.getInt("state-flush-period-seconds", 60);
        this.locale                  = c.getString("locale", "en");
        this.bstats                  = c.getBoolean("bstats", false);
        this.rpEnabled               = c.getBoolean("resource-pack.enabled", false);
        this.rpUrl                   = c.getString("resource-pack.url", "");
        this.rpHash                  = c.getString("resource-pack.hash", "");
        this.rpRequired              = c.getBoolean("resource-pack.required", false);
        this.rpPromptMessage         = c.getString("resource-pack.prompt-message",
                "Install the BeerCraft resource pack for custom brewing visuals.");

        // Drunkenness
        this.drunkEnabled            = c.getBoolean("drunkenness.enabled", true);
        this.drunkWindowMinutes      = c.getInt("drunkenness.window-minutes", 5);
        this.drunkThresholdTipsy     = c.getInt("drunkenness.thresholds.tipsy", 1);
        this.drunkThresholdDrunk     = c.getInt("drunkenness.thresholds.drunk", 3);
        this.drunkThresholdBlackout  = c.getInt("drunkenness.thresholds.blackout", 6);
        this.drunkSoberRateMinutes   = c.getInt("drunkenness.sober-rate-minutes", 5);
        this.drunkStumbleChance      = c.getDouble("drunkenness.blackout-stumble-chance", 0.05);
        this.drunkEffectsTipsy       = loadEffects(c.getMapList("drunkenness.effects.tipsy"));
        this.drunkEffectsDrunk       = loadEffects(c.getMapList("drunkenness.effects.drunk"));
        this.drunkEffectsBlackout    = loadEffects(c.getMapList("drunkenness.effects.blackout"));
    }

    private static List<DrunkEffect> loadEffects(List<java.util.Map<?, ?>> list) {
        if (list == null || list.isEmpty()) return Collections.emptyList();
        List<DrunkEffect> result = new ArrayList<>();
        for (var map : list) {
            String typeName = String.valueOf(map.get("type"));
            int durationSec = map.containsKey("duration-seconds")
                    ? Integer.parseInt(String.valueOf(map.get("duration-seconds"))) : 30;
            int amplifier = map.containsKey("amplifier")
                    ? Integer.parseInt(String.valueOf(map.get("amplifier"))) : 0;
            try {
                PotionEffectType pet = PotionEffectType.getByName(typeName);
                if (pet != null) result.add(new DrunkEffect(pet, durationSec * 20, amplifier));
            } catch (Exception ignored) {}
        }
        return Collections.unmodifiableList(result);
    }
}
