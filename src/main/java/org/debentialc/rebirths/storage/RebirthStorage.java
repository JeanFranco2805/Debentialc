package org.debentialc.rebirths.storage;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.debentialc.Main;
import org.debentialc.rebirths.model.Rebirth;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class RebirthStorage {

    private static final RebirthStorage INSTANCE = new RebirthStorage();

    private File rebirthsFile;
    private FileConfiguration rebirthsConfig;

    private File playerDataFolder;

    private RebirthStorage() {
        File dataFolder = Main.instance.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        this.rebirthsFile = new File(dataFolder, "rebirths.yml");
        this.playerDataFolder = new File(dataFolder, "playerdata");
        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
        }

        loadRebirthsConfig();
    }

    public static RebirthStorage getInstance() {
        return INSTANCE;
    }

    private void loadRebirthsConfig() {
        if (!rebirthsFile.exists()) {
            try {
                rebirthsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.rebirthsConfig = YamlConfiguration.loadConfiguration(rebirthsFile);
    }

    public void saveRebirth(Rebirth rebirth) {
        String path = "rebirths." + rebirth.getId();
        rebirthsConfig.set(path + ".blockId", rebirth.getBlockId());
        rebirthsConfig.set(path + ".displayName", rebirth.getDisplayName());
        rebirthsConfig.set(path + ".requiredLevel", rebirth.getRequiredLevel());
        rebirthsConfig.set(path + ".tpBonusPercent", rebirth.getTpBonusPercent());
        rebirthsConfig.set(path + ".allowedRegions", rebirth.getAllowedRegions());

        List<Map<String, Object>> serializedItems = rebirth.getRewardItems().stream()
                .map(ItemStack::serialize)
                .collect(Collectors.toList());
        rebirthsConfig.set(path + ".rewardItems", serializedItems);

        rebirthsConfig.set(path + ".rewardCommands", rebirth.getRewardCommands());

        saveRebirthsConfig();
    }

    public void deleteRebirth(int id) {
        rebirthsConfig.set("rebirths." + id, null);
        saveRebirthsConfig();
    }

    public Map<Integer, Rebirth> loadAllRebirths() {
        Map<Integer, Rebirth> rebirths = new TreeMap<>();

        if (!rebirthsConfig.contains("rebirths")) {
            return rebirths;
        }

        for (String key : rebirthsConfig.getConfigurationSection("rebirths").getKeys(false)) {
            try {
                int id = Integer.parseInt(key);
                String path = "rebirths." + key;
                int blockId = rebirthsConfig.getInt(path + ".blockId", 0);
                String displayName = rebirthsConfig.getString(path + ".displayName", "Rebirth " + id);
                int requiredLevel = rebirthsConfig.getInt(path + ".requiredLevel", 0);
                double tpBonusPercent = rebirthsConfig.getDouble(path + ".tpBonusPercent", 0.0);
                List<String> allowedRegions = rebirthsConfig.getStringList(path + ".allowedRegions");

                List<ItemStack> rewardItems = new ArrayList<>();
                if (rebirthsConfig.contains(path + ".rewardItems")) {
                    List<Map<?, ?>> itemsList = rebirthsConfig.getMapList(path + ".rewardItems");
                    for (Map<?, ?> itemMap : itemsList) {
                        try {
                            Map<String, Object> typedMap = new HashMap<>();
                            for (Map.Entry<?, ?> entry : itemMap.entrySet()) {
                                typedMap.put(String.valueOf(entry.getKey()), entry.getValue());
                            }
                            rewardItems.add(ItemStack.deserialize(typedMap));
                        } catch (Exception e) {
                            System.err.println("[Rebirths] Error al cargar item de recompensa: " + e.getMessage());
                        }
                    }
                }

                List<String> rewardCommands = rebirthsConfig.getStringList(path + ".rewardCommands");

                rebirths.put(id, new Rebirth(id, blockId, displayName, requiredLevel, tpBonusPercent, allowedRegions, rewardItems, rewardCommands));
            } catch (NumberFormatException e) {
                System.err.println("[Rebirths] ID inválido en config: " + key);
            }
        }

        return rebirths;
    }

    private void saveRebirthsConfig() {
        try {
            rebirthsConfig.save(rebirthsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File getPlayerFile(UUID uuid) {
        return new File(playerDataFolder, uuid.toString() + ".yml");
    }

    public int loadPlayerRebirthLevel(UUID uuid) {
        File file = getPlayerFile(uuid);
        if (!file.exists()) {
            return 0;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        return config.getInt("rebirthLevel", 0);
    }

    public void savePlayerRebirthLevel(UUID uuid, int level) {
        File file = getPlayerFile(uuid);
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("rebirthLevel", level);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
