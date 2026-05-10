package com.herecroppy.herecroppy.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CropConfigManager {
    private final Plugin plugin;
    private final File configDir;
    private final Map<UUID, PlayerCropConfig> playerConfigs = new HashMap<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public CropConfigManager(Plugin plugin) {
        this.plugin = plugin;
        this.configDir = new File(plugin.getDataFolder(), "crop-configs");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        loadConfigurations();
    }

    public PlayerCropConfig getPlayerConfig(UUID playerId) {
        return playerConfigs.computeIfAbsent(playerId, k -> new PlayerCropConfig(playerId.toString()));
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

    private void saveConfiguration(UUID playerId) {
        PlayerCropConfig config = playerConfigs.get(playerId);
        if (config == null) return;

        try {
            File file = new File(configDir, playerId + ".json");
            JsonObject json = new JsonObject();
            json.addProperty("playerId", config.getPlayerId());
            json.addProperty("lastModified", config.getLastModified());

            JsonObject cropSettingsJson = new JsonObject();
            for (Map.Entry<Material, CropSettings> entry : config.getAllCropSettings().entrySet()) {
                CropSettings settings = entry.getValue();
                JsonObject settingsJson = new JsonObject();
                settingsJson.addProperty("cropType", settings.getCropType().name());
                settingsJson.addProperty("seedingEnabled", settings.isSeedingEnabled());
                settingsJson.addProperty("collectingEnabled", settings.isCollectingEnabled());
                settingsJson.addProperty("bonemealEnabled", settings.isBonemealEnabled());
                settingsJson.addProperty("junkEnabled", settings.isJunkEnabled());
                settingsJson.addProperty("seedDumpEnabled", settings.isSeedDumpEnabled());
                cropSettingsJson.add(entry.getKey().name(), settingsJson);
            }
            json.add("cropSettings", cropSettingsJson);

            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(json, writer);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save crop configuration for " + playerId + ": " + e.getMessage());
        }
    }

    private void loadConfigurations() {
        if (!configDir.exists()) return;

        File[] files = configDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;

        for (File file : files) {
            try (FileReader reader = new FileReader(file)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                String playerIdStr = json.get("playerId").getAsString();
                UUID playerId = UUID.fromString(playerIdStr);

                PlayerCropConfig config = new PlayerCropConfig(playerIdStr);

                if (json.has("cropSettings")) {
                    JsonObject cropSettingsJson = json.getAsJsonObject("cropSettings");
                    for (String cropName : cropSettingsJson.keySet()) {
                        try {
                            Material cropType = Material.valueOf(cropName);
                            JsonObject settingsJson = cropSettingsJson.getAsJsonObject(cropName);
                            
                            boolean seeding = settingsJson.get("seedingEnabled").getAsBoolean();
                            boolean collecting = settingsJson.get("collectingEnabled").getAsBoolean();
                            boolean bonemeal = settingsJson.get("bonemealEnabled").getAsBoolean();
                            boolean junk = settingsJson.has("junkEnabled") ? settingsJson.get("junkEnabled").getAsBoolean() : false;
                            boolean seedDump = settingsJson.has("seedDumpEnabled") ? settingsJson.get("seedDumpEnabled").getAsBoolean() : true;
                            
                            CropSettings settings = new CropSettings(cropType, seeding, collecting, bonemeal, junk);
                            settings.setSeedDumpEnabled(seedDump);
                            config.setCropSettings(cropType, settings);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Unknown crop type: " + cropName);
                        }
                    }
                }

                if (json.has("lastModified")) {
                    config.setLastModified(json.get("lastModified").getAsLong());
                }

                playerConfigs.put(playerId, config);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to load crop configuration from " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    public void clearPlayerConfig(UUID playerId) {
        playerConfigs.remove(playerId);
        File file = new File(configDir, playerId + ".json");
        if (file.exists()) {
            file.delete();
        }
    }
}
