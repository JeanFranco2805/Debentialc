package org.debentialc.rebirths.storage;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.debentialc.Main;
import org.debentialc.rebirths.model.PlayerBlockStats;
import org.debentialc.rebirths.model.RebirthBlock;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RebirthBlockStorage {

    private static final RebirthBlockStorage INSTANCE = new RebirthBlockStorage();

    private File blocksFile;
    private FileConfiguration blocksConfig;

    private File playerDataFolder;

    private RebirthBlockStorage() {
        File dataFolder = Main.instance.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        this.blocksFile = new File(dataFolder, "rebirth_blocks.yml");
        this.playerDataFolder = new File(dataFolder, "playerdata");
        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
        }

        loadBlocksConfig();
    }

    public static RebirthBlockStorage getInstance() {
        return INSTANCE;
    }

    private void loadBlocksConfig() {
        if (!blocksFile.exists()) {
            try {
                blocksFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.blocksConfig = YamlConfiguration.loadConfiguration(blocksFile);
    }

    public void saveBlock(RebirthBlock block) {
        String path = "blocks." + block.getId();
        blocksConfig.set(path + ".name", block.getName());
        blocksConfig.set(path + ".saveLevel", block.isSaveLevel());
        blocksConfig.set(path + ".rebirthIds", block.getRebirthIds());
        saveBlocksConfig();
    }

    public void deleteBlock(int id) {
        blocksConfig.set("blocks." + id, null);
        saveBlocksConfig();
    }

    public Map<Integer, RebirthBlock> loadAllBlocks() {
        Map<Integer, RebirthBlock> blocks = new TreeMap<>();

        if (!blocksConfig.contains("blocks")) {
            return blocks;
        }

        for (String key : blocksConfig.getConfigurationSection("blocks").getKeys(false)) {
            try {
                int id = Integer.parseInt(key);
                String path = "blocks." + key;
                String name = blocksConfig.getString(path + ".name", "Bloque " + id);
                boolean saveLevel = blocksConfig.getBoolean(path + ".saveLevel", false);
                List<Integer> rebirthIds = blocksConfig.getIntegerList(path + ".rebirthIds");

                blocks.put(id, new RebirthBlock(id, name, saveLevel, rebirthIds));
            } catch (NumberFormatException e) {
                System.err.println("[Rebirths] ID de bloque inválido en config: " + key);
            }
        }

        return blocks;
    }

    private void saveBlocksConfig() {
        try {
            blocksConfig.save(blocksFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public PlayerBlockStats loadPlayerBlockStats(UUID uuid, int blockId) {
        File file = getPlayerFile(uuid);
        if (!file.exists()) {
            return new PlayerBlockStats();
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String path = "savedBlockStats.block_" + blockId;
        if (!config.contains(path)) {
            return new PlayerBlockStats();
        }

        Map<String, Integer> stats = new HashMap<>();
        if (config.contains(path + ".stats")) {
            for (String key : config.getConfigurationSection(path + ".stats").getKeys(false)) {
                stats.put(key, config.getInt(path + ".stats." + key));
            }
        }
        return new PlayerBlockStats(blockId, stats);
    }

    public void savePlayerBlockStats(UUID uuid, int blockId, PlayerBlockStats blockStats) {
        File file = getPlayerFile(uuid);
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String path = "savedBlockStats.block_" + blockId;
        config.set(path + ".stats", blockStats.getStats());
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clearPlayerBlockStats(UUID uuid, int blockId) {
        File file = getPlayerFile(uuid);
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("savedBlockStats.block_" + blockId, null);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clearAllPlayerBlockStats(UUID uuid) {
        File file = getPlayerFile(uuid);
        if (!file.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("savedBlockStats", null);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File getPlayerFile(UUID uuid) {
        return new File(playerDataFolder, uuid.toString() + ".yml");
    }
}
