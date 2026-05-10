package com.herecroppy.herecroppy;

import com.herecroppy.herecroppy.auraskills.AuraSkillsHelper;
import com.herecroppy.herecroppy.command.HereCroppyCommand;
import com.herecroppy.herecroppy.command.SetupWizardCommand;
import com.herecroppy.herecroppy.command.CropConfigCommand;
import com.herecroppy.herecroppy.config.CropConfigManager;
import com.herecroppy.herecroppy.config.CropConfigUI;
import com.herecroppy.herecroppy.config.CropConfigListener;
import com.herecroppy.herecroppy.listener.FarmListener;
import com.herecroppy.herecroppy.listener.SetupWizardListener;
import com.herecroppy.herecroppy.map.ScanManager;
import com.herecroppy.herecroppy.selection.SelectionManager;
import com.herecroppy.herecroppy.setup.SetupManager;
import com.herecroppy.herecroppy.task.FarmTaskManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class HereCroppyPlugin extends JavaPlugin {

    private static HereCroppyPlugin instance;
    private final SelectionManager selectionManager = new SelectionManager();
    private final FarmTaskManager farmTaskManager = new FarmTaskManager();
    private final AuraSkillsHelper auraSkillsHelper = new AuraSkillsHelper();
    private final ScanManager scanManager = new ScanManager(this);
    private final SetupManager setupManager = new SetupManager(this);
    private final CropConfigManager cropConfigManager = new CropConfigManager(this);
    private final CropConfigUI cropConfigUI = new CropConfigUI(cropConfigManager);
    private SetupWizardCommand setupWizardCommand;

    @Override
    public void onEnable() {
        instance = this;
        auraSkillsHelper.init();
        
        // Create setup wizard command
        setupWizardCommand = new SetupWizardCommand(setupManager, this);
        
        // Register main command with all subcommands
        getCommand("herecroppy").setExecutor(new HereCroppyCommand(selectionManager, farmTaskManager, auraSkillsHelper, scanManager, setupWizardCommand, cropConfigUI));
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new FarmListener(selectionManager, farmTaskManager, scanManager, setupManager), this);
        getServer().getPluginManager().registerEvents(new SetupWizardListener(setupManager), this);
        getServer().getPluginManager().registerEvents(setupWizardCommand, this);
        getServer().getPluginManager().registerEvents(new CropConfigListener(cropConfigUI, cropConfigManager), this);
        
        // Start setup timeout checker
        new BukkitRunnable() {
            @Override
            public void run() {
                setupWizardCommand.checkTimeouts();
            }
        }.runTaskTimer(this, 0, 20); // Check every second
        
        getLogger().info("HereCroppy enabled!");
    }

    @Override
    public void onDisable() {
        farmTaskManager.stopAllTasks();
        getLogger().info("HereCroppy disabled!");
    }

    public static HereCroppyPlugin getInstance() {
        return instance;
    }

    public SelectionManager getSelectionManager() {
        return selectionManager;
    }

    public FarmTaskManager getFarmTaskManager() {
        return farmTaskManager;
    }

    public AuraSkillsHelper getAuraSkillsHelper() {
        return auraSkillsHelper;
    }

    public SetupManager getSetupManager() {
        return setupManager;
    }

    public CropConfigManager getCropConfigManager() {
        return cropConfigManager;
    }

    public CropConfigUI getCropConfigUI() {
        return cropConfigUI;
    }
}
