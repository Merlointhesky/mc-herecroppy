package com.herecroppy.herecroppy.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CropConfigUI {
    private final CropConfigManager configManager;
    private static final String CATEGORY_MENU_TITLE = "HereCroppy - Crop Categories";
    private static final String FARMLAND_MENU_TITLE = "HereCroppy - Farmland Crops";
    private static final String SPECIAL_MENU_TITLE = "HereCroppy - Special Crops";
    private static final String PRIORITY_MENU_TITLE = "HereCroppy - Seeding Priority";
    private static final String SETTINGS_MENU_TITLE = "HereCroppy - Crop Settings";

    public CropConfigUI(CropConfigManager configManager) {
        this.configManager = configManager;
    }

    public void openCategoryMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, Component.text(CATEGORY_MENU_TITLE)
                .color(NamedTextColor.GOLD));

        // Farmland Crops
        ItemStack farmlandItem = createCategoryItem(Material.WHEAT, "Farmland Crops",
                "Wheat, Carrots, Potatoes, Beetroots");
        inventory.setItem(11, farmlandItem);

        // Seeding Priority
        ItemStack priorityItem = createCategoryItem(Material.WHEAT_SEEDS, "Seeding Priority",
                "Change the order of seeds used for replanting");
        inventory.setItem(13, priorityItem);

        // Special Crops
        ItemStack specialItem = createCategoryItem(Material.NETHER_WART, "Special Crops",
                "Nether Wart, Sugar Cane, Pumpkins, Watermelons");
        inventory.setItem(15, specialItem);

        player.openInventory(inventory);
    }

    public void openPriorityMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, Component.text(PRIORITY_MENU_TITLE)
                .color(NamedTextColor.GOLD));

        PlayerCropConfig config = configManager.getPlayerConfig(player.getUniqueId());
        List<Material> priority = config.getSeedingPriority();

        for (int i = 0; i < priority.size(); i++) {
            Material material = priority.get(i);
            ItemStack item = new ItemStack(getSeedMaterial(material));
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text((i + 1) + ". " + getCropDisplayName(material))
                        .color(NamedTextColor.YELLOW)
                        .decorate(TextDecoration.BOLD));
                
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("Left-click to move UP").color(NamedTextColor.GRAY));
                lore.add(Component.text("Right-click to move DOWN").color(NamedTextColor.GRAY));
                
                meta.lore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(10 + i, item);
        }

        // Back button
        ItemStack backItem = createNavigationItem(Material.ARROW, "Back to Categories");
        inventory.setItem(26, backItem);

        player.openInventory(inventory);
    }

    private Material getSeedMaterial(Material crop) {
        return switch (crop) {
            case WHEAT -> Material.WHEAT_SEEDS;
            case BEETROOT -> Material.BEETROOT_SEEDS;
            case PUMPKIN -> Material.PUMPKIN_SEEDS;
            case MELON -> Material.MELON_SEEDS;
            default -> crop;
        };
    }

    public void openFarmlandCropsMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, Component.text(FARMLAND_MENU_TITLE)
                .color(NamedTextColor.GOLD));

        int slot = 10;
        addCropItem(inventory, slot++, Material.WHEAT, player.getUniqueId());
        addCropItem(inventory, slot++, Material.CARROT, player.getUniqueId());
        addCropItem(inventory, slot++, Material.POTATO, player.getUniqueId());
        addCropItem(inventory, slot++, Material.BEETROOT, player.getUniqueId());

        // Back button
        ItemStack backItem = createNavigationItem(Material.ARROW, "Back to Categories");
        inventory.setItem(26, backItem);

        player.openInventory(inventory);
    }

    public void openSpecialCropsMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, Component.text(SPECIAL_MENU_TITLE)
                .color(NamedTextColor.GOLD));

        int slot = 10;
        addCropItem(inventory, slot++, Material.NETHER_WART, player.getUniqueId());
        addCropItem(inventory, slot++, Material.SUGAR_CANE, player.getUniqueId());
        addCropItem(inventory, slot++, Material.PUMPKIN, player.getUniqueId());
        addCropItem(inventory, slot++, Material.MELON, player.getUniqueId());

        // Back button
        ItemStack backItem = createNavigationItem(Material.ARROW, "Back to Categories");
        inventory.setItem(26, backItem);

        player.openInventory(inventory);
    }

    public void openCropSettingsMenu(Player player, Material cropType) {
        Inventory inventory = Bukkit.createInventory(null, 27, Component.text(SETTINGS_MENU_TITLE)
                .color(NamedTextColor.GOLD));

        CropSettings settings = configManager.getCropSettings(player.getUniqueId(), cropType);

        // Title
        ItemStack titleItem = createTitleItem(cropType);
        inventory.setItem(4, titleItem);

        // Seeding toggle
        ItemStack seedingItem = createToggleItem(Material.WHEAT_SEEDS, "Seeding",
                settings.isSeedingEnabled(), "Enable/Disable seeding for this crop");
        inventory.setItem(10, seedingItem);

        // Collecting toggle
        ItemStack collectingItem = createToggleItem(Material.HOPPER, "Collecting",
                settings.isCollectingEnabled(), "Enable/Disable collecting for this crop");
        inventory.setItem(12, collectingItem);

        // Bonemeal toggle
        ItemStack bonemealItem = createToggleItem(Material.BONE_MEAL, "Bonemeal",
                settings.isBonemealEnabled(), "Enable/Disable bonemeal for this crop");
        inventory.setItem(14, bonemealItem);

        // Junk/Keep toggle
        String junkLabel = settings.isJunkEnabled() ? "Junk" : "Keep";
        Material junkMaterial = settings.isJunkEnabled() ? Material.LAVA_BUCKET : Material.WATER_BUCKET;
        ItemStack junkItem = createToggleItem(junkMaterial, junkLabel,
                settings.isJunkEnabled(), "Junk (dump) or Keep (store) this crop");
        inventory.setItem(16, junkItem);

        // Seed dump toggle (only for crops that produce seeds)
        if (isSeedProducingCrop(cropType)) {
            String seedDumpLabel = settings.isSeedDumpEnabled() ? "Dump Seeds" : "Keep Seeds";
            Material seedDumpMaterial = settings.isSeedDumpEnabled() ? Material.REDSTONE : Material.EMERALD;
            ItemStack seedDumpItem = createToggleItem(seedDumpMaterial, seedDumpLabel,
                    settings.isSeedDumpEnabled(), "Dump or Keep seeds from this crop");
            inventory.setItem(18, seedDumpItem);
        }

        // Back button
        ItemStack backItem = createNavigationItem(Material.ARROW, "Back to Crops");
        inventory.setItem(26, backItem);

        player.openInventory(inventory);
    }

    private void addCropItem(Inventory inventory, int slot, Material cropType, UUID playerId) {
        CropSettings settings = configManager.getCropSettings(playerId, cropType);
        ItemStack item = new ItemStack(cropType);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(getCropDisplayName(cropType))
                    .color(NamedTextColor.YELLOW)
                    .decorate(TextDecoration.BOLD));
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Click to configure").color(NamedTextColor.GRAY));
            lore.add(Component.empty());
            lore.add(Component.text("Seeding: " + (settings.isSeedingEnabled() ? "✓" : "✗"))
                    .color(settings.isSeedingEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED));
            lore.add(Component.text("Collecting: " + (settings.isCollectingEnabled() ? "✓" : "✗"))
                    .color(settings.isCollectingEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED));
            lore.add(Component.text("Bonemeal: " + (settings.isBonemealEnabled() ? "✓" : "✗"))
                    .color(settings.isBonemealEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED));
            
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
    }

    private ItemStack createCategoryItem(Material material, String name, String description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name)
                    .color(NamedTextColor.YELLOW)
                    .decorate(TextDecoration.BOLD));
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(description).color(NamedTextColor.GRAY));
            lore.add(Component.text("Click to view").color(NamedTextColor.GRAY));
            
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createToggleItem(Material material, String name, boolean enabled, String description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name)
                    .color(enabled ? NamedTextColor.GREEN : NamedTextColor.RED)
                    .decorate(TextDecoration.BOLD));
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(description).color(NamedTextColor.GRAY));
            lore.add(Component.empty());
            lore.add(Component.text("Status: " + (enabled ? "ENABLED ✓" : "DISABLED ✗"))
                    .color(enabled ? NamedTextColor.GREEN : NamedTextColor.RED));
            lore.add(Component.text("Click to toggle").color(NamedTextColor.GRAY));
            
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createTitleItem(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(getCropDisplayName(material))
                    .color(NamedTextColor.GOLD)
                    .decorate(TextDecoration.BOLD));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createNavigationItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name)
                    .color(NamedTextColor.YELLOW));
            item.setItemMeta(meta);
        }
        return item;
    }

    private String getCropDisplayName(Material cropType) {
    	return switch (cropType) {
    		case WHEAT -> "Wheat";
    		case CARROT -> "Carrots";
    		case POTATO -> "Potatoes";
    		case BEETROOT -> "Beetroots";
    		case NETHER_WART -> "Nether Wart";
    		case SUGAR_CANE -> "Sugar Cane";
    		case PUMPKIN -> "Pumpkins";
    		case MELON -> "Watermelons";
    		default -> cropType.name();
    	};
    }

    private boolean isSeedProducingCrop(Material cropType) {
    	// Only crops with DISTINCT seed items should show the "Dump Seeds" toggle
    	// Self-seeding crops (where the crop IS the seed) don't need this toggle
    	return cropType == Material.WHEAT || cropType == Material.BEETROOT ||
    	       cropType == Material.PUMPKIN || cropType == Material.MELON;
    }
}
