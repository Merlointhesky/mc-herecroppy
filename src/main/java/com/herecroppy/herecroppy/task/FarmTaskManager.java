package com.herecroppy.herecroppy.task;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FarmTaskManager {

    private final Map<UUID, FarmTask> activeTasks = new HashMap<>();
    private final Map<UUID, AutoDefenseTask> activeDefenseTasks = new HashMap<>();
    private final Map<UUID, Integer> lastStopIndices = new HashMap<>();
    private final java.util.Set<UUID> quittingPlayers = new java.util.HashSet<>();

    public void startTask(Player player, FarmTask task) {
        stopTask(player);
        stopAutoDefense(player, true);
        activeTasks.put(player.getUniqueId(), task);
        task.runTaskTimer(task.getPlugin(), 0L, 1L);
    }

    public void stopTask(Player player) {
        FarmTask task = activeTasks.remove(player.getUniqueId());
        if (task != null) {
            recordInventoryFullStop(player, task.getCurrentIndex());
            task.sendActivitySummary();
            task.cancel();
        }
    }

    public void removeActiveTask(UUID playerId) {
        activeTasks.remove(playerId);
    }

    public FarmTask getActiveTask(Player player) {
        return activeTasks.get(player.getUniqueId());
    }

    public void recordInventoryFullStop(Player player, int index) {
        lastStopIndices.put(player.getUniqueId(), index);
    }

    public boolean hasLastStop(Player player) {
        return lastStopIndices.containsKey(player.getUniqueId());
    }

    public int getLastStopIndex(Player player) {
        return lastStopIndices.getOrDefault(player.getUniqueId(), 0);
    }

    public void clearLastStop(Player player) {
        lastStopIndices.remove(player.getUniqueId());
    }

    public boolean isFarming(Player player) {
        return activeTasks.containsKey(player.getUniqueId());
    }

    public void startAutoDefense(Player player) {
        if (activeDefenseTasks.containsKey(player.getUniqueId())) return;
        AutoDefenseTask defenseTask = new AutoDefenseTask(com.herecroppy.herecroppy.HereCroppyPlugin.getInstance(), player);
        activeDefenseTasks.put(player.getUniqueId(), defenseTask);
        defenseTask.runTaskTimer(com.herecroppy.herecroppy.HereCroppyPlugin.getInstance(), 0L, 1L);
        player.sendMessage(net.kyori.adventure.text.Component.text("⚔ AFK Auto-defense activated! Stand still; we will protect you. Move manually to deactivate.").color(net.kyori.adventure.text.format.NamedTextColor.GOLD));
    }

    public void stopAutoDefense(Player player, boolean silent) {
        AutoDefenseTask defenseTask = activeDefenseTasks.remove(player.getUniqueId());
        if (defenseTask != null) {
            defenseTask.cancel();
            if (!silent) {
                player.sendMessage(net.kyori.adventure.text.Component.text("⚔ AFK Auto-defense deactivated.").color(net.kyori.adventure.text.format.NamedTextColor.YELLOW));
            }
        }
    }

    public boolean hasAutoDefense(Player player) {
        return activeDefenseTasks.containsKey(player.getUniqueId());
    }

    public AutoDefenseTask getAutoDefenseTask(Player player) {
        return activeDefenseTasks.get(player.getUniqueId());
    }

    public void markQuitting(UUID playerId) {
        quittingPlayers.add(playerId);
    }

    public void removeQuitting(UUID playerId) {
        quittingPlayers.remove(playerId);
    }

    public boolean isQuitting(UUID playerId) {
        return quittingPlayers.contains(playerId);
    }

    public void stopAllTasks() {
        for (FarmTask task : activeTasks.values()) {
            task.cancel();
        }
        activeTasks.clear();
        for (AutoDefenseTask task : activeDefenseTasks.values()) {
            task.cancel();
        }
        activeDefenseTasks.clear();
    }
}
