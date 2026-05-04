package io.github.pushkindesu.beercraft;

import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.net.URI;
import java.net.URISyntaxException;

public class ResourcePackHandler implements Listener {

    private final BeerCraftPlugin plugin;

    public ResourcePackHandler(BeerCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.cfg.rpEnabled) return;
        if (plugin.cfg.rpUrl == null || plugin.cfg.rpUrl.isBlank()) return;

        Player player = event.getPlayer();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            URI uri;
            try {
                uri = new URI(plugin.cfg.rpUrl);
            } catch (URISyntaxException e) {
                plugin.getLogger().warning("resource-pack.url is not a valid URI: " + plugin.cfg.rpUrl);
                return;
            }

            ResourcePackInfo packInfo = ResourcePackInfo.resourcePackInfo()
                    .uri(uri)
                    .hash(plugin.cfg.rpHash)
                    .build();

            ResourcePackRequest request = ResourcePackRequest.resourcePackRequest()
                    .packs(packInfo)
                    .required(plugin.cfg.rpRequired)
                    .prompt(Component.text(plugin.cfg.rpPromptMessage))
                    .build();

            player.sendResourcePacks(request);
        });
    }
}
