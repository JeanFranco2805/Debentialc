package org.debentialc.factions.zones;

import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.event.EventFactionsChunkChangeType;
import com.massivecraft.factions.event.EventFactionsChunksChange;
import com.massivecraft.massivecore.ps.PS;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.debentialc.factions.menus.FactionAdminZoneMenu;
import org.debentialc.factions.menus.FactionZoneInputManager;
import org.debentialc.service.CC;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FactionZoneListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onChunksChange(EventFactionsChunksChange event) {
        if (event.isCancelled()) return;

        Faction newFaction = event.getNewFaction();
        if (newFaction == null) return;

        Set<PS> eventChunks = event.getChunks();
        if (eventChunks == null || eventChunks.isEmpty()) return;

        // Determinar tipo de cambio (claim/unclaim)
        EventFactionsChunkChangeType type = null;
        Map<PS, EventFactionsChunkChangeType> chunkTypes = event.getChunkType();
        for (PS ps : eventChunks) {
            type = chunkTypes.get(ps);
            if (type != null) break;
        }
        if (type == null) return;

        // Verificar si algún chunk está en una zona
        FactionZone zone = findZoneForChunks(eventChunks);
        if (zone == null) return;

        // Si es en zona, cancelar el evento original
        event.setCancelled(true);

        if (type == EventFactionsChunkChangeType.BUY) {
            handleZoneClaim(event, zone, newFaction);
        } else if (type == EventFactionsChunkChangeType.SELL) {
            handleZoneUnclaim(event, zone, newFaction);
        }
    }

    private void handleZoneClaim(EventFactionsChunksChange event, FactionZone zone, Faction faction) {
        Player player = event.getMSender() != null ? Bukkit.getPlayer(event.getMSender().getUuid()) : null;

        // Verificar power
        if (faction.getPower() < zone.getPowerCost()) {
            if (player != null) {
                player.sendMessage(CC.translate("&c✗ Necesitas &f" + zone.getPowerCost() + " &c de power para reclamar &f" + zone.getDisplayName() + "&c."));
            }
            return;
        }

        // Verificar que la zona no esté ya reclamada
        if (isZoneClaimed(zone, faction)) {
            if (player != null) {
                player.sendMessage(CC.translate("&c✗ La zona &f" + zone.getDisplayName() + " &cya está reclamada por tu facción."));
            }
            return;
        }

        // Verificar que todos los chunks estén libres o sean reclamables
        for (String chunkKey : zone.getChunks()) {
            String[] parts = chunkKey.split(",");
            int cx = Integer.parseInt(parts[0]);
            int cz = Integer.parseInt(parts[1]);
            PS ps = PS.valueOf(zone.getWorld(), null, null, null, null, null, null, cx, cz, null, null, null, null, null);
            Faction at = BoardColl.get().getFactionAt(ps);
            if (at != null && !at.isNone() && !at.getId().equals(faction.getId())) {
                if (player != null) {
                    player.sendMessage(CC.translate("&c✗ La zona &f" + zone.getDisplayName() + " &cestá ocupada por otra facción."));
                }
                return;
            }
        }

        // Reclamar todos los chunks
        for (String chunkKey : zone.getChunks()) {
            String[] parts = chunkKey.split(",");
            int cx = Integer.parseInt(parts[0]);
            int cz = Integer.parseInt(parts[1]);
            PS ps = PS.valueOf(zone.getWorld(), null, null, null, null, null, null, cx, cz, null, null, null, null, null);
            BoardColl.get().setFactionAt(ps, faction);
        }

        if (player != null) {
            player.sendMessage(CC.translate("&a✓ Tu facción ha reclamado toda la zona &f" + zone.getDisplayName() + "&a!"));
        }
        Bukkit.broadcastMessage(CC.translate("&a✓ La facción &f" + faction.getName() + " &aha reclamado la zona &f" + zone.getDisplayName() + "&a!"));
    }

    private void handleZoneUnclaim(EventFactionsChunksChange event, FactionZone zone, Faction faction) {
        Player player = event.getMSender() != null ? Bukkit.getPlayer(event.getMSender().getUuid()) : null;

        if (faction.getPower() > 0) {
            if (player != null) {
                player.sendMessage(CC.translate("&c✗ Para desreclamar &f" + zone.getDisplayName() + " &cdebes bajar el power de tu facción a &f0&c."));
            }
            return;
        }

        // Desreclamar todas las zonas de la facción
        Set<FactionZone> factionZones = getFactionZones(faction);
        for (FactionZone z : factionZones) {
            for (String chunkKey : z.getChunks()) {
                String[] parts = chunkKey.split(",");
                int cx = Integer.parseInt(parts[0]);
                int cz = Integer.parseInt(parts[1]);
                PS ps = PS.valueOf(z.getWorld(), null, null, null, null, null, null, cx, cz, null, null, null, null, null);
                BoardColl.get().setFactionAt(ps, Faction.get("none"));
            }
        }

        if (player != null) {
            player.sendMessage(CC.translate("&a✓ Has desreclamado todas las zonas especiales de tu facción."));
        }
    }

    private FactionZone findZoneForChunks(Set<PS> chunks) {
        for (PS ps : chunks) {
            if (ps.getWorld() == null || ps.getChunkX() == null || ps.getChunkZ() == null) continue;
            FactionZone zone = FactionZoneManager.getInstance().getZoneAtChunk(ps.getWorld(), ps.getChunkX(), ps.getChunkZ());
            if (zone != null) return zone;
        }
        return null;
    }

    private boolean isZoneClaimed(FactionZone zone, Faction faction) {
        for (String chunkKey : zone.getChunks()) {
            String[] parts = chunkKey.split(",");
            int cx = Integer.parseInt(parts[0]);
            int cz = Integer.parseInt(parts[1]);
            PS ps = PS.valueOf(zone.getWorld(), null, null, null, null, null, null, cx, cz, null, null, null, null, null);
            Faction at = BoardColl.get().getFactionAt(ps);
            if (at != null && at.getId().equals(faction.getId())) {
                return true;
            }
        }
        return false;
    }

    private Set<FactionZone> getFactionZones(Faction faction) {
        Set<FactionZone> result = new HashSet<>();
        for (FactionZone zone : FactionZoneManager.getInstance().getAllZones().values()) {
            for (String chunkKey : zone.getChunks()) {
                String[] parts = chunkKey.split(",");
                int cx = Integer.parseInt(parts[0]);
                int cz = Integer.parseInt(parts[1]);
                PS ps = PS.valueOf(zone.getWorld(), null, null, null, null, null, null, cx, cz, null, null, null, null, null);
                Faction at = BoardColl.get().getFactionAt(ps);
                if (at != null && at.getId().equals(faction.getId())) {
                    result.add(zone);
                    break;
                }
            }
        }
        return result;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!FactionZoneInputManager.isEditing(player)) return;
        event.setCancelled(true);
        org.bukkit.Bukkit.getScheduler().runTask(org.debentialc.Main.instance, () -> {
            FactionZoneInputManager.processInput(player, event.getMessage());
            FactionAdminZoneMenu.reopenLastPage(player);
        });
    }
}
