package org.little100.better_slabs.command;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.little100.better_slabs.BetterSlabs;
import org.little100.better_slabs.registry.SlabRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class BetterSlabsCommand implements CommandExecutor, TabCompleter {

    private final BetterSlabs plugin;

    public BetterSlabsCommand(@NotNull BetterSlabs plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("betterslabs.admin")) {
            sender.sendMessage(plugin.getLang("command.no-permission"));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(plugin.getLang("command.usage-main"));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.reloadPlugin();
                sender.sendMessage(plugin.getLang("command.reload-success"));
            }
            case "info" -> {
                sender.sendMessage(plugin.getLang("command.info-version",
                        "{version}", plugin.getPluginVersion()));
                sender.sendMessage(plugin.getLang("command.info-slab-types",
                        "{count}", String.valueOf(plugin.getSlabRegistry().getSupportedCount())));
                sender.sendMessage(plugin.getLang("command.info-stored-cells",
                        "{count}", String.valueOf(plugin.getSlabStorage().size())));
                sender.sendMessage(plugin.getLang("command.info-folia",
                        "{folia}", String.valueOf(plugin.getScheduler().isFolia())));
            }
            case "give" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.getLang("command.players-only"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(plugin.getLang("command.usage-give"));
                    return true;
                }
                Material material = Material.matchMaterial(args[1].toUpperCase(Locale.ROOT));
                if (material == null || !plugin.getSlabRegistry().isSupported(material)) {
                    sender.sendMessage(plugin.getLang("command.unknown-slab"));
                    return true;
                }
                int amount = 1;
                if (args.length >= 3) {
                    try {
                        amount = Math.max(1, Math.min(64, Integer.parseInt(args[2])));
                    } catch (NumberFormatException e) {
                        sender.sendMessage(plugin.getLang("give.invalid-amount"));
                        return true;
                    }
                }
                ItemStack item = plugin.getSlabRegistry().createVerticalSlabItem(material, amount);
                if (item == null) {
                    sender.sendMessage(plugin.getLang("command.unknown-slab"));
                    return true;
                }
                player.getInventory().addItem(item);
                sender.sendMessage(plugin.getLang("command.give-success",
                        "{amount}", String.valueOf(amount),
                        "{material}", SlabRegistry.prettyName(material)));
            }
            default -> sender.sendMessage(plugin.getLang("command.usage-main"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("betterslabs.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            return filter(List.of("reload", "give", "info"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return filter(plugin.getSlabRegistry().getOrderedMaterials().stream()
                    .map(m -> m.name().toLowerCase(Locale.ROOT))
                    .collect(Collectors.toList()), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return filter(Arrays.asList("1", "16", "64"), args[2]);
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(option);
            }
        }
        return out;
    }
}
