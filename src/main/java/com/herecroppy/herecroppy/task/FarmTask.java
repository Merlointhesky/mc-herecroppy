package com.herecroppy.herecroppy.task;

import com.herecroppy.herecroppy.HereCroppyPlugin;
import com.herecroppy.herecroppy.auraskills.AuraSkillsHelper;
import com.herecroppy.herecroppy.config.CropConfigManager;
import com.herecroppy.herecroppy.map.ScanManager;
import com.herecroppy.herecroppy.map.ScanResult;
import com.herecroppy.herecroppy.path.PathGenerator;
import com.herecroppy.herecroppy.selection.SelectionManager;
import com.herecroppy.herecroppy.setup.SetupConfiguration;
import com.herecroppy.herecroppy.setup.SetupManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.Item;
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
    private final SetupManager setupManager;
    private final CropConfigManager cropConfigManager;
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
    private final Map<Material, Material> cropBlockToConfigKeyMap;
    private final Set<String> passagesOpenedByBot = new HashSet<>();
    private int bonemealCollectedThisLoop = 0;
    private final Map<String, Material> pumpkinMelonBlockMap = new HashMap<>();

    public FarmTask(HereCroppyPlugin plugin, Player player, List<Location> path,
                    AuraSkillsHelper auraSkillsHelper, ScanManager scanManager,
                    SelectionManager selectionManager, ScanResult scanResult) {
        this.plugin = plugin;
        this.player = player;
        this.path = path;
        this.auraSkillsHelper = auraSkillsHelper;
        this.scanManager = scanManager;
        this.selectionManager = selectionManager;
        this.setupManager = plugin.getSetupManager();
        this.cropConfigManager = plugin.getCropConfigManager();
        this.scanResult = scanResult;

        // Maps seed ITEM to crop BLOCK material
        this.seedMap = new HashMap<>();
        seedMap.put(Material.WHEAT_SEEDS, Material.WHEAT);
        seedMap.put(Material.CARROT, Material.CARROTS);
        seedMap.put(Material.POTATO, Material.POTATOES);
        seedMap.put(Material.BEETROOT_SEEDS, Material.BEETROOTS);
        seedMap.put(Material.NETHER_WART, Material.NETHER_WART);
        seedMap.put(Material.SUGAR_CANE, Material.SUGAR_CANE);
        seedMap.put(Material.PUMPKIN_SEEDS, Material.PUMPKIN_STEM);
        seedMap.put(Material.MELON_SEEDS, Material.MELON_STEM);
       
        // Maps crop BLOCK material to harvested PRODUCT item
        this.cropProductMap = new HashMap<>();
        cropProductMap.put(Material.WHEAT, Material.WHEAT);
        cropProductMap.put(Material.CARROTS, Material.CARROT);
        cropProductMap.put(Material.POTATOES, Material.POTATO);
        cropProductMap.put(Material.BEETROOTS, Material.BEETROOT);
        cropProductMap.put(Material.NETHER_WART, Material.NETHER_WART);
        cropProductMap.put(Material.SUGAR_CANE, Material.SUGAR_CANE);
        cropProductMap.put(Material.PUMPKIN_STEM, Material.PUMPKIN_SEEDS);
        cropProductMap.put(Material.MELON_STEM, Material.MELON_SEEDS);
        
        // Maps crop BLOCK material to config key (for CropConfigManager lookups)
        this.cropBlockToConfigKeyMap = new HashMap<>();
        cropBlockToConfigKeyMap.put(Material.WHEAT, Material.WHEAT);
        cropBlockToConfigKeyMap.put(Material.CARROTS, Material.CARROT);
        cropBlockToConfigKeyMap.put(Material.POTATOES, Material.POTATO);
        cropBlockToConfigKeyMap.put(Material.BEETROOTS, Material.BEETROOT);
        cropBlockToConfigKeyMap.put(Material.NETHER_WART, Material.NETHER_WART);
        cropBlockToConfigKeyMap.put(Material.SUGAR_CANE, Material.SUGAR_CANE);
        cropBlockToConfigKeyMap.put(Material.PUMPKIN_STEM, Material.PUMPKIN);
        cropBlockToConfigKeyMap.put(Material.MELON_STEM, Material.MELON);
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
            // Try to dump and collect before stopping
            if (attemptDumpAndCollect()) {
                // Successfully dumped/collected, continue farming
                return;
            }
            // If dump/collect failed or not configured, stop
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

        // Map pumpkin and melon blocks at each iteration
        mapPumpkinMelonBlocks();

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
            
            // Check for adjacent pumpkin/melon blocks and harvest them
            harvestAdjacentPumpkinMelon(target);
            
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
                // Reset bonemeal collection counter at loop start
                bonemealCollectedThisLoop = 0;
                // Collect bonemeal once per loop
                collectBonemeal();
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
            // Remap pumpkin and melon blocks after rescan
            mapPumpkinMelonBlocks();
        });
    }

    private void mapPumpkinMelonBlocks() {
        pumpkinMelonBlockMap.clear();
        if (scanResult == null || selectionManager == null || !selectionManager.hasCompleteSelection(player.getUniqueId())) {
            return;
        }

        Location pointA = selectionManager.getPointA(player.getUniqueId());
        Location pointB = selectionManager.getPointB(player.getUniqueId());
        int minX = Math.min(pointA.getBlockX(), pointB.getBlockX());
        int maxX = Math.max(pointA.getBlockX(), pointB.getBlockX());
        int minZ = Math.min(pointA.getBlockZ(), pointB.getBlockZ());
        int maxZ = Math.max(pointA.getBlockZ(), pointB.getBlockZ());

        World world = player.getWorld();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int groundY = scanResult.getGroundY(x, z);
                // Check for pumpkin and melon blocks at ground level and above
                for (int dy = 0; dy <= 2; dy++) {
                    Block block = world.getBlockAt(x, groundY + dy, z);
                    if (block.getType() == Material.PUMPKIN) {
                        pumpkinMelonBlockMap.put(x + "," + z, Material.PUMPKIN);
                    } else if (block.getType() == Material.MELON) {
                        pumpkinMelonBlockMap.put(x + "," + z, Material.MELON);
                    }
                }
            }
        }
    }

    private void harvestAdjacentPumpkinMelon(Location loc) {
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        int groundY = scanResult != null ? scanResult.getGroundY(x, z) : loc.getBlockY();
        World world = player.getWorld();

        // Check all 4 adjacent horizontal directions for pumpkin/melon blocks
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] dir : directions) {
            int adjX = x + dir[0];
            int adjZ = z + dir[1];
            String key = adjX + "," + adjZ;

            if (pumpkinMelonBlockMap.containsKey(key)) {
                Material blockType = pumpkinMelonBlockMap.get(key);
                // Find the actual block (could be at different Y levels)
                for (int dy = 0; dy <= 2; dy++) {
                    Block block = world.getBlockAt(adjX, groundY + dy, adjZ);
                    if (block.getType() == blockType) {
                        // Harvest the pumpkin/melon block
                        if (cropConfigManager.isCollectingEnabled(player.getUniqueId(), blockType)) {
                            auraSkillsHelper.addFarmingXp(player, BASE_HARVEST_XP);
                            int dropMultiplier = calculateDropMultiplier();
                            
                            // Collect as whole block instead of breaking (which would split melons into slices)
                            block.setType(Material.AIR);
                            ItemStack fruit = new ItemStack(blockType, 1);
                            depositCropItem(fruit);

                            // Add extra fortune drops
                            if (dropMultiplier > 1) {
                                ItemStack extraDrops = new ItemStack(blockType, dropMultiplier - 1);
                                depositCropItem(extraDrops);
                            }

                            player.sendMessage(Component.text("Here Crop! We got ")
                                    .color(NamedTextColor.GREEN)
                                    .append(Component.text(formatName(blockType.name())).color(NamedTextColor.YELLOW))
                                    .append(Component.text("!").color(NamedTextColor.GREEN)));
                        }
                        pumpkinMelonBlockMap.remove(key);
                        break;
                    }
                }
            }
        }
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

        boolean justSeeded = false;

        // STEP A: PREPARE SOIL - Till if needed
        if (isTillable(ground) && above.getType().isAir()) {
            if (tryTill(ground)) {
                return true;
            }
        }

        // STEP B: SEED IF NEEDED - Plant first available seed
        if ((ground.getType() == Material.FARMLAND || ground.getType() == Material.SOUL_SAND)
                && above.getType().isAir()) {
            if (tryPlant(ground, above)) {
                justSeeded = true;
                // Continue to check for bonemeal on newly planted crop
            }
        }

        // STEP C: BONEMEAL - Apply to unripe crops (existing or just planted)
        Block cropBlock = findCropBlock(loc);
        if (cropBlock != null && isCrop(cropBlock)) {
            Material cropConfigKey = getCropConfigKey(cropBlock.getType());
            
            // Handle sugar cane specially (not Ageable)
            if (cropBlock.getType() == Material.SUGAR_CANE) {
                // Sugar cane doesn't need bonemeal, skip to harvest
                return processHarvestAndReseed(cropBlock, justSeeded);
            }

            // For Ageable crops
            if (cropBlock.getBlockData() instanceof Ageable ageable) {
                // Apply bonemeal if crop is not fully grown
                if (ageable.getAge() < ageable.getMaximumAge()) {
                    if (cropConfigManager.isBonemealEnabled(player.getUniqueId(), cropConfigKey) && hasItem(Material.BONE_MEAL)) {
                        removeOneItem(Material.BONE_MEAL);
                        cropBlock.applyBoneMeal(org.bukkit.block.BlockFace.UP);
                        // Re-check after bonemeal
                        if (cropBlock.getBlockData() instanceof Ageable after) {
                            ageable = after;
                        }
                    }
                }

                // STEP D: HARVEST + RESEED - If crop is now fully grown
                if (ageable.getAge() == ageable.getMaximumAge()) {
                    return processHarvestAndReseed(cropBlock, justSeeded);
                }
            }
        }

        return justSeeded;
    }

    /**
     * Process harvesting and reseeding of a crop.
     * @param cropBlock The crop block to harvest
     * @param justSeeded Whether we just planted this crop (affects bonemeal usage)
     * @return true if harvest occurred
     */
    private boolean processHarvestAndReseed(Block cropBlock, boolean justSeeded) {
        Material cropBlockType = cropBlock.getType();
        Material cropConfigKey = getCropConfigKey(cropBlockType);

        // Check if collecting is enabled for this crop
        if (!cropConfigManager.isCollectingEnabled(player.getUniqueId(), cropConfigKey)) {
            return false;
        }

        // Special handling for sugar cane - break at height 2, leave 1 block to regrow
        if (cropBlockType == Material.SUGAR_CANE) {
            return harvestSugarCane(cropBlock);
        }

        // Award AuraSkills XP
        auraSkillsHelper.addFarmingXp(player, BASE_HARVEST_XP);

        // Apply fortune/double drops
        int dropMultiplier = calculateDropMultiplier();

        // Break and collect drops immediately
        ItemStack tool = new ItemStack(Material.IRON_HOE);
        cropBlock.breakNaturally(tool);
        collectDroppedItems(cropBlock.getLocation());

        // Add extra fortune drops directly to inventory or dump box
        if (dropMultiplier > 1) {
            Material product = cropProductMap.get(cropBlockType);
            if (product != null) {
                ItemStack extraDrops = new ItemStack(product, dropMultiplier - 1);
                depositCropItem(extraDrops);
            }
        }

        // Replant if seeding is enabled and we have seeds
        if (cropConfigManager.isSeedingEnabled(player.getUniqueId(), cropConfigKey)) {
            Material seedItem = getSeedItemForCrop(cropBlockType);
            if (seedItem != null && hasItem(seedItem)) {
                removeOneItem(seedItem);
                cropBlock.setType(cropBlockType);
                if (cropBlock.getBlockData() instanceof Ageable newAgeable) {
                    newAgeable.setAge(0);
                    cropBlock.setBlockData(newAgeable);
                }
                
                // Apply bonemeal once to newly planted crop if enabled
                if (cropConfigManager.isBonemealEnabled(player.getUniqueId(), cropConfigKey) && hasItem(Material.BONE_MEAL)) {
                    removeOneItem(Material.BONE_MEAL);
                    cropBlock.applyBoneMeal(org.bukkit.block.BlockFace.UP);
                }
            }
        }

        // Send message
        player.sendMessage(Component.text("Here Crop! We got ")
                .color(NamedTextColor.GREEN)
                .append(Component.text(formatName(cropBlockType.name())).color(NamedTextColor.YELLOW))
                .append(Component.text("!").color(NamedTextColor.GREEN)));
        return true;
    }

    /**
     * Get the seed item for a given crop block type.
     */
    private Material getSeedItemForCrop(Material cropBlockType) {
        return switch (cropBlockType) {
            case WHEAT -> Material.WHEAT_SEEDS;
            case CARROTS -> Material.CARROT;
            case POTATOES -> Material.POTATO;
            case BEETROOTS -> Material.BEETROOT_SEEDS;
            case NETHER_WART -> Material.NETHER_WART;
            case SUGAR_CANE -> Material.SUGAR_CANE;
            case PUMPKIN_STEM -> Material.PUMPKIN_SEEDS;
            case MELON_STEM -> Material.MELON_SEEDS;
            default -> null;
        };
    }

    private Block findCropBlock(Location loc) {
        Block blockAtLoc = loc.getBlock();
        if (isCrop(blockAtLoc)) return blockAtLoc;

        Block blockAbove = loc.clone().add(0, 1, 0).getBlock();
        if (isCrop(blockAbove)) return blockAbove;

        return null;
    }

    private boolean isCrop(Block block) {
        Material type = block.getType();
        // Check if it's an Ageable crop (wheat, carrots, potatoes, beetroots, nether wart, pumpkin stem, melon stem)
        if (block.getBlockData() instanceof Ageable) {
            return true;
        }
        // Sugar cane is not Ageable but is a crop
        return type == Material.SUGAR_CANE;
    }
    
    /**
     * Maps a crop block material to its config key for CropConfigManager lookups.
     * For example, CARROTS block maps to CARROT config key.
     */
    private Material getCropConfigKey(Material cropBlockType) {
        return cropBlockToConfigKeyMap.getOrDefault(cropBlockType, cropBlockType);
    }

    private boolean isAdjacentToWater(Block block) {
        // Check all 4 horizontal directions for water
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();
        World world = block.getWorld();

        Material[] directions = {
                world.getBlockAt(x + 1, y, z).getType(),
                world.getBlockAt(x - 1, y, z).getType(),
                world.getBlockAt(x, y, z + 1).getType(),
                world.getBlockAt(x, y, z - 1).getType()
        };

        for (Material mat : directions) {
            if (mat == Material.WATER) {
                return true;
            }
        }
        return false;
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

    private boolean harvestSugarCane(Block cropBlock) {
        // Award AuraSkills XP
        auraSkillsHelper.addFarmingXp(player, BASE_HARVEST_XP);

        // Apply fortune/double drops
        int dropMultiplier = calculateDropMultiplier();

        // Find the bottom of the sugar cane stack
        Block current = cropBlock;
        while (current.getY() > 0) {
            Block below = current.getWorld().getBlockAt(current.getX(), current.getY() - 1, current.getZ());
            if (below.getType() == Material.SUGAR_CANE) {
                current = below;
            } else {
                break;
            }
        }

        // Now current is at the bottom. We want to break from height 2 above ground up to the top
        // Leave 1 block at the bottom to regrow
        Block toBreak = current.getWorld().getBlockAt(current.getX(), current.getY() + 1, current.getZ());
        
        while (toBreak.getType() == Material.SUGAR_CANE) {
            ItemStack tool = new ItemStack(Material.IRON_HOE);
            toBreak.breakNaturally(tool);
            collectDroppedItems(toBreak.getLocation());
            
            // Move up to next block
            toBreak = toBreak.getWorld().getBlockAt(toBreak.getX(), toBreak.getY() + 1, toBreak.getZ());
        }

        // Add extra fortune drops
        if (dropMultiplier > 1) {
            ItemStack extraDrops = new ItemStack(Material.SUGAR_CANE, dropMultiplier - 1);
            depositCropItem(extraDrops);
        }

        // Send message
        player.sendMessage(Component.text("Here Crop! We got ")
                .color(NamedTextColor.GREEN)
                .append(Component.text("Sugar Cane").color(NamedTextColor.YELLOW))
                .append(Component.text("!").color(NamedTextColor.GREEN)));
        return true;
    }

    private boolean tryPlant(Block ground, Block above) {
        boolean isSoulSand = ground.getType() == Material.SOUL_SAND;
        boolean isFarmland = ground.getType() == Material.FARMLAND;

        // Array of: {seedItem, configKey, cropBlockMaterial}
        Object[][] seedPriority = {
                {Material.WHEAT_SEEDS, Material.WHEAT, Material.WHEAT},
                {Material.CARROT, Material.CARROT, Material.CARROTS},
                {Material.POTATO, Material.POTATO, Material.POTATOES},
                {Material.BEETROOT_SEEDS, Material.BEETROOT, Material.BEETROOTS},
                {Material.NETHER_WART, Material.NETHER_WART, Material.NETHER_WART},
                {Material.PUMPKIN_SEEDS, Material.PUMPKIN, Material.PUMPKIN_STEM},
                {Material.MELON_SEEDS, Material.MELON, Material.MELON_STEM},
                {Material.SUGAR_CANE, Material.SUGAR_CANE, Material.SUGAR_CANE}
        };

        for (Object[] entry : seedPriority) {
            Material seedType = (Material) entry[0];
            Material configKey = (Material) entry[1];
            Material cropBlockType = (Material) entry[2];

            // Check if seeding is enabled for this crop
            if (!cropConfigManager.isSeedingEnabled(player.getUniqueId(), configKey)) {
                continue;
            }

            // Nether Wart only on Soul Sand
            if (configKey == Material.NETHER_WART && !isSoulSand) continue;
            if (configKey == Material.NETHER_WART && isSoulSand) {
                if (hasItem(seedType)) {
                    removeOneItem(seedType);
                    above.setType(cropBlockType);
                    if (above.getBlockData() instanceof Ageable ageable) {
                        ageable.setAge(0);
                        above.setBlockData(ageable);
                    }
                    return true;
                }
                continue;
            }

            // Sugar Cane only on dirt/grass adjacent to water
            if (configKey == Material.SUGAR_CANE) {
                if (isSoulSand) continue; // Skip if on soul sand
                if (!isFarmland && !isTillable(ground)) continue; // Must be on dirt-like or farmland
                if (!isAdjacentToWater(ground)) continue; // Must be adjacent to water
                if (hasItem(seedType)) {
                    removeOneItem(seedType);
                    above.setType(cropBlockType);
                    return true;
                }
                continue;
            }

            // Pumpkin and Melon stems only on farmland
            if ((configKey == Material.PUMPKIN || configKey == Material.MELON) && !isFarmland) continue;
         
            // Regular crops (wheat, carrots, potatoes, beetroots) only on farmland
            if ((configKey == Material.WHEAT || configKey == Material.CARROT ||
                 configKey == Material.POTATO || configKey == Material.BEETROOT) && !isFarmland) continue;

            if (hasItem(seedType)) {
                removeOneItem(seedType);
                above.setType(cropBlockType);
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

    private void depositCropItem(ItemStack item) {
        // Add to inventory only. Cleanup/dumping happens when inventory is full.
        player.getInventory().addItem(item);
    }

    private void collectBonemeal() {
        SetupConfiguration setup = setupManager.getSetupConfig(player.getUniqueId());
        if (setup == null || setup.getBonemealCollectionBox() == null) {
            return;
        }

        Location bonemealBox = setup.getBonemealCollectionBox();
        Block box = bonemealBox.getBlock();
        if (!(box.getState() instanceof org.bukkit.block.Container container)) {
            return;
        }

        int targetAmount = setup.getBonemealPerLoop();
        int remaining = targetAmount - bonemealCollectedThisLoop;
        
        // Only collect if we haven't reached the per-loop limit
        if (remaining <= 0) {
            return;
        }

        int collected = 0;

        // Collect bonemeal from the box up to the remaining amount
        for (ItemStack item : container.getInventory().getContents()) {
            if (item != null && item.getType() == Material.BONE_MEAL && collected < remaining) {
                int toTake = Math.min(item.getAmount(), remaining - collected);
                item.setAmount(item.getAmount() - toTake);
                collected += toTake;
                if (collected >= remaining) break;
            }
        }

        // Add collected bonemeal to player inventory
        if (collected > 0) {
            player.getInventory().addItem(new ItemStack(Material.BONE_MEAL, collected));
            bonemealCollectedThisLoop += collected;
        }
    }

    private void applyBonemealToCrop(Block cropBlock) {
        Material cropType = cropBlock.getType();

        // Check if bonemeal is enabled for this crop
        if (!cropConfigManager.isBonemealEnabled(player.getUniqueId(), cropType)) {
            return;
        }

        // Check if we have bonemeal
        if (!hasItem(Material.BONE_MEAL)) {
            return;
        }

        // Apply bonemeal
        removeOneItem(Material.BONE_MEAL);
        cropBlock.applyBoneMeal(org.bukkit.block.BlockFace.UP);
    }

    private boolean attemptDumpAndCollect() {
        SetupConfiguration setup = setupManager.getSetupConfig(player.getUniqueId());
        if (setup == null || !setup.isComplete()) {
            return false;
        }

        // Dump unwanted crops
        dumpUnwantedCrops(setup);

        // Dump keep crops
        dumpKeepCrops(setup);

        // Collect bonemeal
        collectBonemeal();

        // Check if inventory is still full
        if (isInventoryFull()) {
            return false;
        }

        player.sendMessage(Component.text("Dumped crops and collected bonemeal. Resuming farming...")
                .color(NamedTextColor.GREEN));
        return true;
    }

    private void dumpUnwantedCrops(SetupConfiguration setup) {
        Location dumpUnwantedBox = setup.getDumpUnwantedBox();
        if (dumpUnwantedBox == null) {
            return;
        }

        Block box = dumpUnwantedBox.getBlock();
        if (!(box.getState() instanceof org.bukkit.block.Container container)) {
            return;
        }

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getAmount() == 0) continue;

            Material type = item.getType();
            Material configKey = getCropConfigKeyFromItem(type);
            if (configKey == null) continue;

            boolean shouldDump = false;
            boolean isSeed = isSeedItem(type);

            if (isSeed) {
                if (cropConfigManager.isSeedDumpEnabled(player.getUniqueId(), configKey)) {
                    shouldDump = true;
                }
            } else {
                if (cropConfigManager.isJunkEnabled(player.getUniqueId(), configKey)) {
                    shouldDump = true;
                }
            }

            if (shouldDump) {
                ItemStack toMove = item.clone();
                Map<Integer, ItemStack> remaining = container.getInventory().addItem(toMove);
                if (remaining.isEmpty()) {
                    item.setAmount(0);
                } else {
                    item.setAmount(remaining.get(0).getAmount());
                }
            }
        }
    }

    private void dumpKeepCrops(SetupConfiguration setup) {
        Location dumpKeepBox = setup.getDumpKeepBox();
        if (dumpKeepBox == null) {
            return;
        }

        Block box = dumpKeepBox.getBlock();
        if (!(box.getState() instanceof org.bukkit.block.Container container)) {
            return;
        }

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getAmount() == 0) continue;

            Material type = item.getType();
            Material configKey = getCropConfigKeyFromItem(type);
            if (configKey == null) continue;

            boolean isSeed = isSeedItem(type);
            // Only products (non-seeds) go to the keep box if they are not marked as junk
            if (!isSeed) {
                if (!cropConfigManager.isJunkEnabled(player.getUniqueId(), configKey)) {
                    ItemStack toMove = item.clone();
                    Map<Integer, ItemStack> remaining = container.getInventory().addItem(toMove);
                    if (remaining.isEmpty()) {
                        item.setAmount(0);
                    } else {
                        item.setAmount(remaining.get(0).getAmount());
                    }
                }
            }
        }
    }

    private Material getCropConfigKeyFromItem(Material type) {
        return switch (type) {
            case WHEAT, WHEAT_SEEDS -> Material.WHEAT;
            case CARROT -> Material.CARROT;
            case POTATO -> Material.POTATO;
            case BEETROOT, BEETROOT_SEEDS -> Material.BEETROOT;
            case NETHER_WART -> Material.NETHER_WART;
            case SUGAR_CANE -> Material.SUGAR_CANE;
            case PUMPKIN, PUMPKIN_SEEDS -> Material.PUMPKIN;
            case MELON, MELON_SEEDS -> Material.MELON;
            default -> null;
        };
    }

    private boolean isSeedItem(Material type) {
        return type == Material.WHEAT_SEEDS || type == Material.BEETROOT_SEEDS ||
               type == Material.PUMPKIN_SEEDS || type == Material.MELON_SEEDS;
    }

    /**
     * Collects all dropped items within a 2-block radius of the given location.
     * This ensures items dropped from broken crops don't get left behind.
     */
    private void collectDroppedItems(Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }

        // Get all entities within a 2-block radius
        location.getWorld().getNearbyEntities(location, 2.0, 2.0, 2.0).forEach(entity -> {
            if (entity instanceof Item itemEntity) {
                ItemStack itemStack = itemEntity.getItemStack();
                if (itemStack != null && itemStack.getAmount() > 0) {
                    // Deposit the item to inventory or dump boxes
                    depositCropItem(itemStack.clone());
                    // Remove the item entity from the world
                    itemEntity.remove();
                }
            }
        });
    }

}
