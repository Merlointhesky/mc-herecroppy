package com.herecroppy.herecroppy.command;

import com.herecroppy.herecroppy.HereCroppyPlugin;
import com.herecroppy.herecroppy.auraskills.AuraSkillsHelper;
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

    public HereCroppyCommand(SelectionManager selectionManager, FarmTaskManager farmTaskManager, AuraSkillsHelper auraSkillsHelper) {
        this.selectionManager = selectionManager;
        this.farmTaskManager = farmTaskManager;
        this.auraSkillsHelper = auraSkillsHelper;
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
            player.sendMessage(Component.text("Usage: /herecroppy <start|stop|clear>")
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

                Location pointA = selectionManager.getPointA(player.getUniqueId());
                Location pointB = selectionManager.getPointB(player.getUniqueId());
                List<Location> path = PathGenerator.generateSnakePath(pointA, pointB);

                if (path.isEmpty()) {
                    player.sendMessage(Component.text("The selected area is empty or invalid.")
                            .color(NamedTextColor.RED));
                    return true;
                }

                FarmTask task = new FarmTask(HereCroppyPlugin.getInstance(), player, path, auraSkillsHelper);
                farmTaskManager.startTask(player, task);

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
            case "clear" -> {
                selectionManager.clearSelection(player.getUniqueId());
                player.sendMessage(Component.text("Selection cleared.")
                        .color(NamedTextColor.GREEN));
            }
            default -> player.sendMessage(Component.text("Usage: /herecroppy <start|stop|clear>")
                    .color(NamedTextColor.YELLOW));
        }

        return true;
    }
}
