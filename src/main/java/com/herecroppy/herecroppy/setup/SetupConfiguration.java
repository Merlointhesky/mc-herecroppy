package com.herecroppy.herecroppy.setup;

import org.bukkit.Location;

public class SetupConfiguration {
    private final String playerId;
    private Location dumpUnwantedBox;
    private Location dumpKeepBox;
    private Location bonemealCollectionBox;
    private int bonemealPerLoop;
    private long createdAt;
    private long lastModified;

    public SetupConfiguration(String playerId) {
        this.playerId = playerId;
        this.createdAt = System.currentTimeMillis();
        this.lastModified = System.currentTimeMillis();
    }

    public String getPlayerId() {
        return playerId;
    }

    public Location getDumpUnwantedBox() {
        return dumpUnwantedBox;
    }

    public void setDumpUnwantedBox(Location location) {
        this.dumpUnwantedBox = location;
        this.lastModified = System.currentTimeMillis();
    }

    public Location getDumpKeepBox() {
        return dumpKeepBox;
    }

    public void setDumpKeepBox(Location location) {
        this.dumpKeepBox = location;
        this.lastModified = System.currentTimeMillis();
    }

    public Location getBonemealCollectionBox() {
        return bonemealCollectionBox;
    }

    public void setBonemealCollectionBox(Location location) {
        this.bonemealCollectionBox = location;
        this.lastModified = System.currentTimeMillis();
    }

    public int getBonemealPerLoop() {
        return bonemealPerLoop;
    }

    public void setBonemealPerLoop(int amount) {
        this.bonemealPerLoop = amount;
        this.lastModified = System.currentTimeMillis();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getLastModified() {
        return lastModified;
    }

    public boolean isComplete() {
        return dumpUnwantedBox != null && dumpKeepBox != null && 
               bonemealCollectionBox != null && bonemealPerLoop > 0;
    }

    public void reset() {
        this.dumpUnwantedBox = null;
        this.dumpKeepBox = null;
        this.bonemealCollectionBox = null;
        this.bonemealPerLoop = 0;
        this.lastModified = System.currentTimeMillis();
    }
}
