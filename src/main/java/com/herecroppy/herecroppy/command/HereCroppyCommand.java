package com.herecroppy.herecroppy.command;

import com.herecroppy.herecroppy.HereCroppyPlugin;
import com.herecroppy.herecroppy.auraskills.AuraSkillsHelper;
import com.herecroppy.herecroppy.config.CropConfigUI;
import com.herecroppy.herecroppy.map.ScanManager;
import com.herecroppy.herecroppy.map.ScanResult;
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
    private final SetupWizardCommand setupWizardCommand;
    private final CropConfigUI cropConfigUI;

    public HereCroppyCommand(SelectionManager selectionManager, FarmTaskManager farmTaskManager, AuraSkillsHelper auraSkillsHelper, ScanManager scanManager) {
        this.selectionManager = selectionManager;
        this.farmTaskManager = farmTaskManager;
        this.auraSkillsHelper = auraSkillsHelper;
        this.scanManager = scanManager;
        this.setupWizardCommand = null;
        this.cropConfigUI = null;
    }

    public HereCroppyCommand(SelectionManager selectionManager, FarmTaskManager farmTaskManager, AuraSkillsHelper auraSkillsHelper, ScanManager scanManager, SetupWizardCommand setupWizardCommand, CropConfigUI cropConfigUI) {
        this.selectionManager = selectionManager;
        this.farmTaskManager = farmTaskManager;
        this.auraSkillsHelper = auraSkillsHelper;
        this.scanManager = scanManager;
        this.setupWizardCommand = setupWizardCommand;
        this.cropConfigUI = cropConfigUI;
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
            player.sendMessage(Component.text("Usage: /herecroppy <start|stop|restart|clear|setup|config>")
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
                    player.sendMessage(Component.text("Scanning area... Please wait.")
                            .color(NamedTextColor.GREEN));
                    scanManager.scanAreaAsync(player.getUniqueId(),
                            selectionManager.getPointA(player.getUniqueId()),
                            selectionManager.getPointB(player.getUniqueId()),
                            result -> startFarming(player, result));
                    return true;
                }

                ScanResult scanResult = scanManager.getScanResult(player.getUniqueId());
                startFarming(player, scanResult);
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

                if (!selectionManager.hasCompleteSelection(player.getUniqueId())) {
                    player.sendMessage(Component.text("Selection missing! Please reselect the area.")
                            .color(NamedTextColor.RED));
                    return true;
                }

                if (!scanManager.hasScan(player.getUniqueId())) {
                    player.sendMessage(Component.text("Scanning area... Please wait.")
                            .color(NamedTextColor.GREEN));
                    scanManager.scanAreaAsync(player.getUniqueId(),
                            selectionManager.getPointA(player.getUniqueId()),
                            selectionManager.getPointB(player.getUniqueId()),
                            result -> restartFarming(player, result));
                    return true;
                }

                ScanResult scanResult = scanManager.getScanResult(player.getUniqueId());
                restartFarming(player, scanResult);
            }
            case "clear" -> {
                selectionManager.clearSelection(player.getUniqueId());
                scanManager.clearScan(player.getUniqueId());
                farmTaskManager.clearLastStop(player);
                HereCroppyPlugin.getInstance().getSetupManager().clearSetupConfig(player.getUniqueId());
                player.sendMessage(Component.text("Selection and setup configuration cleared.")
                        .color(NamedTextColor.GREEN));
            }
            case "setup" -> {
                if (setupWizardCommand != null) {
                    setupWizardCommand.onCommand(player, command, label, args);
                } else {
                    player.sendMessage(Component.text("Setup wizard is not available.")
                            .color(NamedTextColor.RED));
                }
            }
            case "config" -> {
                if (cropConfigUI != null) {
                    cropConfigUI.openCategoryMenu(player);
                } else {
                    player.sendMessage(Component.text("Crop configuration is not available.")
                            .color(NamedTextColor.RED));
                }
            }
            default -> player.sendMessage(Component.text("Usage: /herecroppy <start|stop|restart|clear|setup|config>")
                    .color(NamedTextColor.YELLOW));
        }

        return true;
    }

    private void startFarming(Player player, ScanResult scanResult) {
        List<Location> path = PathGenerator.generateSafePath(scanResult);

        if (path.isEmpty()) {
            player.sendMessage(Component.text("The selected area has no walkable blocks.")
                    .color(NamedTextColor.RED));
            return;
        }

        FarmTask task = new FarmTask(HereCroppyPlugin.getInstance(), player, path, auraSkillsHelper, scanManager, selectionManager, scanResult);
        int startIndex = PathGenerator.findClosestIndex(path, scanResult.getPointB());
        task.setCurrentIndex(startIndex);
        farmTaskManager.startTask(player, task);
        farmTaskManager.clearLastStop(player);

        player.sendMessage(Component.text("Auto-farming enabled! Walking ")
                .color(NamedTextColor.GREEN)
                .append(Component.text(String.valueOf(path.size())).color(NamedTextColor.YELLOW))
                .append(Component.text(" blocks from index ").color(NamedTextColor.GREEN))
                .append(Component.text(String.valueOf(startIndex + 1)).color(NamedTextColor.YELLOW))
                .append(Component.text(".").color(NamedTextColor.GREEN)));
    }

    private void restartFarming(Player player, ScanResult scanResult) {
        List<Location> path = PathGenerator.generateSafePath(scanResult);

        if (path.isEmpty()) {
            player.sendMessage(Component.text("The selected area has no walkable blocks.")
                    .color(NamedTextColor.RED));
            return;
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
}
