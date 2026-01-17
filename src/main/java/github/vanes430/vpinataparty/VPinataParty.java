package github.vanes430.vpinataparty;

import github.vanes430.vpinataparty.commands.PinataCommand;
import github.vanes430.vpinataparty.managers.LoggingManager;
import github.vanes430.vpinataparty.managers.MessageManager;
import github.vanes430.vpinataparty.managers.PinataManager;
import github.vanes430.vpinataparty.utils.SchedulerUtils;
import org.bukkit.plugin.java.JavaPlugin;

public class VPinataParty extends JavaPlugin {

    private LoggingManager loggingManager;

    @Override
    public void onEnable() {
        /* Initialize Utilities & Managers */
        SchedulerUtils scheduler = new SchedulerUtils(this);
        saveDefaultConfig();
        
        this.loggingManager = new LoggingManager(this);
        MessageManager messageManager = new MessageManager(this);
        PinataManager pinataManager = new PinataManager(this, scheduler, loggingManager);

        /* Register Commands */
        PinataCommand pinataCommand = new PinataCommand(this, messageManager, pinataManager, scheduler);
        getCommand("pinata").setExecutor(pinataCommand);
        getCommand("pinata").setTabCompleter(pinataCommand);
        
        /* Register Listeners */
        getServer().getPluginManager().registerEvents(new PinataListener(this, scheduler, pinataManager), this);
        
        getLogger().info("VPinataParty has been enabled!");
    }

    @Override
    public void onDisable() {
        if (loggingManager != null) {
            loggingManager.shutdown();
        }
    }

    public LoggingManager getLoggingManager() {
        return loggingManager;
    }
}