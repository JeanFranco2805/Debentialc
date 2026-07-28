package org.debentialc.claims.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.debentialc.claims.managers.TerrainCustomizeManager;
import org.debentialc.claims.managers.TerrainManager;
import org.debentialc.claims.models.Terrain;

public class PlayerDeathInTerrain implements Listener {

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Location location = event.getEntity().getLocation();
        Terrain terrain = TerrainManager.getInstance().getTerrainAt(location);
        if (terrain == null) {
            return;
        }
        if (!TerrainCustomizeManager.isRuleEnabled(terrain.getId(), "keepInventory")) {
            Player player = event.getEntity();
            int slot = 0;
            for (ItemStack itemStack : player.getInventory().getContents()) {
                player.getInventory().setItem(slot, null);
                if (itemStack != null && itemStack.getTypeId() != 0) {
                    location.getWorld().dropItem(location, itemStack);
                }
                slot++;
            }
        }
    }
}
