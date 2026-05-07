package com.herecroppy.herecroppy.listener;

import com.herecroppy.herecroppy.selection.SelectionManager;
import com.herecroppy.herecroppy.task.FarmTaskManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class FarmListener implements Listener {

    private final SelectionManager selectionManager;
    private final FarmTaskManager farmTaskManager;

    public FarmListener(SelectionManager selectionManager, FarmTaskManager farmTaskManager) {
        this.selectionManager = selectionManager;
        this.farmTaskManager = farmTaskManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isHoe(item.getType())) return;

        if (event.getClickedBlock() == null) return;

        Location clicked = event.getClickedBlock().getLocation();

        if (selectionManager.getPointA(player.getUniqueId()) == null) {
            selectionManager.setPointA(player.getUniqueId(), clicked);
            player.sendMessage(Component.text("Point A set at ")
                    .color(NamedTextColor.GREEN)
                    .append(Component.text(formatLocation(clicked)).color(NamedTextColor.YELLOW))
                    .append(Component.text(". Shift-click again to set Point B.").color(NamedTextColor.GREEN)));
        } else if (selectionManager.getPointB(player.getUniqueId()) == null) {
            selectionManager.setPointB(player.getUniqueId(), clicked);
            player.sendMessage(Component.text("Point B set at ")
                    .color(NamedTextColor.GREEN)
                    .append(Component.text(formatLocation(clicked)).color(NamedTextColor.YELLOW))
                    .append(Component.text(". Use ").color(NamedTextColor.GREEN))
                    .append(Component.text("/herecroppy start").color(NamedTextColor.YELLOW))
                    .append(Component.text(" to begin.").color(NamedTextColor.GREEN)));
        } else {
            selectionManager.clearSelection(player.getUniqueId());
            selectionManager.setPointA(player.getUniqueId(), clicked);
            player.sendMessage(Component.text("Selection reset. Point A set at ")
                    .color(NamedTextColor.GREEN)
                    .append(Component.text(formatLocation(clicked)).color(NamedTextColor.YELLOW))
                    .append(Component.text(". Shift-click again to set Point B.").color(NamedTextColor.GREEN)));
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        farmTaskManager.stopTask(player);
        selectionManager.clearSelection(player.getUniqueId());
    }

    private boolean isHoe(Material material) {
        return switch (material) {
            case WOODEN_HOE, STONE_HOE, IRON_HOE, GOLDEN_HOE, DIAMOND_HOE, NETHERITE_HOE -> true;
            default -> false;
        };
    }

    private String formatLocation(Location loc) {
        return String.format("(%d, %d, %d)", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
}
