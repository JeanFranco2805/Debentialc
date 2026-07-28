package org.debentialc.rebirths.storage;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.debentialc.Main;
import org.debentialc.customitems.tools.nbt.NbtHandler;
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
        rebirthsConfig.set(path + ".statBonusMultiplier", rebirth.getStatBonusMultiplier());
        rebirthsConfig.set(path + ".statBonusOperation", rebirth.getStatBonusOperation());
        rebirthsConfig.set(path + ".allowedRegions", rebirth.getAllowedRegions());

        List<String> serializedItems = rebirth.getRewardItems().stream()
                .map(NbtHandler::serializeItemStack)
                .filter(s -> s != null)
                .collect(Collectors.toList());
        rebirthsConfig.set(path + ".rewardItemsNbt", serializedItems);
        rebirthsConfig.set(path + ".rewardItems", null);

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
                String blockId = migrateBlockId(rebirthsConfig.getString(path + ".blockId", null));
                String displayName = rebirthsConfig.getString(path + ".displayName", "Rebirth " + id);
                int requiredLevel = rebirthsConfig.getInt(path + ".requiredLevel", 0);
                double tpBonusPercent = rebirthsConfig.getDouble(path + ".tpBonusPercent", 0.0);
                List<String> allowedRegions = rebirthsConfig.getStringList(path + ".allowedRegions");

                List<ItemStack> rewardItems = new ArrayList<>();
                if (rebirthsConfig.contains(path + ".rewardItemsNbt")) {
                    for (String nbt : rebirthsConfig.getStringList(path + ".rewardItemsNbt")) {
                        ItemStack item = NbtHandler.deserializeItemStack(nbt);
                        if (item != null) {
                            rewardItems.add(item);
                        }
                    }
                } else if (rebirthsConfig.contains(path + ".rewardItems")) {
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
                double statBonusMultiplier = rebirthsConfig.getDouble(path + ".statBonusMultiplier", 0.0);
                String statBonusOperation = rebirthsConfig.getString(path + ".statBonusOperation", "*");

                Rebirth rebirth = new Rebirth(id, blockId, displayName, requiredLevel, tpBonusPercent, allowedRegions, rewardItems, rewardCommands);
                rebirth.setStatBonusMultiplier(statBonusMultiplier);
                rebirth.setStatBonusOperation(statBonusOperation);
                rebirths.put(id, rebirth);
            } catch (NumberFormatException e) {
                System.err.println("[Rebirths] ID inválido en config: " + key);
            }
        }

        return rebirths;
    }

    /**
     * Migra IDs de bloque antiguos numéricos al formato alfanumérico A{id}.
     */
    private String migrateBlockId(String blockId) {
        if (blockId == null || blockId.isEmpty()) return null;
        try {
            int numeric = Integer.parseInt(blockId);
            return "A" + numeric;
        } catch (NumberFormatException e) {
            return blockId;
        }
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

    @SuppressWarnings("unchecked")
    public Set<Integer> loadPlayerUnlockedRebirths(UUID uuid) {
        File file = getPlayerFile(uuid);
        Set<Integer> unlocked = new HashSet<>();
        if (!file.exists()) {
            return unlocked;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        if (config.contains("unlockedRebirths")) {
            List<Integer> list = config.getIntegerList("unlockedRebirths");
            if (list != null) {
                unlocked.addAll(list);
            }
        } else if (config.contains("rebirthLevel")) {
            // Migration from old sequential max-level storage
            int level = config.getInt("rebirthLevel", 0);
            for (int i = 1; i <= level; i++) {
                unlocked.add(i);
            }
        }

        return unlocked;
    }

    public void savePlayerUnlockedRebirths(UUID uuid, Set<Integer> unlocked) {
        File file = getPlayerFile(uuid);
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("unlockedRebirths", new ArrayList<>(unlocked));
        config.set("rebirthLevel", null);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void resetPlayerUnlockedRebirths(UUID uuid) {
        File file = getPlayerFile(uuid);
        if (!file.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("unlockedRebirths", null);
        config.set("rebirthLevel", 0);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
