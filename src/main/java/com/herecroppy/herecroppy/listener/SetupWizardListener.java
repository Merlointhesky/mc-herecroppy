package com.herecroppy.herecroppy.listener;

import com.herecroppy.herecroppy.setup.SetupManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class SetupWizardListener implements Listener {

    private final SetupManager setupManager;

    public SetupWizardListener(SetupManager setupManager) {
        this.setupManager = setupManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        // Check if player is in setup mode
        if (!setupManager.isInSetup(player.getUniqueId())) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isHoe(item.getType())) return;

        if (event.getClickedBlock() == null) return;

        event.setCancelled(true);
        Location clicked = event.getClickedBlock().getLocation();
        int step = setupManager.getCurrentStep(player.getUniqueId());

        switch (step) {
            case 0 -> {
                setupManager.setDumpUnwantedBox(player.getUniqueId(), clicked);
                player.sendMessage(Component.text("✓ Dump unwanted box set at ")
                        .color(NamedTextColor.GREEN)
                        .append(Component.text(formatLocation(clicked)).color(NamedTextColor.YELLOW))
                        .append(Component.text(". Now shift-click the box to dump crops to keep.").color(NamedTextColor.GREEN)));
            }
            case 1 -> {
                setupManager.setDumpKeepBox(player.getUniqueId(), clicked);
                player.sendMessage(Component.text("✓ Dump keep box set at ")
                        .color(NamedTextColor.GREEN)
                        .append(Component.text(formatLocation(clicked)).color(NamedTextColor.YELLOW))
                        .append(Component.text(". Now shift-click the box to collect bonemeal from.").color(NamedTextColor.GREEN)));
            }
            case 2 -> {
                setupManager.setBonemealBox(player.getUniqueId(), clicked);
                player.sendMessage(Component.text("✓ Bonemeal collection box set at ")
                        .color(NamedTextColor.GREEN)
                        .append(Component.text(formatLocation(clicked)).color(NamedTextColor.YELLOW))
                        .append(Component.text(". Type the amount of bonemeal to collect per loop (or type 'cancel' to abort).").color(NamedTextColor.GREEN)));
            }
        }
    }

    private boolean isHoe(Material material) {
        return material.name().endsWith("_HOE");
    }

    private String formatLocation(Location loc) {
        return loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
    }
}
