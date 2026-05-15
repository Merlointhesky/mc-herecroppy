package com.herecroppy.herecroppy.config;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerCropConfig {
    private final String playerId;
    private final Map<Material, CropSettings> cropSettings;
    private List<Material> seedingPriority;
    private long lastModified;

    public PlayerCropConfig(String playerId) {
        this.playerId = playerId;
        this.cropSettings = new HashMap<>();
        this.seedingPriority = new ArrayList<>();
        this.lastModified = System.currentTimeMillis();
        initializeDefaults();
    }

    private void initializeDefaults() {
    	// Farmland crops
    	cropSettings.put(Material.WHEAT, new CropSettings(Material.WHEAT, true, true, true));
    	cropSettings.put(Material.CARROT, new CropSettings(Material.CARROT, true, true, true));
    	cropSettings.put(Material.POTATO, new CropSettings(Material.POTATO, true, true, true));
    	cropSettings.put(Material.BEETROOT, new CropSettings(Material.BEETROOT, true, true, true));
   
    	// Special crops
    	cropSettings.put(Material.NETHER_WART, new CropSettings(Material.NETHER_WART, true, true, true));
    	cropSettings.put(Material.SUGAR_CANE, new CropSettings(Material.SUGAR_CANE, true, true, false));
    	cropSettings.put(Material.PUMPKIN, new CropSettings(Material.PUMPKIN, false, true, true));
    	cropSettings.put(Material.MELON, new CropSettings(Material.MELON, false, true, true));

        // Default seeding priority
        seedingPriority.clear();
        seedingPriority.add(Material.NETHER_WART);
        seedingPriority.add(Material.WHEAT);
        seedingPriority.add(Material.CARROT);
        seedingPriority.add(Material.POTATO);
        seedingPriority.add(Material.BEETROOT);
        seedingPriority.add(Material.PUMPKIN);
        seedingPriority.add(Material.MELON);
    }

    public String getPlayerId() {
        return playerId;
    }

    public CropSettings getCropSettings(Material cropType) {
        return cropSettings.getOrDefault(cropType, new CropSettings(cropType));
    }

    public void setCropSettings(Material cropType, CropSettings settings) {
        cropSettings.put(cropType, settings);
        this.lastModified = System.currentTimeMillis();
    }

    public Map<Material, CropSettings> getAllCropSettings() {
        return new HashMap<>(cropSettings);
    }

    public void toggleSeeding(Material cropType) {
        CropSettings settings = cropSettings.get(cropType);
        if (settings != null) {
            settings.toggleSeeding();
            this.lastModified = System.currentTimeMillis();
        }
    }

    public void toggleCollecting(Material cropType) {
        CropSettings settings = cropSettings.get(cropType);
        if (settings != null) {
            settings.toggleCollecting();
            this.lastModified = System.currentTimeMillis();
        }
    }

    public void toggleBonemeal(Material cropType) {
        CropSettings settings = cropSettings.get(cropType);
        if (settings != null) {
            settings.toggleBonemeal();
            this.lastModified = System.currentTimeMillis();
        }
    }

    public void toggleJunk(Material cropType) {
        CropSettings settings = cropSettings.get(cropType);
        if (settings != null) {
            settings.toggleJunk();
            this.lastModified = System.currentTimeMillis();
        }
    }

    public void toggleSeedDump(Material cropType) {
        CropSettings settings = cropSettings.get(cropType);
        if (settings != null) {
            settings.toggleSeedDump();
            this.lastModified = System.currentTimeMillis();
        }
    }

    public void resetToDefaults() {
        cropSettings.clear();
        initializeDefaults();
        this.lastModified = System.currentTimeMillis();
    }

    public List<Material> getSeedingPriority() {
        return new ArrayList<>(seedingPriority);
    }

    public void setSeedingPriority(List<Material> priority) {
        this.seedingPriority = new ArrayList<>(priority);
        this.lastModified = System.currentTimeMillis();
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long timestamp) {
        this.lastModified = timestamp;
    }
}
