package com.herecroppy.herecroppy.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CropConfigManager {
    private final Plugin plugin;
    private final File configDir;
    private final Map<UUID, PlayerCropConfig> playerConfigs = new HashMap<>();

    public CropConfigManager(Plugin plugin) {
        this.plugin = plugin;
        this.configDir = new File(plugin.getDataFolder(), "player-configs");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
    }

    public PlayerCropConfig getPlayerConfig(UUID playerId) {
        if (!playerConfigs.containsKey(playerId)) {
            loadPlayerConfig(playerId);
        }
        return playerConfigs.get(playerId);
    }

    public void loadPlayerConfig(UUID playerId) {
        File file = new File(configDir, playerId + ".yml");
        PlayerCropConfig config = new PlayerCropConfig(playerId.toString());

        if (file.exists()) {
            FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            
            if (yaml.contains("cropSettings")) {
                org.bukkit.configuration.ConfigurationSection cropSection = yaml.getConfigurationSection("cropSettings");
                for (String cropName : cropSection.getKeys(false)) {
                    try {
                        Material cropType = Material.valueOf(cropName);
                        org.bukkit.configuration.ConfigurationSection settingsSection = cropSection.getConfigurationSection(cropName);
                        
                        boolean seeding = settingsSection.getBoolean("seedingEnabled", true);
                        boolean collecting = settingsSection.getBoolean("collectingEnabled", true);
                        boolean bonemeal = settingsSection.getBoolean("bonemealEnabled", false);
                        boolean junk = settingsSection.getBoolean("junkEnabled", false);
                        boolean seedDump = settingsSection.getBoolean("seedDumpEnabled", true);
                        
                        CropSettings settings = new CropSettings(cropType, seeding, collecting, bonemeal, junk);
                        settings.setSeedDumpEnabled(seedDump);
                        config.setCropSettings(cropType, settings);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Unknown crop type in config: " + cropName);
                    }
                }
            }
            
            if (yaml.contains("lastModified")) {
                config.setLastModified(yaml.getLong("lastModified"));
            }
        }

        playerConfigs.put(playerId, config);
    }

    public CropSettings getCropSettings(UUID playerId, Material cropType) {
        PlayerCropConfig config = getPlayerConfig(playerId);
        return config.getCropSettings(cropType);
    }

    public boolean isSeedingEnabled(UUID playerId, Material cropType) {
        return getCropSettings(playerId, cropType).isSeedingEnabled();
    }

    public boolean isCollectingEnabled(UUID playerId, Material cropType) {
        return getCropSettings(playerId, cropType).isCollectingEnabled();
    }

    public boolean isBonemealEnabled(UUID playerId, Material cropType) {
        return getCropSettings(playerId, cropType).isBonemealEnabled();
    }

    public boolean isJunkEnabled(UUID playerId, Material cropType) {
        return getCropSettings(playerId, cropType).isJunkEnabled();
    }

    public boolean isSeedDumpEnabled(UUID playerId, Material cropType) {
        return getCropSettings(playerId, cropType).isSeedDumpEnabled();
    }

    public void toggleSeeding(UUID playerId, Material cropType) {
        PlayerCropConfig config = getPlayerConfig(playerId);
        config.toggleSeeding(cropType);
        saveConfiguration(playerId);
    }

    public void toggleCollecting(UUID playerId, Material cropType) {
        PlayerCropConfig config = getPlayerConfig(playerId);
        config.toggleCollecting(cropType);
        saveConfiguration(playerId);
    }

    public void toggleBonemeal(UUID playerId, Material cropType) {
        PlayerCropConfig config = getPlayerConfig(playerId);
        config.toggleBonemeal(cropType);
        saveConfiguration(playerId);
    }

    public void toggleJunk(UUID playerId, Material cropType) {
        PlayerCropConfig config = getPlayerConfig(playerId);
        config.toggleJunk(cropType);
        saveConfiguration(playerId);
    }

    public void toggleSeedDump(UUID playerId, Material cropType) {
        PlayerCropConfig config = getPlayerConfig(playerId);
        config.toggleSeedDump(cropType);
        saveConfiguration(playerId);
    }

    public void resetToDefaults(UUID playerId) {
        PlayerCropConfig config = getPlayerConfig(playerId);
        config.resetToDefaults();
        saveConfiguration(playerId);
    }

    public void saveConfiguration(UUID playerId) {
        PlayerCropConfig config = playerConfigs.get(playerId);
        if (config == null) return;

        File file = new File(configDir, playerId + ".yml");
        FileConfiguration yaml = new YamlConfiguration();

        yaml.set("playerId", config.getPlayerId());
        yaml.set("lastModified", config.getLastModified());

        for (Map.Entry<Material, CropSettings> entry : config.getAllCropSettings().entrySet()) {
            String path = "cropSettings." + entry.getKey().name();
            CropSettings settings = entry.getValue();
            yaml.set(path + ".seedingEnabled", settings.isSeedingEnabled());
            yaml.set(path + ".collectingEnabled", settings.isCollectingEnabled());
            yaml.set(path + ".bonemealEnabled", settings.isBonemealEnabled());
            yaml.set(path + ".junkEnabled", settings.isJunkEnabled());
            yaml.set(path + ".seedDumpEnabled", settings.isSeedDumpEnabled());
        }

        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save crop configuration for " + playerId + ": " + e.getMessage());
        }
    }

    public void clearPlayerConfig(UUID playerId) {
        playerConfigs.remove(playerId);
        File file = new File(configDir, playerId + ".yml");
        if (file.exists()) {
            file.delete();
        }
    }
}
