package com.herecroppy.herecroppy.setup;

import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SetupManager {
    private final Plugin plugin;
    private final Map<UUID, SetupConfiguration> configurations = new HashMap<>();
    private final Map<UUID, Integer> setupSteps = new HashMap<>();
    private static final int SETUP_TIMEOUT_TICKS = 6000; // 5 minutes

    public SetupManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void startSetup(UUID playerId) {
        configurations.put(playerId, new SetupConfiguration(playerId.toString()));
        setupSteps.put(playerId, 0);
    }

    public int getCurrentStep(UUID playerId) {
        return setupSteps.getOrDefault(playerId, -1);
    }

    public boolean isInSetup(UUID playerId) {
        return setupSteps.containsKey(playerId) && setupSteps.get(playerId) >= 0;
    }

    public void setDumpUnwantedBox(UUID playerId, Location location) {
        if (!isInSetup(playerId)) return;
        SetupConfiguration config = configurations.get(playerId);
        config.setDumpUnwantedBox(location);
        setupSteps.put(playerId, 1);
    }

    public void setDumpKeepBox(UUID playerId, Location location) {
        if (!isInSetup(playerId)) return;
        SetupConfiguration config = configurations.get(playerId);
        config.setDumpKeepBox(location);
        setupSteps.put(playerId, 2);
    }

    public void setBonemealBox(UUID playerId, Location location) {
        if (!isInSetup(playerId)) return;
        SetupConfiguration config = configurations.get(playerId);
        config.setBonemealCollectionBox(location);
        setupSteps.put(playerId, 3);
    }

    public void setBonemealPerLoop(UUID playerId, int amount) {
        if (!isInSetup(playerId)) return;
        SetupConfiguration config = configurations.get(playerId);
        config.setBonemealPerLoop(amount);
        completeSetup(playerId);
    }

    public void completeSetup(UUID playerId) {
        SetupConfiguration config = configurations.get(playerId);
        if (config != null && config.isComplete()) {
            setupSteps.remove(playerId);
            // We no longer save setup configuration to disk as per user request
        }
    }

    public void cancelSetup(UUID playerId) {
        configurations.remove(playerId);
        setupSteps.remove(playerId);
    }

    public SetupConfiguration getSetupConfig(UUID playerId) {
        return configurations.get(playerId);
    }

    public boolean hasSetupConfig(UUID playerId) {
        return configurations.containsKey(playerId) && 
               configurations.get(playerId).isComplete();
    }

    public void clearSetupConfig(UUID playerId) {
        configurations.remove(playerId);
        setupSteps.remove(playerId);
    }
}
