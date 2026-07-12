package org.debentialc.rebirths.events;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.debentialc.Main;
import org.debentialc.rebirths.managers.RebirthManager;
import org.debentialc.rebirths.model.Rebirth;
import org.debentialc.service.CC;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RebirthRegionListener implements Listener {

    private final Map<UUID, Long> lastCheck = new HashMap<>();
    private static final long CHECK_COOLDOWN_MS = 1000;
    private boolean warnedMissingWorldGuard = false;

    private WorldGuardPlugin getWorldGuard() {
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") instanceof WorldGuardPlugin) {
            return (WorldGuardPlugin) Bukkit.getPluginManager().getPlugin("WorldGuard");
        }
        return null;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        WorldGuardPlugin worldGuard = getWorldGuard();
        if (worldGuard == null) {
            if (!warnedMissingWorldGuard) {
                warnedMissingWorldGuard = true;
                Main.instance.getLogger().warning("[Rebirths] WorldGuard no encontrado. La protección de zonas no funcionará.");
            }
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        if (player.hasPermission("debentialc.rebirth.bypass")) {
            return;
        }

        RegionManager regionManager = worldGuard.getRegionManager(to.getWorld());
        if (regionManager == null) return;

        int requiredFrom = getHighestRequiredRebirth(regionManager.getApplicableRegions(from));
        int requiredTo = getHighestRequiredRebirth(regionManager.getApplicableRegions(to));

        int playerRebirth = RebirthManager.getInstance().getPlayerRebirthLevel(player);

        // Log de diagnóstico (temporal para verificar detección)
        if (requiredTo > 0 && Main.instance.getLogger().isLoggable(java.util.logging.Level.FINE)) {
            Main.instance.getLogger().fine("[Rebirths] " + player.getName() + " | requiredFrom=" + requiredFrom
                    + " requiredTo=" + requiredTo + " playerRebirth=" + playerRebirth);
        }

        int required = Math.max(requiredFrom, requiredTo);
        if (required <= 0) return;

        if (playerRebirth >= required) {
            return;
        }

        // Solo expulsar si está entrando o si ya está dentro (cooldown para no spammear)
        boolean entering = requiredTo > requiredFrom;
        if (!entering) {
            long now = System.currentTimeMillis();
            Long last = lastCheck.get(player.getUniqueId());
            if (last != null && now - last < CHECK_COOLDOWN_MS) {
                event.setTo(from);
                return;
            }
            lastCheck.put(player.getUniqueId(), now);
        }

        String blockingRegion = getHighestBlockingRegion(regionManager.getApplicableRegions(to), required);

        event.setTo(from);
        removePlayerFromRegion(player, blockingRegion != null ? blockingRegion : "desconocida", required);
    }

    private int getHighestRequiredRebirth(ApplicableRegionSet regions) {
        if (regions == null) return 0;
        int required = 0;
        for (ProtectedRegion region : regions) {
            int regionRequired = getRequiredRebirthForRegion(region.getId());
            if (regionRequired > required) {
                required = regionRequired;
            }
        }
        return required;
    }

    private String getHighestBlockingRegion(ApplicableRegionSet regions, int requiredRebirth) {
        if (regions == null) return null;
        for (ProtectedRegion region : regions) {
            int regionRequired = getRequiredRebirthForRegion(region.getId());
            if (regionRequired == requiredRebirth) {
                return region.getId();
            }
        }
        return null;
    }

    private int getRequiredRebirthForRegion(String regionId) {
        int required = 0;
        for (Rebirth rebirth : RebirthManager.getInstance().getAllRebirths()) {
            if (rebirth.getAllowedRegions().contains(regionId)) {
                if (rebirth.getId() > required) {
                    required = rebirth.getId();
                }
            }
        }
        return required;
    }

    private void removePlayerFromRegion(Player player, String regionName, int requiredRebirth) {
        Bukkit.dispatchCommand(player, "warp spawn");
        player.sendMessage("");
        player.sendMessage(CC.translate("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(CC.translate("&c✗ Zona restringida"));
        player.sendMessage(CC.translate("&7La región &f" + regionName + " &7requiere Rebirth &e" + requiredRebirth));
        player.sendMessage(CC.translate("&7Has sido enviado al spawn."));
        player.sendMessage(CC.translate("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage("");

        Main.instance.getLogger().info("[Rebirths] " + player.getName() + " fue expulsado de la región " + regionName + " (requiere rebirth " + requiredRebirth + ")");
    }
}
