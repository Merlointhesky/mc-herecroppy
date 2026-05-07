package com.herecroppy.herecroppy.selection;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SelectionManager {

    private final Map<UUID, Location[]> selections = new HashMap<>();

    public void setPointA(UUID playerId, Location location) {
        Location[] locs = selections.computeIfAbsent(playerId, k -> new Location[2]);
        locs[0] = location.clone();
    }

    public void setPointB(UUID playerId, Location location) {
        Location[] locs = selections.computeIfAbsent(playerId, k -> new Location[2]);
        locs[1] = location.clone();
    }

    public Location getPointA(UUID playerId) {
        Location[] locs = selections.get(playerId);
        return locs != null ? locs[0] : null;
    }

    public Location getPointB(UUID playerId) {
        Location[] locs = selections.get(playerId);
        return locs != null ? locs[1] : null;
    }

    public boolean hasCompleteSelection(UUID playerId) {
        Location[] locs = selections.get(playerId);
        return locs != null && locs[0] != null && locs[1] != null;
    }

    public void clearSelection(UUID playerId) {
        selections.remove(playerId);
    }

    public void clearAll() {
        selections.clear();
    }
}
