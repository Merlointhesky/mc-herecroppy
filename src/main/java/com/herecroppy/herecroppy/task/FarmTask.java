package com.herecroppy.herecroppy.task;

import com.herecroppy.herecroppy.HereCroppyPlugin;
import com.herecroppy.herecroppy.auraskills.AuraSkillsHelper;
import com.herecroppy.herecroppy.map.ScanManager;
import com.herecroppy.herecroppy.map.ScanResult;
import com.herecroppy.herecroppy.path.PathGenerator;
import com.herecroppy.herecroppy.selection.SelectionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class FarmTask extends BukkitRunnable {

    private static final double SPEED = 0.25;
    private static final double SNAP_DISTANCE = 0.3;
    private static final double MAX_DIRECT_STEP_DISTANCE = 1.75;
    private static final double BASE_HARVEST_XP = 10.0;
    private static final int HARVEST_PAUSE_TICKS = 5;
    private static final int STUCK_TICK_THRESHOLD = 12;

    private final HereCroppyPlugin plugin;
    private final Player player;
    private final List<Location> path;
    private final AuraSkillsHelper auraSkillsHelper;
    private final ScanManager scanManager;
    private final SelectionManager selectionManager;
    private ScanResult scanResult;
    private int currentIndex = 0;
    private int harvestPause = 0;
    private Location previousVisitedTarget;
    private int lastTargetIndex = -1;
    private double lastHorizontalDist = Double.MAX_VALUE;
    private int stuckTicks = 0;
    private final Random random = new Random();
    private final Map<Material, Material> seedMap;
    private final Map<Material, Material> cropProductMap;
    private final Set<String> passagesOpenedByBot = new HashSet<>();

    public FarmTask(HereCroppyPlugin plugin, Player player, List<Location> path,
                    AuraSkillsHelper auraSkillsHelper, ScanManager scanManager,
                    SelectionManager selectionManager, ScanResult scanResult) {
        this.plugin = plugin;
        this.player = player;
        this.path = path;
        this.auraSkillsHelper = auraSkillsHelper;
        this.scanManager = scanManager;
        this.selectionManager = selectionManager;
        this.scanResult = scanResult;

        this.seedMap = new HashMap<>();
        seedMap.put(Material.WHEAT, Material.WHEAT_SEEDS);
        seedMap.put(Material.CARROTS, Material.CARROT);
        seedMap.put(Material.POTATOES, Material.POTATO);
        seedMap.put(Material.BEETROOTS, Material.BEETROOT_SEEDS);
        seedMap.put(Material.NETHER_WART, Material.NETHER_WART);

        this.cropProductMap = new HashMap<>();
        cropProductMap.put(Material.WHEAT, Material.WHEAT);
        cropProductMap.put(Material.CARROTS, Material.CARROT);
        cropProductMap.put(Material.POTATOES, Material.POTATO);
        cropProductMap.put(Material.BEETROOTS, Material.BEETROOT);
        cropProductMap.put(Material.NETHER_WART, Material.NETHER_WART);
    }

    public HereCroppyPlugin getPlugin() {
        return plugin;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int currentIndex) {
        this.currentIndex = currentIndex;
    }

    @Override
    public void run() {
        if (!player.isOnline()) {
            cancel();
            return;
        }

        if (isInventoryFull()) {
            cancel();
            FarmTaskManager manager = plugin.getFarmTaskManager();
            manager.recordInventoryFullStop(player, currentIndex);
            player.sendMessage(Component.text("Inventory full! Auto-farm paused. Use /herecroppy restart to resume.")
                    .color(NamedTextColor.RED));
            return;
        }

        if (path.isEmpty()) {
            cancel();
            return;
        }

        // If we're pausing after a harvest, count down
        if (harvestPause > 0) {
            harvestPause--;
            return;
        }

        Location target = path.get(currentIndex);
        Location current = player.getLocation();

        if (current.getWorld() != target.getWorld()) {
            cancel();
            player.sendMessage(Component.text("Auto-farming stopped — you left the farming area.")
                    .color(NamedTextColor.RED));
            return;
        }

        double dx = target.getX() - current.getX();
        double dz = target.getZ() - current.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        tryOpenPassageAt(target);

        if (currentIndex != lastTargetIndex) {
            lastTargetIndex = currentIndex;
            lastHorizontalDist = horizontalDist;
            stuckTicks = 0;
        } else {
            if (horizontalDist >= lastHorizontalDist - 0.02) {
                stuckTicks++;
            } else {
                stuckTicks = 0;
            }
            lastHorizontalDist = horizontalDist;
        }

        if (horizontalDist > MAX_DIRECT_STEP_DISTANCE || stuckTicks >= STUCK_TICK_THRESHOLD) {
            // Recovery for blocked transitions (e.g., skipped obstructed cells or closed passage collisions).
            teleportToTarget(current, target);
            stuckTicks = 0;
            return;
        }

        if (horizontalDist < SNAP_DISTANCE) {
            // Arrived at target - process crop (keep current Y to avoid going underground)
            teleportToTarget(current, target);

            // Close passages behind us (only blocks this bot opened earlier).
            if (previousVisitedTarget != null) {
                tryClosePassageAt(previousVisitedTarget);
            }
            previousVisitedTarget = target;

            int blockX = target.getBlockX();
            int blockZ = target.getBlockZ();
            if (scanResult != null && !scanResult.isFarmable(blockX, blockZ)) {
                // Passable but not farmable: just walk through
            } else {
                boolean harvested = processCurrentBlock(target);
                if (harvested) {
                    harvestPause = HARVEST_PAUSE_TICKS;
                }
            }

            currentIndex++;
            if (currentIndex >= path.size()) {
                currentIndex = 0;
                triggerRescan();
            }
        } else {
            // Move toward target using velocity
            Vector direction = new Vector(dx, 0, dz).normalize();
            double speedMultiplier = 1.0 + (auraSkillsHelper.getFarmingLevel(player) * 0.01);
            Vector velocity = direction.multiply(SPEED * speedMultiplier);
            velocity.setY(0);
            player.setVelocity(velocity);
        }
    }

    private void teleportToTarget(Location current, Location target) {
        Location snap = target.clone();
        snap.setY(target.getY() + 1.0);
        snap.setPitch(current.getPitch());
        snap.setYaw(current.getYaw());
        player.teleport(snap);
    }

    private void triggerRescan() {
        if (scanManager == null || selectionManager == null || !selectionManager.hasCompleteSelection(player.getUniqueId())) {
            return;
        }
        Location pointA = selectionManager.getPointA(player.getUniqueId());
        Location pointB = selectionManager.getPointB(player.getUniqueId());
        scanManager.scanAreaAsync(player.getUniqueId(), pointA, pointB, result -> {
            scanResult = result;
            List<Location> newPath = PathGenerator.generateSafePath(result);
            if (!newPath.isEmpty()) {
                path.clear();
                path.addAll(newPath);
            }
        });
    }

    private void tryOpenPassageAt(Location target) {
        int x = target.getBlockX();
        int z = target.getBlockZ();
        int groundY = scanResult != null ? scanResult.getGroundY(x, z) : target.getBlockY();
        tryOpenIfOpenable(x, groundY + 1, z);
        tryOpenIfOpenable(x, groundY + 2, z);
    }

    private void tryClosePassageAt(Location target) {
        int x = target.getBlockX();
        int z = target.getBlockZ();
        int groundY = scanResult != null ? scanResult.getGroundY(x, z) : target.getBlockY();
        tryCloseIfOpenedByBot(x, groundY + 1, z);
        tryCloseIfOpenedByBot(x, groundY + 2, z);
    }

    private void tryOpenIfOpenable(int x, int y, int z) {
        Block block = player.getWorld().getBlockAt(x, y, z);
        Material type = block.getType();
        if (!isOpenablePassage(type)) {
            return;
        }
        if (block.getBlockData() instanceof Openable openable && !openable.isOpen()) {
            openable.setOpen(true);
            block.setBlockData(openable);
            passagesOpenedByBot.add(passageKey(x, y, z));
        }
    }

    private void tryCloseIfOpenedByBot(int x, int y, int z) {
        String key = passageKey(x, y, z);
        if (!passagesOpenedByBot.contains(key)) {
            return;
        }
        Block block = player.getWorld().getBlockAt(x, y, z);
        if (!isOpenablePassage(block.getType())) {
            passagesOpenedByBot.remove(key);
            return;
        }
        if (block.getBlockData() instanceof Openable openable) {
            if (openable.isOpen()) {
                openable.setOpen(false);
                block.setBlockData(openable);
            }
            passagesOpenedByBot.remove(key);
        }
    }

    private boolean processCurrentBlock(Location loc) {
        // Determine ground and above blocks, handling path points at crop Y-level
        Block ground = loc.getBlock();
        Block above = loc.clone().add(0, 1, 0).getBlock();

        if (!isFarmGround(ground)) {
            ground = loc.clone().subtract(0, 1, 0).getBlock();
            above = loc.getBlock();
        }

        // 1. Is there a crop? Check ripeness / bonemeal
        Block cropBlock = findCropBlock(loc);
        if (cropBlock != null && isCrop(cropBlock)) {
            if (cropBlock.getBlockData() instanceof Ageable ageable) {
                if (ageable.getAge() == ageable.getMaximumAge()) {
                    return harvestCrop(cropBlock);
                }

                // Crop is not ripe — try bonemeal to speed it up
                if (hasItem(Material.BONE_MEAL)) {
                    removeOneItem(Material.BONE_MEAL);
                    cropBlock.applyBoneMeal(org.bukkit.block.BlockFace.UP);
                    // Re-check after bonemeal
                    if (cropBlock.getBlockData() instanceof Ageable after) {
                        if (after.getAge() == after.getMaximumAge()) {
                            return harvestCrop(cropBlock);
                        }
                    }
                }
            }
            return false;
        }

        boolean didSomething = false;

        // 2. Is it prepared? If not, till it
        if (isTillable(ground) && above.getType().isAir()) {
            if (tryTill(ground)) {
                didSomething = true;
            }
        }

        // 3. Is it seeded? If not, plant
        if ((ground.getType() == Material.FARMLAND || ground.getType() == Material.SOUL_SAND)
                && above.getType().isAir()) {
            if (tryPlant(ground, above)) {
                didSomething = true;
            }
        }

        return didSomething;
    }

    private Block findCropBlock(Location loc) {
        Block blockAtLoc = loc.getBlock();
        if (isCrop(blockAtLoc)) return blockAtLoc;

        Block blockAbove = loc.clone().add(0, 1, 0).getBlock();
        if (isCrop(blockAbove)) return blockAbove;

        return null;
    }

    private boolean isCrop(Block block) {
        return block.getBlockData() instanceof Ageable;
    }

    private int calculateDropMultiplier() {
        double fortuneBonus = auraSkillsHelper.getFarmingFortune(player);
        double chance = fortuneBonus * 0.01; // 1% per fortune point
        if (random.nextDouble() < chance) {
            return 2;
        }
        return 1;
    }

    private boolean isInventoryFull() {
        return player.getInventory().firstEmpty() == -1;
    }

    private boolean hasItem(Material material) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material && item.getAmount() > 0) {
                return true;
            }
        }
        return false;
    }

    private void removeOneItem(Material material) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material && item.getAmount() > 0) {
                item.setAmount(item.getAmount() - 1);
                return;
            }
        }
    }

    private String formatName(String materialName) {
        String[] words = materialName.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!result.isEmpty()) result.append(" ");
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    private boolean harvestCrop(Block cropBlock) {
        Material cropType = cropBlock.getType();

        // Award AuraSkills XP
        auraSkillsHelper.addFarmingXp(player, BASE_HARVEST_XP);

        // Apply fortune/double drops
        int dropMultiplier = calculateDropMultiplier();

        // Break and collect
        ItemStack tool = new ItemStack(Material.IRON_HOE);
        cropBlock.breakNaturally(tool);

        // Add extra fortune drops directly to inventory
        if (dropMultiplier > 1) {
            Material product = cropProductMap.get(cropType);
            if (product != null) {
                player.getInventory().addItem(new ItemStack(product, dropMultiplier - 1));
            }
        }

        // Replant
        Material seedType = seedMap.get(cropType);
        if (seedType != null && hasItem(seedType)) {
            removeOneItem(seedType);
            cropBlock.setType(cropType);
            if (cropBlock.getBlockData() instanceof Ageable newAgeable) {
                newAgeable.setAge(0);
                cropBlock.setBlockData(newAgeable);
            }
        }

        // Send message
        player.sendMessage(Component.text("Here Crop! We got ")
                .color(NamedTextColor.GREEN)
                .append(Component.text(formatName(cropType.name())).color(NamedTextColor.YELLOW))
                .append(Component.text("!").color(NamedTextColor.GREEN)));
        return true;
    }

    private boolean tryPlant(Block ground, Block above) {
        boolean isSoulSand = ground.getType() == Material.SOUL_SAND;

        Material[][] seedPriority = {
                {Material.WHEAT_SEEDS, Material.WHEAT},
                {Material.CARROT, Material.CARROTS},
                {Material.POTATO, Material.POTATOES},
                {Material.BEETROOT_SEEDS, Material.BEETROOTS},
                {Material.NETHER_WART, Material.NETHER_WART}
        };

        for (Material[] pair : seedPriority) {
            Material seedType = pair[0];
            Material cropType = pair[1];

            if (cropType == Material.NETHER_WART && !isSoulSand) continue;
            if (cropType != Material.NETHER_WART && isSoulSand) continue;

            if (hasItem(seedType)) {
                removeOneItem(seedType);
                above.setType(cropType);
                if (above.getBlockData() instanceof Ageable ageable) {
                    ageable.setAge(0);
                    above.setBlockData(ageable);
                }
                return true;
            }
        }
        return false;
    }

    private boolean tryTill(Block ground) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!isHoe(hand.getType())) {
            return false;
        }

        if (hand.getItemMeta() instanceof Damageable damageable) {
            if (damageable.getDamage() + 1 >= hand.getType().getMaxDurability()) {
                return false;
            }
        }

        ground.setType(Material.FARMLAND);

        if (hand.getItemMeta() instanceof Damageable damageable) {
            damageable.setDamage(damageable.getDamage() + 1);
            hand.setItemMeta(damageable);
        }

        return true;
    }

    private boolean isFarmGround(Block block) {
        Material type = block.getType();
        return type == Material.FARMLAND || type == Material.SOUL_SAND || isTillable(block);
    }

    private boolean isTillable(Block block) {
        return switch (block.getType()) {
            case DIRT, GRASS_BLOCK, COARSE_DIRT, PODZOL, MYCELIUM, ROOTED_DIRT -> true;
            default -> false;
        };
    }

    private boolean isHoe(Material material) {
        return switch (material) {
            case WOODEN_HOE, STONE_HOE, IRON_HOE, GOLDEN_HOE, DIAMOND_HOE, NETHERITE_HOE -> true;
            default -> false;
        };
    }

    private boolean isOpenablePassage(Material material) {
        return Tag.DOORS.isTagged(material)
                || Tag.FENCE_GATES.isTagged(material)
                || Tag.TRAPDOORS.isTagged(material);
    }

    private String passageKey(int x, int y, int z) {
        return x + "," + y + "," + z;
    }

}
