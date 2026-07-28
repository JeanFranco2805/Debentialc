package org.debentialc.worldguard;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.bukkit.RegionQuery;
import noppes.npcs.api.entity.IDBCPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.debentialc.service.CC;
import org.debentialc.service.General;

import java.lang.reflect.Method;

public class WorldGuardListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (WorldGuardFlagManager.ENTRY_LVL_FLAG == null && WorldGuardFlagManager.ENTRY_QUEST_FLAG == null) {
            return;
        }

        Location to = event.getTo();
        if (to == null) return;

        Player player = event.getPlayer();
        if (!isBlocked(to, player)) {
            return;
        }

        Location from = event.getFrom();
        Location safe;
        if (!isBlocked(from, player)) {
            safe = from;
        } else {
            safe = findSafeExitLocation(player, from);
            if (safe == null) {
                safe = player.getWorld().getSpawnLocation();
            }
        }

        event.setTo(safe);
        player.sendMessage(CC.translate("&c✗ " + getBlockReason(to, player)));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (WorldGuardFlagManager.ENTRY_LVL_FLAG == null && WorldGuardFlagManager.ENTRY_QUEST_FLAG == null) {
            return;
        }

        Player player = event.getPlayer();
        Location loc = player.getLocation();
        if (!isBlocked(loc, player)) {
            return;
        }

        Location safe = findSafeExitLocation(player, loc);
        if (safe == null) {
            safe = player.getWorld().getSpawnLocation();
        }

        player.teleport(safe);
        player.sendMessage(CC.translate("&c✗ " + getBlockReason(loc, player)));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (WorldGuardFlagManager.ENTRY_LVL_FLAG == null && WorldGuardFlagManager.ENTRY_QUEST_FLAG == null) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        if (from.getBlockX() == to.getBlockX() &&
                from.getBlockY() == to.getBlockY() &&
                from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();

        boolean fromBlocked = isBlocked(from, player);
        boolean toBlocked = isBlocked(to, player);

        if (!toBlocked) {
            return;
        }

        event.setCancelled(true);

        String reason = getBlockReason(to, player);
        player.sendMessage(CC.translate("&c✗ " + reason));

        if (!fromBlocked) {
            player.teleport(from);
        } else {
            Location exit = findSafeExitLocation(player, from);
            if (exit != null) {
                player.teleport(exit);
            } else {
                player.teleport(player.getWorld().getSpawnLocation());
                player.sendMessage(CC.translate("&c✗ No se encontró una salida cercana. Has sido teleportado al spawn."));
            }
        }
    }

    private boolean isBlocked(Location location, Player player) {
        return isBlockedByLevel(location, player) || isBlockedByQuest(location, player);
    }

    private boolean isBlockedByLevel(Location location, Player player) {
        if (WorldGuardFlagManager.ENTRY_LVL_FLAG == null) return false;
        RegionQuery query = WorldGuardPlugin.inst().getRegionContainer().createQuery();
        Integer requiredLevel = query.queryValue(location, player, WorldGuardFlagManager.ENTRY_LVL_FLAG);
        if (requiredLevel == null || requiredLevel <= 0) return false;
        int playerLevel = getPlayerLevel(player);
        return playerLevel < requiredLevel;
    }

    private boolean isBlockedByQuest(Location location, Player player) {
        if (WorldGuardFlagManager.ENTRY_QUEST_FLAG == null) return false;
        RegionQuery query = WorldGuardPlugin.inst().getRegionContainer().createQuery();
        Integer questId = query.queryValue(location, player, WorldGuardFlagManager.ENTRY_QUEST_FLAG);
        if (questId == null || questId <= 0) return false;
        return !hasFinishedQuest(player, questId);
    }

    private String getBlockReason(Location location, Player player) {
        if (WorldGuardFlagManager.ENTRY_LVL_FLAG != null) {
            RegionQuery query = WorldGuardPlugin.inst().getRegionContainer().createQuery();
            Integer requiredLevel = query.queryValue(location, player, WorldGuardFlagManager.ENTRY_LVL_FLAG);
            if (requiredLevel != null && requiredLevel > 0) {
                int playerLevel = getPlayerLevel(player);
                if (playerLevel < requiredLevel) {
                    return "Necesitas ser nivel " + requiredLevel + " o superior para estar aquí.";
                }
            }
        }
        if (WorldGuardFlagManager.ENTRY_QUEST_FLAG != null) {
            RegionQuery query = WorldGuardPlugin.inst().getRegionContainer().createQuery();
            Integer questId = query.queryValue(location, player, WorldGuardFlagManager.ENTRY_QUEST_FLAG);
            if (questId != null && questId > 0) {
                if (!hasFinishedQuest(player, questId)) {
                    return "Necesitas completar la misión " + questId + " para estar aquí.";
                }
            }
        }
        return "No puedes estar en esta zona.";
    }

    private Location findSafeExitLocation(Player player, Location start) {
        for (int distance = 1; distance <= 50; distance++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    Location candidate = start.clone().add(dx * distance, 0, dz * distance);
                    candidate.setX(candidate.getBlockX() + 0.5);
                    candidate.setZ(candidate.getBlockZ() + 0.5);
                    candidate.setY(start.getY());
                    if (!isBlocked(candidate, player)) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private int getPlayerLevel(Player player) {
        try {
            IDBCPlayer idbcPlayer = General.getDBCPlayer(player.getName());
            int level = idbcPlayer.getStat("jrmcLevelI");
            if (level <= 0) {
                level = idbcPlayer.getStat("jrmcLvlI");
            }
            if (level <= 0) {
                level = idbcPlayer.getStat("jrmcLevel");
            }
            if (level <= 0) {
                level = idbcPlayer.getStat("jrmcLvl");
            }
            if (level <= 0) {
                level = player.getLevel();
            }
            return level;
        } catch (Exception e) {
            return player.getLevel();
        }
    }

    private boolean hasFinishedQuest(Player player, int questId) {
        try {
            IDBCPlayer idbcPlayer = General.getDBCPlayer(player.getName());
            Method method = idbcPlayer.getClass().getMethod("hasFinishedQuest", int.class);
            Object result = method.invoke(idbcPlayer, questId);
            return result instanceof Boolean && (Boolean) result;
        } catch (NoSuchMethodException e) {
            player.sendMessage(CC.translate("&c✗ El sistema de quests no está disponible."));
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
