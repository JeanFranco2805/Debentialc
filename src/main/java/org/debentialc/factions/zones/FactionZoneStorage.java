package org.debentialc.factions.zones;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.debentialc.Main;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class FactionZoneStorage {

    private static FactionZoneStorage instance;
    private File file;
    private FileConfiguration config;

    private FactionZoneStorage() {
        load();
    }

    public static synchronized FactionZoneStorage getInstance() {
        if (instance == null) {
            instance = new FactionZoneStorage();
        }
        return instance;
    }

    public void load() {
        File dataFolder = new File(Main.instance.getDataFolder(), "factions");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        file = new File(dataFolder, "zones.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveZone(FactionZone zone) {
        String path = "zones." + zone.getId();
        config.set(path + ".displayName", zone.getDisplayName());
        config.set(path + ".world", zone.getWorld());
        config.set(path + ".powerCost", zone.getPowerCost());
        config.set(path + ".rarity", zone.getRarity());
        config.set(path + ".minChunkX", zone.getMinChunkX());
        config.set(path + ".maxChunkX", zone.getMaxChunkX());
        config.set(path + ".minChunkZ", zone.getMinChunkZ());
        config.set(path + ".maxChunkZ", zone.getMaxChunkZ());
        config.set(path + ".chunks", new ArrayList<>(zone.getChunks()));
        save();
    }

    public void deleteZone(String id) {
        config.set("zones." + id, null);
        save();
    }

    public Map<String, FactionZone> loadZones() {
        Map<String, FactionZone> zones = new HashMap<>();
        if (!config.contains("zones")) return zones;
        for (String id : config.getConfigurationSection("zones").getKeys(false)) {
            String path = "zones." + id;
            FactionZone zone = new FactionZone();
            zone.setId(id);
            zone.setDisplayName(config.getString(path + ".displayName", id));
            zone.setWorld(config.getString(path + ".world", ""));
            zone.setPowerCost(config.getInt(path + ".powerCost", 0));
            zone.setRarity(config.getString(path + ".rarity", "COMMON"));
            zone.setMinChunkX(config.getInt(path + ".minChunkX", 0));
            zone.setMaxChunkX(config.getInt(path + ".maxChunkX", 0));
            zone.setMinChunkZ(config.getInt(path + ".minChunkZ", 0));
            zone.setMaxChunkZ(config.getInt(path + ".maxChunkZ", 0));
            List<String> chunks = config.getStringList(path + ".chunks");
            zone.setChunks(new HashSet<>(chunks));
            zones.put(id.toLowerCase(), zone);
        }
        return zones;
    }
}
