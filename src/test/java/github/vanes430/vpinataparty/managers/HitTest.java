package github.vanes430.vpinataparty.managers;

import github.vanes430.vpinataparty.PinataListener;
import github.vanes430.vpinataparty.VPinataParty;
import github.vanes430.vpinataparty.utils.SchedulerUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class HitTest {

    private MockedStatic<Bukkit> mockedBukkit;
    private VPinataParty plugin;
    private SchedulerUtils scheduler;
    private PinataManager pinataManager;
    private PinataListener listener;
    private LoggingManager loggingManager;

    @BeforeEach
    void setUp() {
        mockedBukkit = mockStatic(Bukkit.class);
        plugin = mock(VPinataParty.class);
        when(plugin.getName()).thenReturn("vpinataparty");
        
        FileConfiguration mockConfig = mock(FileConfiguration.class);
        when(plugin.getConfig()).thenReturn(mockConfig);
        when(mockConfig.getInt(anyString(), anyInt())).thenReturn(10);
        
        loggingManager = mock(LoggingManager.class);
        when(plugin.getLoggingManager()).thenReturn(loggingManager);
        
        scheduler = mock(SchedulerUtils.class);
        pinataManager = new PinataManager(plugin, scheduler, loggingManager);
        listener = new PinataListener(plugin, scheduler, pinataManager);

        when(plugin.getLogger()).thenReturn(Logger.getLogger("VPinataParty"));
    }

    @AfterEach
    void tearDown() {
        mockedBukkit.close();
    }

    @Test
    void testMemoryLeakPrevention() {
        UUID pinataUuid = UUID.randomUUID();
        
        injectHealth(pinataUuid, 1.0);
        assertNotNull(pinataManager.getHealth(pinataUuid), "Pinata data should exist in memory after spawn");

        Llama mockPinata = mock(Llama.class);
        when(mockPinata.getUniqueId()).thenReturn(pinataUuid);
        
        EntityDeathEvent deathEvent = new EntityDeathEvent(mockPinata, null, new java.util.ArrayList<>());
        listener.onEntityDeath(deathEvent);

        assertEquals(0.0, pinataManager.getHealth(pinataUuid), "Pinata data must be cleared from memory (0.0) after death event");
    }

    @Test
    void testManualDespawnCleanup() {
        UUID pinataUuid = UUID.randomUUID();
        injectHealth(pinataUuid, 1.0);

        Llama mockPinata = mock(Llama.class);
        Player mockPlayer = mock(Player.class);
        World mockWorld = mock(World.class);
        Location mockLoc = new Location(mockWorld, 0, 0, 0);
        PersistentDataContainer mockPdc = mock(PersistentDataContainer.class);

        when(mockPinata.getUniqueId()).thenReturn(pinataUuid);
        when(mockPinata.getPersistentDataContainer()).thenReturn(mockPdc);
        when(mockPinata.getLocation()).thenReturn(mockLoc);
        when(mockPinata.getWorld()).thenReturn(mockWorld);
        
        NamespacedKey pinataKey = new NamespacedKey(plugin, "is_pinata");
        when(mockPdc.has(eq(pinataKey), eq(PersistentDataType.BYTE))).thenReturn(true);

        EntityDamageByEntityEvent hitEvent = mock(EntityDamageByEntityEvent.class);
        when(hitEvent.getDamager()).thenReturn(mockPlayer);
        when(hitEvent.getEntity()).thenReturn(mockPinata);

        listener.onHit(hitEvent);

        assertEquals(0.0, pinataManager.getHealth(pinataUuid), "Pinata data must be cleared from memory (0.0) after reached 0 HP");
        verify(mockPinata).remove();
    }

    private void injectHealth(UUID uuid, double health) {
        try {
            java.lang.reflect.Field field = PinataManager.class.getDeclaredField("pinataHealthMap");
            field.setAccessible(true);
            java.util.Map<UUID, PinataManager.PinataData> map = (java.util.Map<UUID, PinataManager.PinataData>) field.get(pinataManager);
            map.put(uuid, new PinataManager.PinataData(health, health));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}