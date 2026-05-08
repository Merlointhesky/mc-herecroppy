package com.herecroppy.herecroppy.path;

import com.herecroppy.herecroppy.map.ScanResult;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

public class PathGenerator {

    public static List<Location> generateSafePath(ScanResult scanResult) {
        List<Location> path = new ArrayList<>();

        Location pointA = scanResult.getPointA();
        Location pointB = scanResult.getPointB();
        World world = pointA.getWorld();

        int minX = Math.min(pointA.getBlockX(), pointB.getBlockX());
        int maxX = Math.max(pointA.getBlockX(), pointB.getBlockX());
        int minZ = Math.min(pointA.getBlockZ(), pointB.getBlockZ());
        int maxZ = Math.max(pointA.getBlockZ(), pointB.getBlockZ());

        boolean goingRight = true;
        for (int z = minZ; z <= maxZ; z++) {
            if (goingRight) {
                for (int x = minX; x <= maxX; x++) {
                    if (!scanResult.isObstructed(x, z)) {
                        path.add(new Location(world, x + 0.5, scanResult.getGroundY(x, z), z + 0.5));
                    }
                }
            } else {
                for (int x = maxX; x >= minX; x--) {
                    if (!scanResult.isObstructed(x, z)) {
                        path.add(new Location(world, x + 0.5, scanResult.getGroundY(x, z), z + 0.5));
                    }
                }
            }
            goingRight = !goingRight;
        }

        return path;
    }

    public static List<Location> generateSnakePath(ScanResult scanResult) {
        List<Location> path = new ArrayList<>();

        Location pointA = scanResult.getPointA();
        Location pointB = scanResult.getPointB();

        if (pointA.getWorld() != pointB.getWorld()) {
            return path;
        }

        World world = pointA.getWorld();
        int minX = Math.min(pointA.getBlockX(), pointB.getBlockX());
        int maxX = Math.max(pointA.getBlockX(), pointB.getBlockX());
        int minZ = Math.min(pointA.getBlockZ(), pointB.getBlockZ());
        int maxZ = Math.max(pointA.getBlockZ(), pointB.getBlockZ());

        boolean goingRight = true;
        for (int z = minZ; z <= maxZ; z++) {
            if (goingRight) {
                for (int x = minX; x <= maxX; x++) {
                    if (!scanResult.isObstructed(x, z)) {
                        path.add(new Location(world, x + 0.5, scanResult.getGroundY(x, z), z + 0.5));
                    }
                }
            } else {
                for (int x = maxX; x >= minX; x--) {
                    if (!scanResult.isObstructed(x, z)) {
                        path.add(new Location(world, x + 0.5, scanResult.getGroundY(x, z), z + 0.5));
                    }
                }
            }
            goingRight = !goingRight;
        }

        return path;
    }

    public static List<Location> generateSnakePath(Location pointA, Location pointB) {
        List<Location> path = new ArrayList<>();

        if (pointA.getWorld() != pointB.getWorld()) {
            return path;
        }

        World world = pointA.getWorld();
        int minX = Math.min(pointA.getBlockX(), pointB.getBlockX());
        int maxX = Math.max(pointA.getBlockX(), pointB.getBlockX());
        int minZ = Math.min(pointA.getBlockZ(), pointB.getBlockZ());
        int maxZ = Math.max(pointA.getBlockZ(), pointB.getBlockZ());
        int y = pointA.getBlockY();

        boolean goingRight = true;
        for (int z = minZ; z <= maxZ; z++) {
            if (goingRight) {
                for (int x = minX; x <= maxX; x++) {
                    path.add(new Location(world, x + 0.5, y, z + 0.5));
                }
            } else {
                for (int x = maxX; x >= minX; x--) {
                    path.add(new Location(world, x + 0.5, y, z + 0.5));
                }
            }
            goingRight = !goingRight;
        }

        return path;
    }
}
