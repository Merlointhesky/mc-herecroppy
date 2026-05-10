package com.herecroppy.herecroppy.command;

import com.herecroppy.herecroppy.config.CropConfigUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CropConfigCommand implements CommandExecutor {

    private final CropConfigUI configUI;

    public CropConfigCommand(CropConfigUI configUI) {
        this.configUI = configUI;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!")
                    .color(NamedTextColor.RED));
            return true;
        }

        configUI.openCategoryMenu(player);
        return true;
    }
}
