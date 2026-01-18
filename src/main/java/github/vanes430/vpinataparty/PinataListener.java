package github.vanes430.vpinataparty;

import github.vanes430.vpinataparty.managers.PinataManager;
import github.vanes430.vpinataparty.utils.SchedulerUtils;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class PinataListener implements Listener {

    private final VPinataParty plugin;
    private final SchedulerUtils scheduler;
    private final PinataManager pinataManager;
    private final NamespacedKey pinataKey;
    private final NamespacedKey lastHitKey;

    public PinataListener(VPinataParty plugin, SchedulerUtils scheduler, PinataManager pinataManager) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.pinataManager = pinataManager;
        this.pinataKey = new NamespacedKey(plugin, "is_pinata");
        this.lastHitKey = new NamespacedKey(plugin, "last_hit_tick");
    }

    /* God Mode: Cancel all types of damage to the Pinata */
    @EventHandler
    public void onGeneralDamage(EntityDamageEvent event) {
        if (event.getEntity().getPersistentDataContainer().has(pinataKey, PersistentDataType.BYTE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getRightClicked().getPersistentDataContainer().has(pinataKey, PersistentDataType.BYTE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteractAt(PlayerInteractAtEntityEvent event) {
        if (event.getRightClicked().getPersistentDataContainer().has(pinataKey, PersistentDataType.BYTE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMount(EntityMountEvent event) {
        if (event.getMount().getPersistentDataContainer().has(pinataKey, PersistentDataType.BYTE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        
        Entity entity = event.getEntity();
        PersistentDataContainer container = entity.getPersistentDataContainer();

        /* 1. Validation: Is this entity a Pinata? */
        if (!container.has(pinataKey, PersistentDataType.BYTE)) return;

        /* Always cancel the event to prevent knockback and vanilla damage */
        event.setCancelled(true);

        /* 2. Cooldown Logic (Invulnerability) */
        int cooldownTicks = plugin.getConfig().getInt("pinata.tick-hit-cooldown", 10);
        long currentTime = Bukkit.getCurrentTick();
        
        if (container.has(lastHitKey, PersistentDataType.LONG)) {
            long lastHit = container.get(lastHitKey, PersistentDataType.LONG);
            if (currentTime - lastHit < cooldownTicks) {
                return;
            }
        }

        /* 3. Update the last hit time */
        container.set(lastHitKey, PersistentDataType.LONG, currentTime);
        
        /* 4. Memory-based Health Logic (Atomic) */
        UUID uuid = entity.getUniqueId();
        Double newHealth = pinataManager.decrementHealth(uuid);
        
        if (newHealth != null) {
            if (newHealth <= 0) {
                /* Pinata is destroyed! */
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                entity.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION, entity.getLocation(), 1);
                
                giveRewards(player);
                
                /* Log Death */
                plugin.getLoggingManager().log("DESTROYED: Pinata " + uuid + " destroyed by " + player.getName() + " at " + entity.getLocation().getBlockX() + "," + entity.getLocation().getBlockY() + "," + entity.getLocation().getBlockZ());

                /* Cleanup memory manually before removal */
                pinataManager.removePinataData(uuid);
                entity.remove();
                return;
            } else {
                /* Update Health Bar & BossBar Display */
                pinataManager.updateDisplayNameAndBossBar(entity, newHealth, pinataManager.getMaxHealth(uuid));
            }
        }
        
        /* 5. Grant Reward per Hit */
        giveRewards(player);
    }

    /* Fallback cleanup if the entity dies or is removed by other means */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        plugin.getLoggingManager().log("REMOVED: Pinata " + event.getEntity().getUniqueId() + " was removed/expired.");
        pinataManager.removePinataData(event.getEntity().getUniqueId());
    }

    private void giveRewards(Player player) {
        FileConfiguration config = plugin.getConfig();
        List<java.util.Map<?, ?>> rewardList = config.getMapList("pinata.rewards");

        for (java.util.Map<?, ?> rewardMap : rewardList) {
            if (!rewardMap.containsKey("chance") || !rewardMap.containsKey("commands")) continue;

            double chance = 0.0;
            Object chanceObj = rewardMap.get("chance");
            if (chanceObj instanceof Number) {
                chance = ((Number) chanceObj).doubleValue();
            }

            if (ThreadLocalRandom.current().nextDouble() * 100 <= chance) {
                Object cmdsObj = rewardMap.get("commands");
                if (cmdsObj instanceof List) {
                    List<String> commands = (List<String>) cmdsObj;
                    for (String cmd : commands) {
                        executeCommand(player, cmd);
                        /* Log Reward Command */
                        plugin.getLoggingManager().log("REWARD: Player " + player.getName() + " triggered reward command: " + cmd);
                    }
                }
            }
        }
    }

    private void executeCommand(Player player, String command) {
        String finalCommand;
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            finalCommand = PlaceholderAPI.setPlaceholders(player, command);
        } else {
            finalCommand = command.replace("%player_name%", player.getName());
        }

        scheduler.runGlobal(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand));
    }
}