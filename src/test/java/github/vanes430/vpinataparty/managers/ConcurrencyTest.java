package github.vanes430.vpinataparty.managers;

import github.vanes430.vpinataparty.VPinataParty;
import github.vanes430.vpinataparty.utils.SchedulerUtils;
import org.bukkit.Bukkit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class ConcurrencyTest {

    private MockedStatic<Bukkit> mockedBukkit;
    private VPinataParty plugin;
    private SchedulerUtils scheduler;
    private PinataManager pinataManager;
    private LoggingManager loggingManager;

    @BeforeEach
    void setUp() {
        mockedBukkit = mockStatic(Bukkit.class);
        plugin = mock(VPinataParty.class);
        loggingManager = mock(LoggingManager.class);
        scheduler = mock(SchedulerUtils.class);
        pinataManager = new PinataManager(plugin, scheduler, loggingManager);

        when(plugin.getLogger()).thenReturn(Logger.getLogger("VPinataParty"));
    }

    @AfterEach
    void tearDown() {
        mockedBukkit.close();
    }

    @Test
    void testConcurrentHealthDecrement() throws InterruptedException {
        UUID pinataUuid = UUID.randomUUID();
        double initialHealth = 1000.0;
        int threadCount = 100;
        int hitsPerThread = 10;

        injectHealth(pinataUuid, initialHealth);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < hitsPerThread; j++) {
                        pinataManager.decrementHealth(pinataUuid);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        Double finalHealth = pinataManager.getHealth(pinataUuid);
        assertEquals(0.0, finalHealth, "Health should be exactly 0.0 after concurrent hits");
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