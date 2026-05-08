package com.herecroppy.herecroppy.command;

import com.herecroppy.herecroppy.HereCroppyPlugin;
import com.herecroppy.herecroppy.auraskills.AuraSkillsHelper;
import com.herecroppy.herecroppy.map.ScanManager;
import com.herecroppy.herecroppy.path.PathGenerator;
import com.herecroppy.herecroppy.selection.SelectionManager;
import com.herecroppy.herecroppy.task.FarmTask;
import com.herecroppy.herecroppy.task.FarmTaskManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HereCroppyCommand implements CommandExecutor {

    private final SelectionManager selectionManager;
    private final FarmTaskManager farmTaskManager;
    private final AuraSkillsHelper auraSkillsHelper;
    private final ScanManager scanManager;

    public HereCroppyCommand(SelectionManager selectionManager, FarmTaskManager farmTaskManager, AuraSkillsHelper auraSkillsHelper, ScanManager scanManager) {
        this.selectionManager = selectionManager;
        this.farmTaskManager = farmTaskManager;
        this.auraSkillsHelper = auraSkillsHelper;
        this.scanManager = scanManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!")
                    .color(NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(Component.text("Usage: /herecroppy <start|stop|restart|clear>")
                    .color(NamedTextColor.YELLOW));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "start" -> {
                if (farmTaskManager.isFarming(player)) {
                    player.sendMessage(Component.text("Auto-farming is already enabled!")
                            .color(NamedTextColor.YELLOW));
                    return true;
                }

                if (!selectionManager.hasCompleteSelection(player.getUniqueId())) {
                    player.sendMessage(Component.text("You must set two points first! Shift-right-click with a hoe to set Point A and Point B.")
                            .color(NamedTextColor.RED));
                    return true;
                }

                if (!scanManager.hasScan(player.getUniqueId())) {
                    player.sendMessage(Component.text("Area has not been scanned yet. Please select Point A and Point B first.")
                            .color(NamedTextColor.RED));
                    return true;
                }

                var scanResult = scanManager.getScanResult(player.getUniqueId());
                List<Location> path = PathGenerator.generateSafePath(scanResult);

                if (path.isEmpty()) {
                    player.sendMessage(Component.text("The selected area has no walkable blocks.")
                            .color(NamedTextColor.RED));
                    return true;
                }

                FarmTask task = new FarmTask(HereCroppyPlugin.getInstance(), player, path, auraSkillsHelper, scanManager, selectionManager, scanResult);
                farmTaskManager.startTask(player, task);
                farmTaskManager.clearLastStop(player);

                player.sendMessage(Component.text("Auto-farming enabled! Walking ")
                        .color(NamedTextColor.GREEN)
                        .append(Component.text(String.valueOf(path.size())).color(NamedTextColor.YELLOW))
                        .append(Component.text(" blocks.").color(NamedTextColor.GREEN)));
            }
            case "stop" -> {
                if (!farmTaskManager.isFarming(player)) {
                    player.sendMessage(Component.text("Auto-farming is not enabled!")
                            .color(NamedTextColor.YELLOW));
                } else {
                    farmTaskManager.stopTask(player);
                    player.sendMessage(Component.text("Auto-farming disabled.")
                            .color(NamedTextColor.GREEN));
                }
            }
            case "restart" -> {
                if (!farmTaskManager.hasLastStop(player)) {
                    player.sendMessage(Component.text("No paused session to restart. Use /herecroppy start instead.")
                            .color(NamedTextColor.YELLOW));
                    return true;
                }

                if (!scanManager.hasScan(player.getUniqueId())) {
                    player.sendMessage(Component.text("Area scan is no longer available. Please reselect the area.")
                            .color(NamedTextColor.RED));
                    return true;
                }

                var scanResult = scanManager.getScanResult(player.getUniqueId());
                List<Location> path = PathGenerator.generateSafePath(scanResult);

                if (path.isEmpty()) {
                    player.sendMessage(Component.text("The selected area has no walkable blocks.")
                            .color(NamedTextColor.RED));
                    return true;
                }

                int lastIndex = farmTaskManager.getLastStopIndex(player);
                FarmTask task = new FarmTask(HereCroppyPlugin.getInstance(), player, path, auraSkillsHelper, scanManager, selectionManager, scanResult);
                if (lastIndex >= 0 && lastIndex < path.size()) {
                    task.setCurrentIndex(lastIndex);
                }
                farmTaskManager.startTask(player, task);
                farmTaskManager.clearLastStop(player);

                player.sendMessage(Component.text("Auto-farming restarted from block ")
                        .color(NamedTextColor.GREEN)
                        .append(Component.text(String.valueOf(task.getCurrentIndex() + 1)).color(NamedTextColor.YELLOW))
                        .append(Component.text(" of ").color(NamedTextColor.GREEN))
                        .append(Component.text(String.valueOf(path.size())).color(NamedTextColor.YELLOW))
                        .append(Component.text(".").color(NamedTextColor.GREEN)));
            }
            case "clear" -> {
                selectionManager.clearSelection(player.getUniqueId());
                scanManager.clearScan(player.getUniqueId());
                farmTaskManager.clearLastStop(player);
                player.sendMessage(Component.text("Selection cleared.")
                        .color(NamedTextColor.GREEN));
            }
            default -> player.sendMessage(Component.text("Usage: /herecroppy <start|stop|restart|clear>")
                    .color(NamedTextColor.YELLOW));
        }

        return true;
    }
}
