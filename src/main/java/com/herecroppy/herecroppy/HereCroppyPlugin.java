package com.herecroppy.herecroppy;

import com.herecroppy.herecroppy.auraskills.AuraSkillsHelper;
import com.herecroppy.herecroppy.command.HereCroppyCommand;
import com.herecroppy.herecroppy.listener.FarmListener;
import com.herecroppy.herecroppy.map.ScanManager;
import com.herecroppy.herecroppy.selection.SelectionManager;
import com.herecroppy.herecroppy.task.FarmTaskManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class HereCroppyPlugin extends JavaPlugin {

    private static HereCroppyPlugin instance;
    private final SelectionManager selectionManager = new SelectionManager();
    private final FarmTaskManager farmTaskManager = new FarmTaskManager();
    private final AuraSkillsHelper auraSkillsHelper = new AuraSkillsHelper();
    private final ScanManager scanManager = new ScanManager(this);

    @Override
    public void onEnable() {
        instance = this;
        auraSkillsHelper.init();
        getCommand("herecroppy").setExecutor(new HereCroppyCommand(selectionManager, farmTaskManager, auraSkillsHelper, scanManager));
        getServer().getPluginManager().registerEvents(new FarmListener(selectionManager, farmTaskManager, scanManager), this);
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
}
