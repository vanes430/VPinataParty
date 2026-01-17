package github.vanes430.vpinataparty.utils;

import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.*;

public class ThreadingTest {

    private MockedStatic<Bukkit> mockedBukkit;
    private Plugin plugin;
    private SchedulerUtils schedulerUtils;
    private RegionScheduler regionScheduler;
    private GlobalRegionScheduler globalScheduler;

    @BeforeEach
    void setUp() {
        mockedBukkit = mockStatic(Bukkit.class);
        plugin = mock(Plugin.class);
        schedulerUtils = new SchedulerUtils(plugin);
        regionScheduler = mock(RegionScheduler.class);
        globalScheduler = mock(GlobalRegionScheduler.class);

        mockedBukkit.when(Bukkit::getRegionScheduler).thenReturn(regionScheduler);
        mockedBukkit.when(Bukkit::getGlobalRegionScheduler).thenReturn(globalScheduler);
    }

    @AfterEach
    void tearDown() {
        mockedBukkit.close();
    }

    @Test
    void testRunAtLocationUsesRegionScheduler() {
        Location location = mock(Location.class);
        Runnable task = () -> {};

        schedulerUtils.runAtLocation(location, task);

        /* Verify that the RegionScheduler.execute method was called with the correct parameters */
        verify(regionScheduler).execute(eq(plugin), eq(location), eq(task));
    }

    @Test
    void testRunGlobalUsesGlobalScheduler() {
        Runnable task = () -> {};

        schedulerUtils.runGlobal(task);

        /* Verify that the GlobalRegionScheduler.execute method was called */
        verify(globalScheduler).execute(eq(plugin), eq(task));
    }
}
