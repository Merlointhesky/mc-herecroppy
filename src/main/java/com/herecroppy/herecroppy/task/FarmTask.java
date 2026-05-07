package com.herecroppy.herecroppy.task;

import com.herecroppy.herecroppy.HereCroppyPlugin;
import com.herecroppy.herecroppy.auraskills.AuraSkillsHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class FarmTask extends BukkitRunnable {

    private static final double SPEED = 0.25;
    private static final double SNAP_DISTANCE = 0.3;
    private static final double BASE_HARVEST_XP = 10.0;
    private static final int HARVEST_PAUSE_TICKS = 5;

    private final HereCroppyPlugin plugin;
    private final Player player;
    private final List<Location> path;
    private final AuraSkillsHelper auraSkillsHelper;
    private int currentIndex = 0;
    private int harvestPause = 0;
    private final Random random = new Random();
    private final Map<Material, Material> seedMap;
    private final Map<Material, Material> cropProductMap;

    public FarmTask(HereCroppyPlugin plugin, Player player, List<Location> path, AuraSkillsHelper auraSkillsHelper) {
        this.plugin = plugin;
        this.player = player;
        this.path = path;
        this.auraSkillsHelper = auraSkillsHelper;

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

    @Override
    public void run() {
        if (!player.isOnline()) {
            cancel();
            return;
        }

        if (isInventoryFull()) {
            cancel();
            player.sendMessage(Component.text("Inventory full! Auto-farm paused.")
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

        if (horizontalDist < SNAP_DISTANCE) {
            // Arrived at target - process crop (keep current Y to avoid going underground)
            Location snap = target.clone();
            snap.setY(current.getY());
            snap.setPitch(current.getPitch());
            snap.setYaw(current.getYaw());
            player.teleport(snap);
            boolean harvested = processCurrentBlock(target);
            if (harvested) {
                harvestPause = HARVEST_PAUSE_TICKS;
            }
            currentIndex++;
            if (currentIndex >= path.size()) {
                currentIndex = 0;
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

    private boolean processCurrentBlock(Location loc) {
        Block cropBlock = findCropBlock(loc);
        if (cropBlock == null) {
            return false;
        }

        if (cropBlock.getBlockData() instanceof Ageable ageable) {
            if (ageable.getAge() == ageable.getMaximumAge()) {
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
        }
        return false;
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

}
