package github.vanes430.vpinataparty.utils;

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

public class SchedulerUtils {

    private final Plugin plugin;

    public SchedulerUtils(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Executes a task on the region that owns the location.
     * Compatible with both Folia (region thread) and Paper (main thread).
     */
    public void runAtLocation(Location location, Runnable runnable) {
        RegionScheduler scheduler = Bukkit.getRegionScheduler();
        scheduler.execute(plugin, location, runnable);
    }

    /**
     * Executes a task on the global region.
     * Used for commands, global logic, etc.
     */
    public void runGlobal(Runnable runnable) {
        GlobalRegionScheduler scheduler = Bukkit.getGlobalRegionScheduler();
        scheduler.execute(plugin, runnable);
    }

    /**
     * Executes a task on the entity's scheduler.
     * The task follows the entity across regions.
     */
    public void runEntity(Entity entity, Runnable runnable) {
        /* entity.getScheduler() is available in Paper API 1.20+ */
        entity.getScheduler().execute(plugin, runnable, null, 0);
    }

    /**
     * Executes a task asynchronously.
     */
    public void runAsync(Consumer<ScheduledTask> consumer) {
        AsyncScheduler scheduler = Bukkit.getAsyncScheduler();
        scheduler.runNow(plugin, consumer);
    }
}