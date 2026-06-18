package com.herecroppy.herecroppy.config;

import com.herecroppy.herecroppy.HereCroppyPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

public class CropConfigListener implements Listener {

    private final CropConfigUI configUI;
    private final CropConfigManager configManager;

    public CropConfigListener(CropConfigUI configUI, CropConfigManager configManager) {
        this.configUI = configUI;
        this.configManager = configManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Component titleComponent = event.getView().title();
        String titleStr = PlainTextComponentSerializer.plainText().serialize(titleComponent);
        if (!titleStr.contains("HereCroppy")) {
            return;
        }

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (titleStr.contains("Crop Categories")) {
            handleCategoryMenuClick(player, clicked, event.getSlot());
        } else if (titleStr.contains("Seeding Priority")) {
            handlePriorityMenuClick(player, clicked, event.getSlot(), event.isLeftClick());
        } else if (titleStr.contains("Farmland Crops")) {
            handleFarmlandMenuClick(player, clicked, event.getSlot());
        } else if (titleStr.contains("Special Crops")) {
            handleSpecialMenuClick(player, clicked, event.getSlot());
        } else if (titleStr.contains("Crop Settings")) {
            handleSettingsMenuClick(player, clicked, event.getSlot());
        }
    }

    private void handleCategoryMenuClick(Player player, ItemStack clicked, int slot) {
        Material material = clicked.getType();

        // Check for Farmland Crops (slot 11)
        if (material == Material.WHEAT || slot == 11) {
            configUI.openFarmlandCropsMenu(player);
        }
        // Check for Seeding Priority (slot 13)
        else if (material == Material.WHEAT_SEEDS || slot == 13) {
            configUI.openPriorityMenu(player);
        }
        // Check for Special Crops (slot 15)
        else if (material == Material.NETHER_WART || slot == 15) {
            configUI.openSpecialCropsMenu(player);
        }
        // Back/Close button
        else if (material == Material.ARROW) {
            player.closeInventory();
        }
    }

    private void handlePriorityMenuClick(Player player, ItemStack clicked, int slot, boolean isLeftClick) {
        Material material = clicked.getType();

        if (material == Material.ARROW || slot == 26) {
            configUI.openCategoryMenu(player);
            return;
        }

        int priorityIndex = slot - 10;
        PlayerCropConfig config = configManager.getPlayerConfig(player.getUniqueId());
        java.util.List<Material> priority = config.getSeedingPriority();

        if (priorityIndex >= 0 && priorityIndex < priority.size()) {
            if (isLeftClick) {
                // Move up
                if (priorityIndex > 0) {
                    Material temp = priority.get(priorityIndex);
                    priority.set(priorityIndex, priority.get(priorityIndex - 1));
                    priority.set(priorityIndex - 1, temp);
                }
            } else {
                // Move down
                if (priorityIndex < priority.size() - 1) {
                    Material temp = priority.get(priorityIndex);
                    priority.set(priorityIndex, priority.get(priorityIndex + 1));
                    priority.set(priorityIndex + 1, temp);
                }
            }
            config.setSeedingPriority(priority);
            configManager.saveConfiguration(player.getUniqueId());
            configUI.openPriorityMenu(player);
        }
    }

    private void handleFarmlandMenuClick(Player player, ItemStack clicked, int slot) {
        Material material = clicked.getType();

        if (material == Material.ARROW || slot == 26) {
            configUI.openCategoryMenu(player);
        } else if (isCropMaterial(material)) {
            configUI.openCropSettingsMenu(player, material);
        }
    }

    private void handleSpecialMenuClick(Player player, ItemStack clicked, int slot) {
        Material material = clicked.getType();

        if (material == Material.ARROW || slot == 26) {
            configUI.openCategoryMenu(player);
        } else if (isCropMaterial(material)) {
            configUI.openCropSettingsMenu(player, material);
        }
    }

