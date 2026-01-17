package github.vanes430.vpinataparty.commands;

import github.vanes430.vpinataparty.VPinataParty;
import github.vanes430.vpinataparty.managers.MessageManager;
import github.vanes430.vpinataparty.managers.PinataManager;
import github.vanes430.vpinataparty.utils.SchedulerUtils;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PinataCommand implements CommandExecutor, TabCompleter {

    private final VPinataParty plugin;
    private final MessageManager messageManager;
    private final PinataManager pinataManager;
    private final SchedulerUtils scheduler;

    public PinataCommand(VPinataParty plugin, MessageManager messageManager, PinataManager pinataManager, SchedulerUtils scheduler) {
        this.plugin = plugin;
        this.messageManager = messageManager;
        this.pinataManager = pinataManager;
        this.scheduler = scheduler;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("vpinataparty.admin")) {
            messageManager.sendMessage(sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            messageManager.sendMessage(sender, "invalid-args");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("reload")) {
            plugin.reloadConfig();
            messageManager.reloadMessages();
            messageManager.sendMessage(sender, "reload-success");
            return true;
        }

        if (subCommand.equals("party")) {
            pinataManager.spawnParty();
            messageManager.sendMessage(sender, "party-started");
            return true;
        }

        if (subCommand.equals("summon")) {
            if (args.length < 2) {
                /* Default behavior: summon at player location if no arg provided */
                if (sender instanceof Player player) {
                    Location loc = player.getLocation();
                    scheduler.runAtLocation(loc, () -> pinataManager.summonPinata(loc));
                    messageManager.sendMessage(player, "summon-success");
                } else {
                    messageManager.sendMessage(sender, "summon-console-usage");
                }
                return true;
            }

            String locName = args[1];
            String locStr = plugin.getConfig().getString("spawn-locations." + locName);

            if (locStr == null) {
                messageManager.sendMessage(sender, "location-not-found", "<arg>", locName);
                return true;
            }

            Location location = pinataManager.parseLocation(locStr);
            if (location != null) {
                scheduler.runAtLocation(location, () -> pinataManager.summonPinata(location));
                messageManager.sendMessage(sender, "summon-success");
            } else {
                messageManager.sendMessage(sender, "invalid-location-format", "<arg>", locName);
            }
            return true;
        }

        messageManager.sendMessage(sender, "unknown-command");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("vpinataparty.admin")) return new ArrayList<>();

        if (args.length == 1) {
            return Arrays.asList("summon", "party", "reload").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("summon")) {
            ConfigurationSection section = plugin.getConfig().getConfigurationSection("spawn-locations");
            if (section == null) return new ArrayList<>();
            return section.getKeys(false).stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
