package io.github.pushkindesu.beercraft;

import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.java.JavaPlugin;

public final class BeerCraftPlugin extends JavaPlugin {

    public PluginConfig cfg;
    public Messages msg;
    public RecipeRegistry registry;
    public HeatSourceChecker heatChecker;
    public CauldronStateStore stateStore;
    public BeerItem beerItem;
    public CauldronManager cauldronManager;

    private BukkitTask cauldronTickTask;
    private BukkitTask stateFlushTask;

    @Override
    public void onEnable() {
        cfg         = new PluginConfig(this);
        msg         = new Messages(this);
        registry    = new RecipeRegistry(this);
        heatChecker = new HeatSourceChecker(this);
        stateStore  = new CauldronStateStore(this);
        beerItem    = new BeerItem(this);

        registry.load();

        // Register listeners
        var pm = getServer().getPluginManager();
        pm.registerEvents(stateStore, this);
        cauldronManager = new CauldronManager(this);
        pm.registerEvents(cauldronManager, this);
        pm.registerEvents(new DrinkListener(this), this);
        pm.registerEvents(new ResourcePackHandler(this), this);

        startSchedulers(cauldronManager);

        // Register command
        var cmdExecutor = new BeerCraftCommand(this);
        var beerCraftCmd = getCommand("beercraft");
        if (beerCraftCmd != null) {
            beerCraftCmd.setExecutor(cmdExecutor);
            beerCraftCmd.setTabCompleter(cmdExecutor);
        }

        // bStats
        if (cfg.bstats) {
            new org.bstats.bukkit.Metrics(this, 25000); // placeholder chart ID
        }

        getLogger().info("BeerCraft enabled.");
    }

    @Override
    public void onDisable() {
        if (stateStore != null) {
            stateStore.onDisableFlush();
        }
        getLogger().info("BeerCraft disabled.");
    }

    private void startSchedulers(CauldronManager cm) {
        cauldronTickTask = getServer().getScheduler().runTaskTimer(
                this, cm::tick, cfg.cauldronTickPeriod, cfg.cauldronTickPeriod);
        long flushTicks = (long) cfg.stateFlushPeriodSeconds * 20L;
        stateFlushTask = getServer().getScheduler().runTaskTimer(
                this, stateStore::periodicFlush, flushTicks, flushTicks);
    }

    public void restartSchedulers(CauldronManager cm) {
        if (cauldronTickTask != null) cauldronTickTask.cancel();
        if (stateFlushTask  != null) stateFlushTask.cancel();
        startSchedulers(cm);
    }
}
