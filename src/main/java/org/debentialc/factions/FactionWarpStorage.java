package org.debentialc.factions;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.debentialc.Main;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class FactionWarpStorage {

    private static final FactionWarpStorage INSTANCE = new FactionWarpStorage();

    private File dataFolder;
    private File warpsFile;
    private FileConfiguration warpsConfig;

    private FactionWarpStorage() {
        this.dataFolder = new File(Main.instance.getDataFolder(), "factions");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.warpsFile = new File(dataFolder, "faction_warps.yml");
        load();
    }

    public static FactionWarpStorage getInstance() {
        return INSTANCE;
    }

    public void load() {
        if (!warpsFile.exists()) {
            try {
                warpsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.warpsConfig = YamlConfiguration.loadConfiguration(warpsFile);
    }

    public void save() {
        try {
            warpsConfig.save(warpsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, FactionWarp> getWarps(String factionId) {
        Map<String, FactionWarp> warps = new LinkedHashMap<>();
        String path = "warps." + factionId;
        if (!warpsConfig.contains(path)) return warps;

        for (String warpName : warpsConfig.getConfigurationSection(path).getKeys(false)) {
            String warpPath = path + "." + warpName;
            FactionWarp warp = new FactionWarp();
            warp.setName(warpName);
            warp.setWorld(warpsConfig.getString(warpPath + ".world"));
            warp.setX(warpsConfig.getDouble(warpPath + ".x"));
            warp.setY(warpsConfig.getDouble(warpPath + ".y"));
            warp.setZ(warpsConfig.getDouble(warpPath + ".z"));
            warp.setYaw((float) warpsConfig.getDouble(warpPath + ".yaw"));
            warp.setPitch((float) warpsConfig.getDouble(warpPath + ".pitch"));
            warps.put(warpName.toLowerCase(), warp);
        }
        return warps;
    }

    public FactionWarp getWarp(String factionId, String warpName) {
        return getWarps(factionId).get(warpName.toLowerCase());
    }

    public void setWarp(String factionId, FactionWarp warp) {
        String path = "warps." + factionId + "." + warp.getName();
        warpsConfig.set(path + ".world", warp.getWorld());
        warpsConfig.set(path + ".x", warp.getX());
        warpsConfig.set(path + ".y", warp.getY());
        warpsConfig.set(path + ".z", warp.getZ());
        warpsConfig.set(path + ".yaw", warp.getYaw());
        warpsConfig.set(path + ".pitch", warp.getPitch());
        save();
    }

    public void removeWarp(String factionId, String warpName) {
        String path = "warps." + factionId + "." + warpName;
        warpsConfig.set(path, null);
        save();
    }

    public void removeAllWarps(String factionId) {
        String path = "warps." + factionId;
        warpsConfig.set(path, null);
        save();
    }
}
