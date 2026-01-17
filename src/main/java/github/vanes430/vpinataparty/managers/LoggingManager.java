package github.vanes430.vpinataparty.managers;

import github.vanes430.vpinataparty.VPinataParty;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoggingManager {

    private final VPinataParty plugin;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean enabled;
    private File logFile;

    public LoggingManager(VPinataParty plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.enabled = plugin.getConfig().getBoolean("pinata.logging.enabled", true);
        String fileName = plugin.getConfig().getString("pinata.logging.file-name", "vpinataparty.log");
        this.logFile = new File(plugin.getDataFolder(), fileName);
    }

    public void log(String message) {
        if (!enabled) return;

        executor.execute(() -> {
            try {
                if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
                if (!logFile.exists()) logFile.createNewFile();

                try (PrintWriter out = new PrintWriter(new FileWriter(logFile, true))) {
                    String timestamp = LocalDateTime.now().toString().replace("T", " ").substring(0, 19);
                    out.println("[" + timestamp + "] " + message);
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Could not write to log file: " + e.getMessage());
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}
