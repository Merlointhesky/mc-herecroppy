package com.herecroppy.herecroppy.setup;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SetupManager {
    private final Plugin plugin;
    private final File setupDir;
    private final Map<UUID, SetupConfiguration> configurations = new HashMap<>();
    private final Map<UUID, Integer> setupSteps = new HashMap<>();
    private static final int SETUP_TIMEOUT_TICKS = 6000; // 5 minutes

    public SetupManager(Plugin plugin) {
        this.plugin = plugin;
        this.setupDir = new File(plugin.getDataFolder(), "setup-configs");
        if (!setupDir.exists()) {
            setupDir.mkdirs();
        }
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

    public void updateBonemealAmount(UUID playerId, int amount) {
        SetupConfiguration config = getSetupConfig(playerId);
        if (config == null) {
            config = new SetupConfiguration(playerId.toString());
            configurations.put(playerId, config);
        }
        config.setBonemealPerLoop(amount);
        saveConfiguration(playerId);
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
            saveConfiguration(playerId);
        }
    }

    public void cancelSetup(UUID playerId) {
        configurations.remove(playerId);
        setupSteps.remove(playerId);
    }

    public SetupConfiguration getSetupConfig(UUID playerId) {
        if (!configurations.containsKey(playerId)) {
            loadConfiguration(playerId);
        }
        return configurations.get(playerId);
    }

    public boolean hasSetupConfig(UUID playerId) {
        SetupConfiguration config = getSetupConfig(playerId);
        return config != null && config.isComplete();
    }

    public void clearSetupConfig(UUID playerId) {
        configurations.remove(playerId);
        setupSteps.remove(playerId);
        File file = new File(setupDir, playerId + ".yml");
        if (file.exists()) {
            file.delete();
        }
    }

    public void saveConfiguration(UUID playerId) {
        SetupConfiguration config = configurations.get(playerId);
        if (config == null) return;

        File file = new File(setupDir, playerId + ".yml");
        FileConfiguration yaml = new YamlConfiguration();

        yaml.set("playerId", config.getPlayerId());
        yaml.set("dumpUnwantedBox", config.getDumpUnwantedBox());
        yaml.set("dumpKeepBox", config.getDumpKeepBox());
        yaml.set("bonemealCollectionBox", config.getBonemealCollectionBox());
        yaml.set("bonemealPerLoop", config.getBonemealPerLoop());
        yaml.set("createdAt", config.getCreatedAt());
        yaml.set("lastModified", config.getLastModified());

        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save setup configuration for " + playerId + ": " + e.getMessage());
        }
    }

    public void loadConfiguration(UUID playerId) {
        File file = new File(setupDir, playerId + ".yml");
        if (!file.exists()) return;

        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        SetupConfiguration config = new SetupConfiguration(playerId.toString());

        config.setDumpUnwantedBox(yaml.getLocation("dumpUnwantedBox"));
        config.setDumpKeepBox(yaml.getLocation("dumpKeepBox"));
        config.setBonemealCollectionBox(yaml.getLocation("bonemealCollectionBox"));
        config.setBonemealPerLoop(yaml.getInt("bonemealPerLoop"));
        // createdAt/lastModified could be loaded too if needed, but not strictly necessary for functionality

        configurations.put(playerId, config);
    }
}
