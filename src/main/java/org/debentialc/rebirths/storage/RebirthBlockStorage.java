package org.debentialc.rebirths.storage;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.debentialc.Main;
import org.debentialc.rebirths.model.RebirthBlock;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RebirthBlockStorage {

    private static final RebirthBlockStorage INSTANCE = new RebirthBlockStorage();

    private File blocksFile;
    private FileConfiguration blocksConfig;

    private RebirthBlockStorage() {
        File dataFolder = Main.instance.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        this.blocksFile = new File(dataFolder, "rebirth_blocks.yml");

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
        blocksConfig.set(path + ".vip", block.isVip());
        blocksConfig.set(path + ".requiredPermission", block.getRequiredPermission());
        blocksConfig.set(path + ".rebirthIds", block.getRebirthIds());
        saveBlocksConfig();
    }

    public void deleteBlock(String id) {
        blocksConfig.set("blocks." + id, null);
        saveBlocksConfig();
    }

    public Map<String, RebirthBlock> loadAllBlocks() {
        Map<String, RebirthBlock> blocks = new LinkedHashMap<>();

        if (!blocksConfig.contains("blocks")) {
            return blocks;
        }

        for (String key : blocksConfig.getConfigurationSection("blocks").getKeys(false)) {
            String id = migrateBlockId(key);
            String path = "blocks." + key;
            String name = blocksConfig.getString(path + ".name", "Bloque " + id);
            boolean vip = blocksConfig.getBoolean(path + ".vip", false);
            String requiredPermission = blocksConfig.getString(path + ".requiredPermission", null);
            List<Integer> rebirthIds = blocksConfig.getIntegerList(path + ".rebirthIds");

            RebirthBlock block = new RebirthBlock(id, name, rebirthIds);
            block.setVip(vip);
            block.setRequiredPermission(requiredPermission);
            blocks.put(id, block);
        }

        return blocks;
    }

    /**
     * Migra IDs antiguos numéricos al formato alfanumérico A{id}.
     * Si la clave ya es alfanumérica se devuelve tal cual.
     */
    private String migrateBlockId(String key) {
        try {
            int numeric = Integer.parseInt(key);
            return "A" + numeric;
        } catch (NumberFormatException e) {
            return key;
        }
    }

    private void saveBlocksConfig() {
        try {
            blocksConfig.save(blocksFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
