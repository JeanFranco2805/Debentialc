package org.debentialc.factions.zones;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.debentialc.Main;

import java.util.HashMap;
import java.util.Map;

public class FactionZoneManager {

    private static FactionZoneManager instance;
    private final FactionZoneStorage storage;
    private final Map<String, FactionZone> zones = new HashMap<>();

    private FactionZoneManager() {
        this.storage = FactionZoneStorage.getInstance();
        load();
    }

    public static synchronized FactionZoneManager getInstance() {
        if (instance == null) {
            instance = new FactionZoneManager();
        }
        return instance;
    }

    public void load() {
        zones.clear();
        zones.putAll(storage.loadZones());
    }

    public void reload() {
        storage.load();
        load();
    }

    public Map<String, FactionZone> getAllZones() {
        return new HashMap<>(zones);
    }

    public FactionZone getZone(String id) {
        return zones.get(id.toLowerCase());
    }

    public FactionZone getZoneAtChunk(String world, int chunkX, int chunkZ) {
        String key = chunkX + "," + chunkZ;
        for (FactionZone zone : zones.values()) {
            if (zone.getWorld().equalsIgnoreCase(world) && zone.getChunks().contains(key)) {
                return zone;
            }
        }
        return null;
    }

    public boolean createZone(String id, String displayName, String regionName, String worldName, int powerCost, String rarity) {
        id = id.toLowerCase();
        if (zones.containsKey(id)) {
            return false;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return false;
        }

        WorldGuardPlugin wg = getWorldGuard();
        if (wg == null) {
            return false;
        }

        RegionManager regionManager = wg.getRegionManager(world);
        if (regionManager == null) {
            return false;
        }

        ProtectedRegion region = regionManager.getRegion(regionName);
        if (region == null) {
            return false;
        }

        FactionZone zone = new FactionZone(id, displayName, worldName, powerCost, rarity);

        int minX = region.getMinimumPoint().getBlockX() >> 4;
        int maxX = region.getMaximumPoint().getBlockX() >> 4;
        int minZ = region.getMinimumPoint().getBlockZ() >> 4;
        int maxZ = region.getMaximumPoint().getBlockZ() >> 4;

        zone.setMinChunkX(minX);
        zone.setMaxChunkX(maxX);
        zone.setMinChunkZ(minZ);
        zone.setMaxChunkZ(maxZ);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                zone.getChunks().add(x + "," + z);
            }
        }

        zones.put(id, zone);
        storage.saveZone(zone);
        return true;
    }

    public boolean deleteZone(String id) {
        id = id.toLowerCase();
        if (!zones.containsKey(id)) {
            return false;
        }
        zones.remove(id);
        storage.deleteZone(id);
        return true;
    }

    public boolean updateZone(String id, String displayName, Integer powerCost, String rarity) {
        FactionZone zone = zones.get(id.toLowerCase());
        if (zone == null) return false;
        if (displayName != null) zone.setDisplayName(displayName);
        if (powerCost != null) zone.setPowerCost(powerCost);
        if (rarity != null) zone.setRarity(rarity);
        storage.saveZone(zone);
        return true;
    }

    private WorldGuardPlugin getWorldGuard() {
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") instanceof WorldGuardPlugin) {
            return (WorldGuardPlugin) Bukkit.getPluginManager().getPlugin("WorldGuard");
        }
        return null;
    }
}
