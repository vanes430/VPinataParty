package github.vanes430.vpinataparty.managers;

import github.vanes430.vpinataparty.VPinataParty;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class MessageManager {

    private final VPinataParty plugin;
    private File messagesFile;
    private FileConfiguration messagesConfig;

    public MessageManager(VPinataParty plugin) {
        this.plugin = plugin;
        saveDefaultMessages();
    }

    private void saveDefaultMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public void reloadMessages() {
        if (messagesFile == null) {
            messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public void sendMessage(CommandSender sender, String key, String... placeholders) {
        if (messagesConfig == null) reloadMessages();
        
        String rawMessage = messagesConfig.getString(key);
        if (rawMessage == null) {
            sender.sendMessage(ChatColor.RED + "Missing message key: " + key);
            return;
        }

        String prefix = messagesConfig.getString("prefix", "");
        String fullMessage = prefix + rawMessage;

        /* Handle Placeholders (Simple replacement) */
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                fullMessage = fullMessage.replace(placeholders[i], placeholders[i+1]);
            }
        }
        
        sender.sendMessage(MiniMessage.miniMessage().deserialize(fullMessage));
    }
}