    private void handleSettingsMenuClick(Player player, ItemStack clicked, int slot) {
        Material material = clicked.getType();

        // Get the crop type from the title or from the item in slot 4 (title item)
        Material cropType = extractCropTypeFromSettings(player);
        if (cropType == null) return;

        if (material == Material.WHEAT_SEEDS || slot == 10) {
            // Seeding toggle
            configManager.toggleSeeding(player.getUniqueId(), cropType);
            player.sendMessage(Component.text("Seeding for " + getCropName(cropType) + " toggled.")
                    .color(NamedTextColor.GREEN));
            configUI.openCropSettingsMenu(player, cropType);
        } else if (material == Material.HOPPER || slot == 12) {
            // Collecting toggle
            configManager.toggleCollecting(player.getUniqueId(), cropType);
            player.sendMessage(Component.text("Collecting for " + getCropName(cropType) + " toggled.")
                    .color(NamedTextColor.GREEN));
            configUI.openCropSettingsMenu(player, cropType);
        } else if (material == Material.BONE_MEAL || slot == 14) {
            // Bonemeal toggle
            configManager.toggleBonemeal(player.getUniqueId(), cropType);
            player.sendMessage(Component.text("Bonemeal for " + getCropName(cropType) + " toggled.")
                    .color(NamedTextColor.GREEN));
            configUI.openCropSettingsMenu(player, cropType);
        } else if (material == Material.LAVA_BUCKET || material == Material.WATER_BUCKET || slot == 16) {
            // Junk/Keep toggle
            configManager.toggleJunk(player.getUniqueId(), cropType);
            boolean isJunk = configManager.isJunkEnabled(player.getUniqueId(), cropType);
            player.sendMessage(Component.text("Disposal mode for " + getCropName(cropType) + " set to: ")
                    .color(NamedTextColor.GREEN)
                    .append(Component.text(isJunk ? "JUNK" : "KEEP")
                            .color(isJunk ? NamedTextColor.RED : NamedTextColor.BLUE)));
            configUI.openCropSettingsMenu(player, cropType);
        } else if (material == Material.REDSTONE || material == Material.EMERALD || slot == 18) {
            // Seed dump toggle
            configManager.toggleSeedDump(player.getUniqueId(), cropType);
            boolean isDumpEnabled = configManager.isSeedDumpEnabled(player.getUniqueId(), cropType);
            player.sendMessage(Component.text("Seed dumping for " + getCropName(cropType) + " set to: ")
                    .color(NamedTextColor.GREEN)
                    .append(Component.text(isDumpEnabled ? "DUMP" : "KEEP")
                            .color(isDumpEnabled ? NamedTextColor.RED : NamedTextColor.BLUE)));
            configUI.openCropSettingsMenu(player, cropType);
        } else if (material == Material.ARROW || slot == 26) {
            // Back button - determine which menu to go back to
            if (isFarmlandCrop(cropType)) {
                configUI.openFarmlandCropsMenu(player);
            } else {
                configUI.openSpecialCropsMenu(player);
            }
        }
    }

    private Material extractCropTypeFromSettings(Player player) {
        // Try to get the crop type from the inventory title or by checking the title item
        ItemStack titleItem = player.getOpenInventory().getTopInventory().getItem(4);
        if (titleItem != null && isCropMaterial(titleItem.getType())) {
            return titleItem.getType();
        }
        return null;
    }

    private boolean isCropMaterial(Material material) {
    	return material == Material.WHEAT || material == Material.CARROT ||
    	       material == Material.POTATO || material == Material.BEETROOT ||
    	       material == Material.NETHER_WART || material == Material.SUGAR_CANE ||
    	       material == Material.PUMPKIN || material == Material.MELON;
    }
   
    private boolean isFarmlandCrop(Material cropType) {
    	return cropType == Material.WHEAT || cropType == Material.CARROT ||
    	       cropType == Material.POTATO || cropType == Material.BEETROOT;
    }
   
    private String getCropName(Material cropType) {
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

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Component titleComponent = event.getView().title();
        String titleStr = PlainTextComponentSerializer.plainText().serialize(titleComponent);
        if (titleStr.contains("HereCroppy")) {
            event.setCancelled(true);
        }
    }
}
