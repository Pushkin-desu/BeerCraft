package io.github.pushkindesu.beercraft;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Campfire;
import org.bukkit.block.data.Levelled;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class HeatSourceChecker {

    private final BeerCraftPlugin plugin;
    private Set<Material> heatSources = EnumSet.noneOf(Material.class);

    public HeatSourceChecker(BeerCraftPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        heatSources = EnumSet.noneOf(Material.class);
        List<String> list = plugin.getConfig().getStringList("heat-sources");
        for (String s : list) {
            try {
                heatSources.add(Material.valueOf(s.toUpperCase()));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Unknown heat-source material: " + s);
            }
        }
    }

    public boolean isHot(Block cauldron) {
        Block below = cauldron.getRelative(0, -1, 0);
        Material mat = below.getType();

        if (!heatSources.contains(mat)) return false;

        // Special handling for campfires
        if (mat == Material.CAMPFIRE || mat == Material.SOUL_CAMPFIRE) {
            if (below.getBlockData() instanceof Campfire campfire) {
                return campfire.isLit();
            }
            return false;
        }

        // Special handling for lava: only source blocks (level == 0)
        if (mat == Material.LAVA) {
            if (below.getBlockData() instanceof Levelled levelled) {
                return levelled.getLevel() == 0;
            }
            return false;
        }

        // All other heat sources: type match is enough
        return true;
    }
}
