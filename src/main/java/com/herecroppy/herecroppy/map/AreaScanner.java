package com.herecroppy.herecroppy.map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.HashMap;
import java.util.Map;

public class AreaScanner {

    public static ScanResult scan(Location pointA, Location pointB) {
        World world = pointA.getWorld();
        int minX = Math.min(pointA.getBlockX(), pointB.getBlockX());
        int maxX = Math.max(pointA.getBlockX(), pointB.getBlockX());
        int minZ = Math.min(pointA.getBlockZ(), pointB.getBlockZ());
        int maxZ = Math.max(pointA.getBlockZ(), pointB.getBlockZ());
        int baseY = pointA.getBlockY();

        Map<String, BlockClassification> classifications = new HashMap<>();
        Map<String, Integer> groundYLevels = new HashMap<>();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                var result = classifyColumn(world, x, baseY, z);
                classifications.put(x + "," + z, result.classification());
                if (result.groundY() != null) {
                    groundYLevels.put(x + "," + z, result.groundY());
                }
            }
        }

        return new ScanResult(pointA, pointB, classifications, groundYLevels);
    }

    private static ColumnResult classifyColumn(World world, int x, int baseY, int z) {
        // Find the top solid block near baseY (scan within +/- 2 blocks vertically)
        Block ground = findGroundBlock(world, x, baseY, z);
        if (ground == null) {
            return new ColumnResult(BlockClassification.OBSTRUCTED, null);
        }

        Material groundType = ground.getType();

        // Check headroom: need at least 2 air blocks above ground
        Block above1 = world.getBlockAt(x, ground.getY() + 1, z);
        Block above2 = world.getBlockAt(x, ground.getY() + 2, z);
        if (!isAirOrPassable(above1.getType()) || !isAirOrPassable(above2.getType())) {
            return new ColumnResult(BlockClassification.OBSTRUCTED, null);
        }

        boolean hasDoorPassage = isOpenablePassage(above1.getType()) || isOpenablePassage(above2.getType());

        // Unsafe ground materials
        if (isUnsafeGround(groundType)) {
            return new ColumnResult(BlockClassification.OBSTRUCTED, null);
        }

        if (hasDoorPassage) {
            return new ColumnResult(BlockClassification.DOOR, ground.getY());
        }

        // Farmable ground materials
        if (isFarmableGround(groundType)) {
            return new ColumnResult(BlockClassification.FARMABLE, ground.getY());
        }

        // Non-farmable but walkable terrain (paths, plain blocks) can still be traversed
        if (isWalkableGround(groundType)) {
            return new ColumnResult(BlockClassification.PASSABLE, ground.getY());
        }

        return new ColumnResult(BlockClassification.OBSTRUCTED, null);
    }

    private record ColumnResult(BlockClassification classification, Integer groundY) {
    }

    private static Block findGroundBlock(World world, int x, int baseY, int z) {
        // Prefer baseY, then below, then above (avoid selecting decorations above farmland)
        for (int dy = 0; dy <= 2; dy++) {
            int[] yCandidates = dy == 0
                    ? new int[]{baseY}
                    : new int[]{baseY - dy, baseY + dy};
            for (int y : yCandidates) {
                Block block = world.getBlockAt(x, y, z);
                if (block.getType().isSolid() && !isUnsafeGround(block.getType())) {
                    return block;
                }
            }
        }
        // Fallback: scan downward from baseY to find first solid
        for (int y = baseY; y >= Math.max(0, baseY - 5); y--) {
            Block block = world.getBlockAt(x, y, z);
            if (block.getType().isSolid() && !isUnsafeGround(block.getType())) {
                return block;
            }
        }
        return null;
    }

    private static boolean isUnsafeGround(Material material) {
        return switch (material) {
            case LAVA, LAVA_CAULDRON, WATER, WATER_CAULDRON, ICE, PACKED_ICE, BLUE_ICE,
                 MAGMA_BLOCK, CACTUS, FIRE, SOUL_FIRE, CAMPFIRE, SOUL_CAMPFIRE,
                 LANTERN, SOUL_LANTERN, TORCH, SOUL_TORCH, WALL_TORCH -> true;
            default -> false;
        };
    }

    private static boolean isFarmableGround(Material material) {
        return switch (material) {
            case DIRT, GRASS_BLOCK, COARSE_DIRT, PODZOL, MYCELIUM, ROOTED_DIRT,
                 FARMLAND, SOUL_SAND -> true;
            default -> false;
        };
    }

    private static boolean isAirOrPassable(Material material) {
        return material.isAir()
                || material == Material.CAVE_AIR
                || material == Material.VOID_AIR
                || Tag.CROPS.isTagged(material)
                || material == Material.SUGAR_CANE
                || material == Material.PUMPKIN
                || material == Material.MELON
                || material == Material.NETHER_WART
                || material == Material.ATTACHED_PUMPKIN_STEM
                || material == Material.ATTACHED_MELON_STEM
                || isOpenablePassage(material);
    }

    private static boolean isWalkableGround(Material material) {
        if (!material.isSolid() || material.isInteractable()) {
            return false;
        }
        if (Tag.STAIRS.isTagged(material)
                || Tag.SLABS.isTagged(material)
                || Tag.FENCES.isTagged(material)
                || Tag.WALLS.isTagged(material)
                || Tag.TRAPDOORS.isTagged(material)
                || Tag.FENCE_GATES.isTagged(material)) {
            return false;
        }
        return true;
    }

    private static boolean isOpenablePassage(Material material) {
        return Tag.DOORS.isTagged(material)
                || Tag.FENCE_GATES.isTagged(material)
                || Tag.TRAPDOORS.isTagged(material);
    }
}
