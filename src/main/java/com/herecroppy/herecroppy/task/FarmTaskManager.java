package com.herecroppy.herecroppy.task;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FarmTaskManager {

    private final Map<UUID, FarmTask> activeTasks = new HashMap<>();
    private final Map<UUID, Integer> lastStopIndices = new HashMap<>();

    public void startTask(Player player, FarmTask task) {
        stopTask(player);
        activeTasks.put(player.getUniqueId(), task);
        task.runTaskTimer(task.getPlugin(), 0L, 1L);
    }

    public void stopTask(Player player) {
        FarmTask task = activeTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
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

    public void stopAllTasks() {
        for (FarmTask task : activeTasks.values()) {
            task.cancel();
        }
        activeTasks.clear();
    }
}
