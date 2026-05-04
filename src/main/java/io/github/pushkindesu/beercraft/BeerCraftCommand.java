package io.github.pushkindesu.beercraft;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

public class BeerCraftCommand implements CommandExecutor, TabCompleter {

    private final BeerCraftPlugin plugin;

    public BeerCraftCommand(BeerCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        if (!sender.hasPermission("beercraft.admin")) {
            sender.sendMessage(Component.text(plugin.msg.get("no_permission")).color(NamedTextColor.RED));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload"      -> handleReload(sender);
            case "list"        -> handleList(sender);
            case "give"        -> handleGive(sender, args);
            case "info"        -> handleInfo(sender);
            case "sober"       -> handleSober(sender, args);
            case "drunkstatus" -> handleDrunkStatus(sender, args);
            default            -> sendHelp(sender, label);
        }
        return true;
    }

    // ---- Subcommands ----

    private void handleReload(CommandSender sender) {
        try {
            plugin.reloadConfig();
            plugin.cfg = new PluginConfig(plugin);
            plugin.registry.load();
            plugin.heatChecker.reload();
            plugin.msg.reload();
            plugin.restartSchedulers(plugin.cauldronManager);
            sender.sendMessage(Component.text(plugin.msg.get("reload_ok")).color(NamedTextColor.GREEN));
        } catch (Exception e) {
            plugin.getLogger().severe("Reload error: " + e.getMessage());
            sender.sendMessage(Component.text(plugin.msg.get("reload_error")).color(NamedTextColor.RED));
        }
    }

