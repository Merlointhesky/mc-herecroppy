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
    private static final Map<Material, Double> CROP_XP_MAP = new HashMap<>();
    static {
        CROP_XP_MAP.put(Material.WHEAT, 10.0);
        CROP_XP_MAP.put(Material.CARROTS, 12.0);
        CROP_XP_MAP.put(Material.POTATOES, 12.0);
        CROP_XP_MAP.put(Material.BEETROOTS, 15.0);
        CROP_XP_MAP.put(Material.NETHER_WART, 20.0);
        CROP_XP_MAP.put(Material.SUGAR_CANE, 8.0);
        CROP_XP_MAP.put(Material.PUMPKIN, 15.0);
        CROP_XP_MAP.put(Material.MELON, 15.0);
    }
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
    
    // Activity Tracking
    private final Map<Material, Integer> harvestedCrops = new HashMap<>();
    private final Map<Material, Integer> harvestedSeeds = new HashMap<>();
    private int inventoryEmptyCount = 0;
    private int bonemealUsedCount = 0;

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
            } else {
                // If dump/collect failed or not configured, stop
                sendActivitySummary();
                cancel();
                FarmTaskManager manager = plugin.getFarmTaskManager();
                manager.recordInventoryFullStop(player, currentIndex);
                player.sendMessage(Component.text("Inventory full! Auto-farm paused. Use /herecroppy restart to resume.")
                        .color(NamedTextColor.RED));
                return;
            }
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
            sendActivitySummary();
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
                sendActivitySummary();
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
                            auraSkillsHelper.addFarmingXp(player, CROP_XP_MAP.getOrDefault(blockType, BASE_HARVEST_XP));
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

        boolean harvested = false;

        // 1. TILL if needed
        if (isTillable(ground) && above.getType().isAir()) {
            if (tryTill(ground)) {
                // Tilled, now we can try to seed in the next step
            }
        }

        // 2. SEED if needed
        if ((ground.getType() == Material.FARMLAND || ground.getType() == Material.SOUL_SAND)
                && above.getType().isAir()) {
            tryPlant(ground, above);
        }

        // 3. BONEMEAL & 4. COLLECT
        Block cropBlock = findCropBlock(loc);
        if (cropBlock != null && isCrop(cropBlock)) {
            Material cropConfigKey = getCropConfigKey(cropBlock.getType());
            
            // Handle sugar cane specially
            if (cropBlock.getType() == Material.SUGAR_CANE) {
                harvested = harvestSugarCane(cropBlock);
            } else {
                // Apply bonemeal if needed
                attemptBonemeal(cropBlock);
                
                // If crop is now fully grown, harvest it (unless it's a pumpkin/melon stem)
                if (cropBlock.getBlockData() instanceof Ageable ageable && ageable.getAge() == ageable.getMaximumAge()) {
                    if (cropBlock.getType() != Material.PUMPKIN_STEM && cropBlock.getType() != Material.MELON_STEM) {
                        harvested = processHarvestAndReseed(cropBlock);
                    }
                }
            }
        }

        // 5. REPEAT SEED step (if we just harvested)
        if (harvested) {
            if ((ground.getType() == Material.FARMLAND || ground.getType() == Material.SOUL_SAND)
                    && above.getType().isAir()) {
                if (tryPlant(ground, above)) {
                    // Try to bonemeal the newly planted seed
                    attemptBonemeal(above);
                }
            }
        }

        // 6. Check inventory status and dump if full
        if (isInventoryFull()) {
            attemptDumpAndCollect();
        }

        // 7. Check bonemeal status and collect if empty
        if (!hasItem(Material.BONE_MEAL)) {
            collectBonemeal();
        }

        return harvested;
    }

    private void attemptBonemeal(Block cropBlock) {
        if (!(cropBlock.getBlockData() instanceof Ageable ageable)) {
            return;
        }

        if (ageable.getAge() >= ageable.getMaximumAge()) {
            return;
        }

        Material cropConfigKey = getCropConfigKey(cropBlock.getType());
        if (!cropConfigManager.isBonemealEnabled(player.getUniqueId(), cropConfigKey)) {
            return;
        }

        if (!hasItem(Material.BONE_MEAL)) {
            collectBonemeal();
        }

        if (hasItem(Material.BONE_MEAL)) {
            if (cropBlock.applyBoneMeal(org.bukkit.block.BlockFace.UP)) {
                removeOneItem(Material.BONE_MEAL);
                bonemealUsedCount++;
                player.sendActionBar(Component.text("Used bonemeal on " + formatName(cropConfigKey.name()))
                        .color(NamedTextColor.YELLOW));
            }
        }
    }

    /**
     * Process harvesting and reseeding of a crop.
     * @param cropBlock The crop block to harvest
     * @return true if harvest occurred
     */
    private boolean processHarvestAndReseed(Block cropBlock) {
        Material cropBlockType = cropBlock.getType();
        Material cropConfigKey = getCropConfigKey(cropBlockType);

        // Check if collecting is enabled for this crop
        if (!cropConfigManager.isCollectingEnabled(player.getUniqueId(), cropConfigKey)) {
            return false;
        }

        // Award AuraSkills XP
        auraSkillsHelper.addFarmingXp(player, CROP_XP_MAP.getOrDefault(cropBlockType, BASE_HARVEST_XP));

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
        return player.getInventory().contains(material);
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
        auraSkillsHelper.addFarmingXp(player, CROP_XP_MAP.getOrDefault(Material.SUGAR_CANE, BASE_HARVEST_XP));

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
        if (item == null || item.getAmount() == 0) return;
        
        Material type = item.getType();
        if (isSeedItem(type)) {
            harvestedSeeds.put(type, harvestedSeeds.getOrDefault(type, 0) + item.getAmount());
        } else {
            harvestedCrops.put(type, harvestedCrops.getOrDefault(type, 0) + item.getAmount());
        }
        
        // Try to add to inventory
        Map<Integer, ItemStack> remaining = player.getInventory().addItem(item);
        
        // If some items couldn't be added, try to dump and then add again
        if (!remaining.isEmpty()) {
            if (attemptDumpAndCollect()) {
                // Try adding the remainder again
                for (ItemStack rem : remaining.values()) {
                    Map<Integer, ItemStack> stillRemaining = player.getInventory().addItem(rem);
                    // Still can't add, drop them
                    for (ItemStack dropped : stillRemaining.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), dropped);
                    }
                }
            } else {
                // No dump setup or dump box full, drop them
                for (ItemStack rem : remaining.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), rem);
                }
            }
        }
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

    private boolean attemptDumpAndCollect() {
        SetupConfiguration setup = setupManager.getSetupConfig(player.getUniqueId());
        if (setup == null || !setup.isComplete()) {
            return false;
        }

        // Dump unwanted crops
        dumpUnwantedCrops(setup);

        // Dump keep crops
        dumpKeepCrops(setup);

        inventoryEmptyCount++;

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
            
            // Hardcoded: Poisonous Potatoes always go to keep box
            if (type == Material.POISONOUS_POTATO) {
                ItemStack toMove = item.clone();
                Map<Integer, ItemStack> remaining = container.getInventory().addItem(toMove);
                if (remaining.isEmpty()) {
                    item.setAmount(0);
                } else {
                    item.setAmount(remaining.get(0).getAmount());
                }
                continue;
            }

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
            case POTATO, POISONOUS_POTATO -> Material.POTATO;
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

    public void sendActivitySummary() {
        if (harvestedCrops.isEmpty() && harvestedSeeds.isEmpty() && inventoryEmptyCount == 0 && bonemealUsedCount == 0) {
            return;
        }

        player.sendMessage(Component.text("-----------------------------------").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("#HERECROP ACTIVITY LIST:").color(NamedTextColor.GOLD));

        // Group crops and seeds
        Set<Material> allCrops = new HashSet<>(harvestedCrops.keySet());
        // Also include crops that only had seeds collected (though unlikely)
        for (Material seed : harvestedSeeds.keySet()) {
            Material crop = getCropConfigKeyFromItem(seed);
            if (crop != null) allCrops.add(crop);
        }

        for (Material crop : allCrops) {
            int cropCount = harvestedCrops.getOrDefault(crop, 0);
            
            // Find corresponding seed count
            int seedCount = 0;
            Material seedMaterial = getSeedItemForCrop(seedMap.containsValue(crop) ? 
                seedMap.entrySet().stream().filter(e -> e.getValue() == crop).findFirst().get().getValue() : crop);
            // This is getting complex, let's simplify seed lookup
            
            seedCount = getSeedCountForCrop(crop);

            StringBuilder msg = new StringBuilder("# ");
            msg.append(formatName(crop.name())).append(": ");
            
            if (seedCount > 0 && cropCount > 0) {
                msg.append(seedCount).append(" seed and ").append(cropCount).append(" ").append(formatName(crop.name())).append(" collected!");
            } else if (cropCount > 0) {
                msg.append(cropCount).append(" ").append(formatName(crop.name())).append(" collected!");
            } else if (seedCount > 0) {
                msg.append(seedCount).append(" seed collected!");
            }

            player.sendMessage(Component.text(msg.toString()).color(NamedTextColor.GREEN));
        }

        if (inventoryEmptyCount > 0) {
            player.sendMessage(Component.text("# Emptied inventory " + inventoryEmptyCount + " times").color(NamedTextColor.YELLOW));
        }
        if (bonemealUsedCount > 0) {
            player.sendMessage(Component.text("# Used " + bonemealUsedCount + " bonemeal").color(NamedTextColor.WHITE));
        }
        player.sendMessage(Component.text("-----------------------------------").color(NamedTextColor.GRAY));
        
        // Reset counters after sending summary? 
        // User didn't specify, but usually you'd want a fresh summary next time.
        // If it's sent per loop, we should reset. If sent at the end, it doesn't matter much as task is cancelled.
        // I'll reset them.
        harvestedCrops.clear();
        harvestedSeeds.clear();
        inventoryEmptyCount = 0;
        bonemealUsedCount = 0;
    }

    private int getSeedCountForCrop(Material crop) {
        return switch (crop) {
            case WHEAT -> harvestedSeeds.getOrDefault(Material.WHEAT_SEEDS, 0);
            case BEETROOT, BEETROOTS -> harvestedSeeds.getOrDefault(Material.BEETROOT_SEEDS, 0);
            case PUMPKIN, PUMPKIN_STEM -> harvestedSeeds.getOrDefault(Material.PUMPKIN_SEEDS, 0);
            case MELON, MELON_STEM -> harvestedSeeds.getOrDefault(Material.MELON_SEEDS, 0);
            default -> 0;
        };
    }

}
