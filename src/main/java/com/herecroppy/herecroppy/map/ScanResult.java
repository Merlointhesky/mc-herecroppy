package com.herecroppy.herecroppy.map;

import org.bukkit.Location;

import java.util.Map;

public class ScanResult {

    private final Location pointA;
    private final Location pointB;
    private final Map<String, BlockClassification> classifications;
    private final Map<String, Integer> groundYLevels;
    private final int farmableCount;
    private final int passableCount;
    private final int doorCount;
    private final int obstructedCount;

    public ScanResult(Location pointA, Location pointB, Map<String, BlockClassification> classifications, Map<String, Integer> groundYLevels) {
        this.pointA = pointA;
        this.pointB = pointB;
        this.classifications = classifications;
        this.groundYLevels = groundYLevels;
        int farmable = 0, passable = 0, door = 0, obstructed = 0;
        for (BlockClassification bc : classifications.values()) {
            switch (bc) {
                case FARMABLE -> farmable++;
                case PASSABLE -> passable++;
                case DOOR -> door++;
                case OBSTRUCTED -> obstructed++;
            }
        }
        this.farmableCount = farmable;
        this.passableCount = passable;
        this.doorCount = door;
        this.obstructedCount = obstructed;
    }

    public Location getPointA() {
        return pointA;
    }

    public Location getPointB() {
        return pointB;
    }

    public BlockClassification getClassification(int x, int z) {
        return classifications.getOrDefault(key(x, z), BlockClassification.OBSTRUCTED);
    }

    public boolean isFarmable(int x, int z) {
        return getClassification(x, z) == BlockClassification.FARMABLE;
    }

    public boolean isPassable(int x, int z) {
        return getClassification(x, z) == BlockClassification.PASSABLE;
    }

    public boolean isObstructed(int x, int z) {
        return getClassification(x, z) == BlockClassification.OBSTRUCTED;
    }

    public boolean isDoor(int x, int z) {
        return getClassification(x, z) == BlockClassification.DOOR;
    }

    public int getFarmableCount() {
        return farmableCount;
    }

    public int getPassableCount() {
        return passableCount;
    }

    public int getObstructedCount() {
        return obstructedCount;
    }

    public int getDoorCount() {
        return doorCount;
    }

    public int getGroundY(int x, int z) {
        return groundYLevels.getOrDefault(key(x, z), pointA.getBlockY());
    }

    public int getTotalWalkable() {
        return farmableCount + passableCount + doorCount;
    }

    private static String key(int x, int z) {
        return x + "," + z;
    }
}