    private void handleList(CommandSender sender) {
        Collection<Recipe> all = plugin.registry.getAll();
        for (Recipe recipe : all) {
            StringJoiner ing = new StringJoiner(", ");
            for (var e : recipe.ingredients.entrySet()) {
                ing.add(e.getKey().name() + "\u00d7" + e.getValue());
            }
            sender.sendMessage(Component.text()
                    .append(Component.text(recipe.name, NamedTextColor.YELLOW))
                    .append(Component.text(" | " + ing + " | " + recipe.boilSeconds + "s"
                            + " | effects: " + recipe.effects.size(), NamedTextColor.GRAY))
                    .build());
        }
        sender.sendMessage(Component.text(
                plugin.msg.get("list_total", Map.of("count", String.valueOf(all.size()))))
                .color(NamedTextColor.GOLD));
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /beercraft give <recipe> [player]")
                    .color(NamedTextColor.RED));
            return;
        }

        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found: " + args[2]).color(NamedTextColor.RED));
                return;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage(Component.text(plugin.msg.get("give_console_no_target"))
                    .color(NamedTextColor.RED));
            return;
        }

        String recipeName = args[1].toLowerCase(Locale.ROOT);
        ItemStack stack;
        if (recipeName.equalsIgnoreCase("swill")) {
            stack = plugin.beerItem.createSwill();
            recipeName = "swill";
        } else {
            Optional<Recipe> opt = plugin.registry.get(recipeName);
            if (opt.isEmpty()) {
                sender.sendMessage(Component.text(
                        plugin.msg.get("recipe_unknown", Map.of("name", recipeName)))
                        .color(NamedTextColor.RED));
                return;
            }
            stack = plugin.beerItem.create(opt.get());
        }

        Map<Integer, ItemStack> overflow = target.getInventory().addItem(stack);
        if (!overflow.isEmpty()) {
            target.getWorld().dropItemNaturally(target.getLocation(), stack);
        }

        sender.sendMessage(Component.text(
                plugin.msg.get("give_success", Map.of("recipe", recipeName, "player", target.getName())))
                .color(NamedTextColor.GREEN));
    }

    private void handleInfo(CommandSender sender) {
        String version = plugin.getPluginMeta().getVersion();
        int recipeCount = plugin.registry.getAll().size();
        int cauldronCount = plugin.stateStore.getActiveCauldronCount();
        sender.sendMessage(Component.text()
                .append(Component.text(
                        plugin.msg.get("info_header", Map.of("version", version)),
                        NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text(
                        plugin.msg.get("info_recipes", Map.of("count", String.valueOf(recipeCount))),
                        NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text(
                        plugin.msg.get("info_cauldrons", Map.of("count", String.valueOf(cauldronCount))),
                        NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text(
                        plugin.msg.get("info_locale", Map.of("locale", plugin.cfg.locale)),
                        NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text(
                        plugin.msg.get("info_tick", Map.of(
                                "ticks", String.valueOf(plugin.cfg.cauldronTickPeriod),
                                "flush", String.valueOf(plugin.cfg.stateFlushPeriodSeconds))),
                        NamedTextColor.GRAY))
                .build());
    }

    private void handleSober(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /beercraft sober <player>").color(NamedTextColor.RED));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + args[1]).color(NamedTextColor.RED));
            return;
        }
        plugin.drunknessTracker.sober(target);
        sender.sendMessage(Component.text(
                plugin.msg.get("sober_success", Map.of("player", target.getName())))
                .color(NamedTextColor.GREEN));
    }

    private void handleDrunkStatus(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found: " + args[1]).color(NamedTextColor.RED));
                return;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage(Component.text("Usage: /beercraft drunkstatus <player>").color(NamedTextColor.RED));
            return;
        }

        int count = plugin.drunknessTracker.getDrinkCount(target);
        int level = plugin.drunknessTracker.getDrunkLevel(target);
        String levelName = switch (level) {
            case 1 -> plugin.msg.get("level_tipsy");
            case 2 -> plugin.msg.get("level_drunk");
            case 3 -> plugin.msg.get("level_blackout");
            default -> plugin.msg.get("level_sober");
        };

        boolean isSelf = sender instanceof Player p && p.equals(target);
        String msgKey = isSelf ? "drunkstatus_self" : "drunkstatus_other";
        Map<String, String> placeholders = isSelf
                ? Map.of("count", String.valueOf(count), "level", levelName)
                : Map.of("player", target.getName(), "count", String.valueOf(count), "level", levelName);
        sender.sendMessage(Component.text(plugin.msg.get(msgKey, placeholders)).color(NamedTextColor.GOLD));
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(Component.text()
                .append(Component.text("BeerCraft v" + plugin.getPluginMeta().getVersion(), NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("/" + label + " reload", NamedTextColor.YELLOW))
                .append(Component.text(" \u2014 reload config & recipes", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("/" + label + " list", NamedTextColor.YELLOW))
                .append(Component.text(" \u2014 list all loaded recipes", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("/" + label + " give <recipe> [player]", NamedTextColor.YELLOW))
                .append(Component.text(" \u2014 give a brewed bottle", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("/" + label + " info", NamedTextColor.YELLOW))
                .append(Component.text(" \u2014 show plugin info", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("/" + label + " sober <player>", NamedTextColor.YELLOW))
                .append(Component.text(" \u2014 sober up a player", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("/" + label + " drunkstatus [player]", NamedTextColor.YELLOW))
                .append(Component.text(" \u2014 check drunkenness level", NamedTextColor.GRAY))
                .build());
    }

    // ---- Tab completion ----

    private static final List<String> SUBCOMMANDS = List.of("reload", "list", "give", "info", "sober", "drunkstatus");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("beercraft.admin")) return List.of();

        if (args.length == 1) {
            return filterPrefix(SUBCOMMANDS, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            List<String> names = new ArrayList<>(plugin.registry.getAllNames());
            names.add("swill");
            return filterPrefix(names, args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            List<String> players = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .toList();
            return filterPrefix(players, args[2]);
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("sober")
                || args[0].equalsIgnoreCase("drunkstatus"))) {
            List<String> players = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .toList();
            return filterPrefix(players, args[1]);
        }

        return List.of();
    }

    private static List<String> filterPrefix(Collection<String> source, String prefix) {
        String lower = prefix.toLowerCase();
        return source.stream()
                .filter(s -> s.toLowerCase().startsWith(lower))
                .toList();
    }
}
