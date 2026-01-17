package github.vanes430.vpinataparty.managers;

import github.vanes430.vpinataparty.VPinataParty;
import github.vanes430.vpinataparty.utils.SchedulerUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class PinataManager {

    private final VPinataParty plugin;
    private final SchedulerUtils scheduler;
    private final LoggingManager logger;
    
    /* Internal class to track Pinata stats */
    public static record PinataData(double currentHealth, double maxHealth) {}
    private final Map<UUID, PinataData> pinataHealthMap = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> pinataBossBarMap = new ConcurrentHashMap<>();

    public PinataManager(VPinataParty plugin, SchedulerUtils scheduler, LoggingManager logger) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.logger = logger;
    }

    public void spawnParty() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("spawn-locations");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String locStr = section.getString(key);
            if (locStr != null) {
                Location loc = parseLocation(locStr);
                if (loc != null) {
                    scheduler.runAtLocation(loc, () -> summonPinata(loc));
                } else {
                    plugin.getLogger().warning("Invalid location format for key: " + key);
                }
            }
        }
    }

    public Location parseLocation(String str) {
        try {
            String[] parts = str.split(":");
            if (parts.length != 2) return null;
            String worldName = parts[0].trim();
            String[] coords = parts[1].trim().split(",");
            World world = Bukkit.getWorld(worldName);
            if (world == null) return null;
            return new Location(world, Double.parseDouble(coords[0].trim()), Double.parseDouble(coords[1].trim()), Double.parseDouble(coords[2].trim()));
        } catch (Exception e) { return null; }
    }

    public void summonPinata(Location location) {
        String typeStr = plugin.getConfig().getString("pinata.type", "minecraft:llama");
        EntityType type = EntityType.LLAMA;
        try {
            if (typeStr.contains(":")) {
                String[] split = typeStr.split(":");
                type = Registry.ENTITY_TYPE.get(new NamespacedKey(split[0], split[1]));
            } else {
                type = EntityType.valueOf(typeStr.toUpperCase());
            }
        } catch (Exception e) {}
        if (type == null) type = EntityType.LLAMA;

        Llama pinata = (Llama) location.getWorld().spawnEntity(location, type);
        pinata.setMaxHealth(100.0);
        pinata.setHealth(100.0);
        pinata.setCustomNameVisible(true);
        pinata.setAI(!plugin.getConfig().getBoolean("pinata.no-ai", true));
        
        double initialHealth = calculateHealth();
        pinataHealthMap.put(pinata.getUniqueId(), new PinataData(initialHealth, initialHealth));

        updateDisplayNameAndBossBar(pinata, initialHealth, initialHealth);

        /* Log Spawn */
        logger.log("SPAWN: Pinata spawned at " + location.getWorld().getName() + " " + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ() + " with " + initialHealth + " HP. (UUID: " + pinata.getUniqueId() + ")");

        /* Start proximity task for BossBar */
        if (plugin.getConfig().getBoolean("pinata.boss-bar.enabled", true)) {
            startBossBarTask(pinata);
        }

        /* Expiry Task */
        int expiry = plugin.getConfig().getInt("pinata.expiry-time", 300);
        if (expiry > 0) {
            pinata.getScheduler().execute(plugin, () -> {
                if (pinata.isValid()) {
                    removePinataData(pinata.getUniqueId());
                    pinata.remove();
                }
            }, null, expiry * 20L);
        }

        pinata.getPersistentDataContainer().set(new NamespacedKey(plugin, "is_pinata"), PersistentDataType.BYTE, (byte) 1);
    }

    private void startBossBarTask(Entity entity) {
        BossBar bossBar = createBossBar(entity.getUniqueId());
        pinataBossBarMap.put(entity.getUniqueId(), bossBar);

        int range = plugin.getConfig().getInt("pinata.boss-bar.range", 30);

        /* Repeating task on entity scheduler (Folia safe) */
        entity.getScheduler().runAtFixedRate(plugin, (task) -> {
            if (!entity.isValid()) {
                task.cancel();
                return;
            }

            float progress = (float) (getHealth(entity.getUniqueId()) / getMaxHealth(entity.getUniqueId()));
            bossBar.progress(Math.max(0, Math.min(1, progress)));

            /* Manage players who see the bar */
            for (Player player : Bukkit.getOnlinePlayers()) {
                boolean inRange = player.getWorld().equals(entity.getWorld()) && 
                                 player.getLocation().distanceSquared(entity.getLocation()) <= (range * range);
                
                if (inRange) {
                    player.showBossBar(bossBar);
                } else {
                    player.hideBossBar(bossBar);
                }
            }
        }, () -> {}, 1L, 20L); /* Every 1 second */
    }

    private BossBar createBossBar(UUID uuid) {
        String baseName = plugin.getConfig().getString("pinata.display-name", "<red><b>PINATA");
        BossBar.Color color = BossBar.Color.valueOf(plugin.getConfig().getString("pinata.boss-bar.color", "RED").toUpperCase());
        BossBar.Overlay overlay = BossBar.Overlay.valueOf(plugin.getConfig().getString("pinata.boss-bar.overlay", "PROGRESS").toUpperCase());

        return BossBar.bossBar(MiniMessage.miniMessage().deserialize(baseName), 1.0f, color, overlay);
    }

    public void updateDisplayNameAndBossBar(Entity entity, double current, double max) {
        /* 1. Update Head Display Name */
        String baseName = plugin.getConfig().getString("pinata.display-name", "<red><b>PINATA");
        String format = plugin.getConfig().getString("pinata.health-bar.format", "<name> <gray>[<health_color><current><gray>/<max>]");
        String healthColor = current / max > 0.5 ? "<green>" : (current / max > 0.25 ? "<yellow>" : "<red>");
        String finalName = format.replace("<name>", baseName).replace("<current>", String.valueOf((int) Math.max(0, current))).replace("<max>", String.valueOf((int) max)).replace("<health_color>", healthColor);

        entity.getScheduler().execute(plugin, () -> {
            entity.customName(MiniMessage.miniMessage().deserialize(finalName));
        }, null, 0L);

        /* 2. Update BossBar Progress */
        BossBar bar = pinataBossBarMap.get(entity.getUniqueId());
        if (bar != null) {
            bar.progress((float) (Math.max(0, current) / max));
        }
    }

    private double calculateHealth() {
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        int minHp = plugin.getConfig().getInt("pinata.health-scaling.min-health", 20);
        int maxHp = plugin.getConfig().getInt("pinata.health-scaling.max-health", 200);
        int minPl = plugin.getConfig().getInt("pinata.health-scaling.min-players", 5);
        int maxPl = plugin.getConfig().getInt("pinata.health-scaling.max-players", 50);
        if (onlinePlayers <= minPl) return (double) minHp;
        if (onlinePlayers >= maxPl) return (double) maxHp;
        double slope = (double) (maxHp - minHp) / (maxPl - minPl);
        return Math.floor(minHp + (slope * (onlinePlayers - minPl)));
    }

    public Double decrementHealth(UUID uuid) {
        AtomicReference<Double> result = new AtomicReference<>();
        pinataHealthMap.computeIfPresent(uuid, (k, old) -> {
            double newVal = old.currentHealth() - 1.0;
            result.set(newVal);
            return new PinataData(newVal, old.maxHealth());
        });
        return result.get();
    }

    public Double getHealth(UUID uuid) { PinataData data = pinataHealthMap.get(uuid); return data != null ? data.currentHealth() : 0.0; }
    public Double getMaxHealth(UUID uuid) { PinataData data = pinataHealthMap.get(uuid); return data != null ? data.maxHealth() : 1.0; }

    public void removePinataData(UUID uuid) {
        pinataHealthMap.remove(uuid);
        BossBar bar = pinataBossBarMap.remove(uuid);
        if (bar != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.hideBossBar(bar);
            }
        }
    }
}