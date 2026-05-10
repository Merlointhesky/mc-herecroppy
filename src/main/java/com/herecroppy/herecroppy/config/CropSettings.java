package com.herecroppy.herecroppy.config;

import org.bukkit.Material;

public class CropSettings {
    private final Material cropType;
    private boolean seedingEnabled;
    private boolean collectingEnabled;
    private boolean bonemealEnabled;
    private boolean junkEnabled; // true = junk (dump), false = keep (store)
    private boolean seedDumpEnabled; // true = dump seeds, false = keep seeds

    public CropSettings(Material cropType) {
        this.cropType = cropType;
        this.seedingEnabled = true;
        this.collectingEnabled = true;
        this.bonemealEnabled = false;
        this.junkEnabled = false; // Default to keep
        this.seedDumpEnabled = true; // Default to dump seeds
    }

    public CropSettings(Material cropType, boolean seeding, boolean collecting, boolean bonemeal) {
        this.cropType = cropType;
        this.seedingEnabled = seeding;
        this.collectingEnabled = collecting;
        this.bonemealEnabled = bonemeal;
        this.junkEnabled = false; // Default to keep
        this.seedDumpEnabled = true; // Default to dump seeds
    }

    public CropSettings(Material cropType, boolean seeding, boolean collecting, boolean bonemeal, boolean junk) {
        this.cropType = cropType;
        this.seedingEnabled = seeding;
        this.collectingEnabled = collecting;
        this.bonemealEnabled = bonemeal;
        this.junkEnabled = junk;
        this.seedDumpEnabled = true; // Default to dump seeds
    }

    public Material getCropType() {
        return cropType;
    }

    public boolean isSeedingEnabled() {
        return seedingEnabled;
    }

    public void setSeedingEnabled(boolean enabled) {
        this.seedingEnabled = enabled;
    }

    public void toggleSeeding() {
        this.seedingEnabled = !this.seedingEnabled;
    }

    public boolean isCollectingEnabled() {
        return collectingEnabled;
    }

    public void setCollectingEnabled(boolean enabled) {
        this.collectingEnabled = enabled;
    }

    public void toggleCollecting() {
        this.collectingEnabled = !this.collectingEnabled;
    }

    public boolean isBonemealEnabled() {
        return bonemealEnabled;
    }

    public void setBonemealEnabled(boolean enabled) {
        this.bonemealEnabled = enabled;
    }

    public void toggleBonemeal() {
        this.bonemealEnabled = !this.bonemealEnabled;
    }

    public boolean isJunkEnabled() {
        return junkEnabled;
    }

    public void setJunkEnabled(boolean enabled) {
        this.junkEnabled = enabled;
    }

    public void toggleJunk() {
        this.junkEnabled = !this.junkEnabled;
    }

    public boolean isSeedDumpEnabled() {
        return seedDumpEnabled;
    }

    public void setSeedDumpEnabled(boolean enabled) {
        this.seedDumpEnabled = enabled;
    }

    public void toggleSeedDump() {
        this.seedDumpEnabled = !this.seedDumpEnabled;
    }

    public void reset() {
        this.seedingEnabled = true;
        this.collectingEnabled = true;
        this.bonemealEnabled = false;
        this.junkEnabled = false;
        this.seedDumpEnabled = true;
    }
}
