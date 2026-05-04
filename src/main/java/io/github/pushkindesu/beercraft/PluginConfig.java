package io.github.pushkindesu.beercraft;

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
    }
}
