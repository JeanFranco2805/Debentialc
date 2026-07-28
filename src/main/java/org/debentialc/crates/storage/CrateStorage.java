package org.debentialc.crates.storage;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.debentialc.Main;
import org.debentialc.crates.models.Crate;
import org.debentialc.crates.models.CrateItem;
import org.debentialc.crates.models.CrateRarity;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CrateStorage {

    private static CrateStorage instance;
    private File crateFile;
    private FileConfiguration crateConfig;

    private CrateStorage() {
        load();
    }

    public static synchronized CrateStorage getInstance() {
        if (instance == null) {
            instance = new CrateStorage();
        }
        return instance;
    }

    public static void reload() {
        instance = null;
    }

    public void load() {
        File dataFolder = new File(Main.instance.getDataFolder(), "crates");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        crateFile = new File(dataFolder, "crates.yml");
        if (!crateFile.exists()) {
            try {
                crateFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        crateConfig = YamlConfiguration.loadConfiguration(crateFile);
    }

    public void save() {
        try {
            crateConfig.save(crateFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveRarities(Map<String, CrateRarity> rarities) {
        for (CrateRarity rarity : rarities.values()) {
            String path = "rarities." + rarity.getId();
            crateConfig.set(path + ".displayName", rarity.getDisplayName());
            crateConfig.set(path + ".color", rarity.getColor());
            crateConfig.set(path + ".weight", rarity.getWeight());
            crateConfig.set(path + ".announce", rarity.isAnnounce());
        }
        save();
    }

    public Map<String, CrateRarity> loadRarities() {
        Map<String, CrateRarity> rarities = new HashMap<>();
        if (!crateConfig.contains("rarities")) {
            return rarities;
        }
        for (String id : crateConfig.getConfigurationSection("rarities").getKeys(false)) {
            String path = "rarities." + id;
            String displayName = crateConfig.getString(path + ".displayName", id);
            String color = crateConfig.getString(path + ".color", "&7");
            double weight = crateConfig.getDouble(path + ".weight", 1.0);
            boolean announce = crateConfig.getBoolean(path + ".announce", false);
            rarities.put(id.toUpperCase(), new CrateRarity(id.toUpperCase(), displayName, color, weight, announce));
        }
        return rarities;
    }

    public void saveCrate(Crate crate) {
        String path = "crates." + crate.getId();
        crateConfig.set(path + ".displayName", crate.getDisplayName());
        if (crate.hasLocation()) {
            crateConfig.set(path + ".location.world", crate.getWorld());
            crateConfig.set(path + ".location.x", crate.getX());
            crateConfig.set(path + ".location.y", crate.getY());
            crateConfig.set(path + ".location.z", crate.getZ());
        }
        for (CrateItem item : crate.getItems().values()) {
            crateConfig.set(path + ".items." + item.getId() + ".rarity", item.getRarityId());
            crateConfig.set(path + ".items." + item.getId() + ".item", item.getItemBase64());
        }
        save();
    }

    public void deleteCrate(String id) {
        crateConfig.set("crates." + id, null);
        save();
    }

    public void deleteCrateItem(String crateId, String itemId) {
        crateConfig.set("crates." + crateId + ".items." + itemId, null);
        save();
    }

    public void deleteRarity(String id) {
        crateConfig.set("rarities." + id, null);
        save();
    }

    public Map<String, Crate> loadCrate(Map<String, CrateRarity> rarities) {
        Map<String, Crate> crates = new HashMap<>();
        if (!crateConfig.contains("crates")) {
            return crates;
        }
        for (String id : crateConfig.getConfigurationSection("crates").getKeys(false)) {
            String path = "crates." + id;
            String displayName = crateConfig.getString(path + ".displayName", id);
            Crate crate = new Crate(id, displayName);
            if (crateConfig.contains(path + ".location")) {
                crate.setWorld(crateConfig.getString(path + ".location.world", ""));
                crate.setX(crateConfig.getInt(path + ".location.x", 0));
                crate.setY(crateConfig.getInt(path + ".location.y", 0));
                crate.setZ(crateConfig.getInt(path + ".location.z", 0));
            }
            if (crateConfig.contains(path + ".items")) {
                for (String itemId : crateConfig.getConfigurationSection(path + ".items").getKeys(false)) {
                    String itemPath = path + ".items." + itemId;
                    String rarityId = crateConfig.getString(itemPath + ".rarity", "COMMON");
                    String itemBase64 = crateConfig.getString(itemPath + ".item");
                    crate.getItems().put(itemId.toLowerCase(), new CrateItem(itemId.toLowerCase(), rarityId.toUpperCase(), itemBase64));
                }
            }
            crates.put(id.toLowerCase(), crate);
        }
        return crates;
    }

    public boolean crateExists(String id) {
        return crateConfig.contains("crates." + id);
    }

    public boolean rarityExists(String id) {
        return crateConfig.contains("rarities." + id);
    }

    public Set<String> getCrateIds() {
        if (!crateConfig.contains("crates")) {
            return new HashSet<>();
        }
        return crateConfig.getConfigurationSection("crates").getKeys(false);
    }

    public Set<String> getRarityIds() {
        if (!crateConfig.contains("rarities")) {
            return new HashSet<>();
        }
        return crateConfig.getConfigurationSection("rarities").getKeys(false);
    }
}
