package com.herecroppy.herecroppy.command;

import com.herecroppy.herecroppy.setup.SetupManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SetupWizardCommand implements CommandExecutor, Listener {

    private final SetupManager setupManager;
    private final Plugin plugin;
    private final Map<UUID, Long> setupTimeouts = new HashMap<>();
    private static final long SETUP_TIMEOUT_MS = 300000; // 5 minutes

    public SetupWizardCommand(SetupManager setupManager, Plugin plugin) {
        this.setupManager = setupManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!")
                    .color(NamedTextColor.RED));
            return true;
        }

        // Handle cancel subcommand from /herecroppy setup cancel
        if (args.length > 1 && args[1].equalsIgnoreCase("cancel")) {
            if (setupManager.isInSetup(player.getUniqueId())) {
                setupManager.cancelSetup(player.getUniqueId());
                setupTimeouts.remove(player.getUniqueId());
                player.sendMessage(Component.text("Setup wizard cancelled.")
                        .color(NamedTextColor.YELLOW));
            } else {
                player.sendMessage(Component.text("You are not in setup mode.")
                        .color(NamedTextColor.YELLOW));
            }
            return true;
        }

        // Handle cancel from direct command /herecroppy setup cancel
        if (args.length > 0 && args[0].equalsIgnoreCase("cancel")) {
            if (setupManager.isInSetup(player.getUniqueId())) {
                setupManager.cancelSetup(player.getUniqueId());
                setupTimeouts.remove(player.getUniqueId());
                player.sendMessage(Component.text("Setup wizard cancelled.")
                        .color(NamedTextColor.YELLOW));
            } else {
                player.sendMessage(Component.text("You are not in setup mode.")
                        .color(NamedTextColor.YELLOW));
            }
            return true;
        }

        // Handle bonemeal subcommand from /herecroppy setup bonemeal [nn]
        if (args.length > 2 && args[1].equalsIgnoreCase("bonemeal")) {
            try {
                int amount = Integer.parseInt(args[2]);
                if (amount < 0) {
                    player.sendMessage(Component.text("Please enter a non-negative number.")
                            .color(NamedTextColor.RED));
                    return true;
                }

                setupManager.updateBonemealAmount(player.getUniqueId(), amount);
                player.sendMessage(Component.text("Bonemeal per loop set to: ")
                        .color(NamedTextColor.GREEN)
                        .append(Component.text(amount).color(NamedTextColor.YELLOW)));
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Please enter a valid number.")
                        .color(NamedTextColor.RED));
            }
            return true;
        }

        if (setupManager.isInSetup(player.getUniqueId())) {
            player.sendMessage(Component.text("You are already in setup mode! Type 'cancel' to abort or continue with the setup.")
                    .color(NamedTextColor.YELLOW));
            return true;
        }

        setupManager.startSetup(player.getUniqueId());
        setupTimeouts.put(player.getUniqueId(), System.currentTimeMillis());
        
        player.sendMessage(Component.text("═══════════════════════════════════════")
                .color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("HereCroppy Setup Wizard Started")
                .color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("═══════════════════════════════════════")
                .color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("Step 1 of 3: Dump Unwanted Crops")
                .color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("Hold a hoe and shift-right-click the box where you want to dump unwanted crops.")
                .color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("Type '/herecroppy setup cancel' to abort.")
                .color(NamedTextColor.GRAY));

        return true;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (!setupManager.isInSetup(playerId)) return;

        int step = setupManager.getCurrentStep(playerId);
        if (step != 3) return; // Only handle chat input at step 3

        event.setCancelled(true);

        String message = event.getMessage().trim();

        if (message.equalsIgnoreCase("cancel")) {
            setupManager.cancelSetup(playerId);
            setupTimeouts.remove(playerId);
            player.sendMessage(Component.text("Setup wizard cancelled.")
                    .color(NamedTextColor.YELLOW));
            return;
        }

        try {
            int amount = Integer.parseInt(message);
            if (amount <= 0) {
                player.sendMessage(Component.text("Please enter a positive number.")
                        .color(NamedTextColor.RED));
                return;
            }

            setupManager.setBonemealPerLoop(playerId, amount);
            setupTimeouts.remove(playerId);

            player.sendMessage(Component.text("═══════════════════════════════════════")
                    .color(NamedTextColor.GOLD));
            player.sendMessage(Component.text("Setup Complete!")
                    .color(NamedTextColor.GREEN));
            player.sendMessage(Component.text("═══════════════════════════════════════")
                    .color(NamedTextColor.GOLD));
            player.sendMessage(Component.text("Bonemeal per loop: ")
                    .color(NamedTextColor.GREEN)
                    .append(Component.text(amount).color(NamedTextColor.YELLOW)));
            player.sendMessage(Component.text("You can now use /herecroppy start to begin farming!")
                    .color(NamedTextColor.GREEN));

        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Please enter a valid number.")
                    .color(NamedTextColor.RED));
        }
    }

    public void checkTimeouts() {
        long now = System.currentTimeMillis();
        setupTimeouts.entrySet().removeIf(entry -> {
            if (now - entry.getValue() > SETUP_TIMEOUT_MS) {
                UUID playerId = entry.getKey();
                setupManager.cancelSetup(playerId);
                Player player = plugin.getServer().getPlayer(playerId);
                if (player != null) {
                    player.sendMessage(Component.text("Setup wizard timed out after 5 minutes.")
                            .color(NamedTextColor.YELLOW));
                }
                return true;
            }
            return false;
        });
    }
}
