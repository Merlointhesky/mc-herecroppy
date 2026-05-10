package com.herecroppy.herecroppy.setup;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SetupManager {
    private final Plugin plugin;
    private final File configDir;
    private final Map<UUID, SetupConfiguration> configurations = new HashMap<>();
    private final Map<UUID, Integer> setupSteps = new HashMap<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final int SETUP_TIMEOUT_TICKS = 6000; // 5 minutes

    public SetupManager(Plugin plugin) {
        this.plugin = plugin;
        this.configDir = new File(plugin.getDataFolder(), "setup-configs");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        loadConfigurations();
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
            saveConfiguration(playerId);
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

    private void saveConfiguration(UUID playerId) {
        SetupConfiguration config = configurations.get(playerId);
        if (config == null) return;

        try {
            File file = new File(configDir, playerId + ".json");
            JsonObject json = new JsonObject();
            json.addProperty("playerId", config.getPlayerId());
            json.addProperty("bonemealPerLoop", config.getBonemealPerLoop());
            json.addProperty("createdAt", config.getCreatedAt());
            json.addProperty("lastModified", config.getLastModified());

            if (config.getDumpUnwantedBox() != null) {
                json.add("dumpUnwantedBox", locationToJson(config.getDumpUnwantedBox()));
            }
            if (config.getDumpKeepBox() != null) {
                json.add("dumpKeepBox", locationToJson(config.getDumpKeepBox()));
            }
            if (config.getBonemealCollectionBox() != null) {
                json.add("bonemealCollectionBox", locationToJson(config.getBonemealCollectionBox()));
            }

            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(json, writer);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save setup configuration for " + playerId + ": " + e.getMessage());
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

                SetupConfiguration config = new SetupConfiguration(playerIdStr);
                
                if (json.has("dumpUnwantedBox")) {
                    config.setDumpUnwantedBox(jsonToLocation(json.getAsJsonObject("dumpUnwantedBox")));
                }
                if (json.has("dumpKeepBox")) {
                    config.setDumpKeepBox(jsonToLocation(json.getAsJsonObject("dumpKeepBox")));
                }
                if (json.has("bonemealCollectionBox")) {
                    config.setBonemealCollectionBox(jsonToLocation(json.getAsJsonObject("bonemealCollectionBox")));
                }
                if (json.has("bonemealPerLoop")) {
                    config.setBonemealPerLoop(json.get("bonemealPerLoop").getAsInt());
                }

                configurations.put(playerId, config);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to load setup configuration from " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    private JsonObject locationToJson(Location loc) {
        JsonObject json = new JsonObject();
        json.addProperty("world", loc.getWorld().getName());
        json.addProperty("x", loc.getBlockX());
        json.addProperty("y", loc.getBlockY());
        json.addProperty("z", loc.getBlockZ());
        return json;
    }

    private Location jsonToLocation(JsonObject json) {
        String worldName = json.get("world").getAsString();
        int x = json.get("x").getAsInt();
        int y = json.get("y").getAsInt();
        int z = json.get("z").getAsInt();
        
        org.bukkit.World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("World " + worldName + " not found");
            return null;
        }
        return new Location(world, x, y, z);
    }

    public void clearSetupConfig(UUID playerId) {
        configurations.remove(playerId);
        setupSteps.remove(playerId);
        File file = new File(configDir, playerId + ".json");
        if (file.exists()) {
            file.delete();
        }
    }
}
