package org.debentialc.customitems.tools.storage;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.debentialc.Main;
import org.debentialc.customitems.tools.ci.CustomItem;
import org.debentialc.service.CC;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;

public class CustomItemStorage {
    private static CustomItemStorage instance;
    private static final Object LOCK = new Object();

    private File dataFolder;
    private File itemsFile;
    private FileConfiguration itemsConfig;

    private CustomItemStorage() {
        this.dataFolder = new File(Main.instance.getDataFolder(), "items");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.itemsFile = new File(dataFolder, "custom_items.yml");
        loadItems();
    }

    public static CustomItemStorage getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new CustomItemStorage();
                }
            }
        }
        return instance;
    }

    public static void reload() {
        synchronized (LOCK) {
            instance = null;
        }
    }

    public void loadItems() {
        if (!itemsFile.exists()) {
            try {
                itemsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            this.itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
        } catch (Exception e) {
            Main.instance.getLogger().severe("[CustomItemStorage] Error cargando custom_items.yml: " + e.getMessage());
            backupCorruptFile();
            this.itemsConfig = new YamlConfiguration();
        }
    }

    public void saveItem(CustomItem item) {
        synchronized (LOCK) {
            String path = "items." + item.getId();
            itemsConfig.set(path + ".id", item.getId());
            itemsConfig.set(path + ".material", item.getMaterial());
            itemsConfig.set(path + ".durabilityData", item.getDurabilityData());
            itemsConfig.set(path + ".displayName", item.getDisplayName() != null ? item.getDisplayName().replace('§', '&') : null);
            if (item.getLore() != null) {
                List<String> safeLore = new ArrayList<>();
                for (String line : item.getLore()) {
                    safeLore.add(line != null ? line.replace('§', '&') : "");
                }
                itemsConfig.set(path + ".lore", safeLore);
            } else {
                itemsConfig.set(path + ".lore", null);
            }
            itemsConfig.set(path + ".isActive", item.isActive());
            itemsConfig.set(path + ".bonusStat", new HashMap<>(item.getValueByStat()));
            itemsConfig.set(path + ".operations", new HashMap<>(item.getOperation()));
            itemsConfig.set(path + ".effects", new HashMap<>(item.getEffects()));
            itemsConfig.set(path + ".maxDurability", item.getMaxDurability());
            itemsConfig.set(path + ".unbreakable", item.isUnbreakable());
            itemsConfig.set(path + ".consumable", item.isConsumable());
            itemsConfig.set(path + ".delaySeconds", item.getDelaySeconds());
            itemsConfig.set(path + ".expirationSeconds", item.getExpirationSeconds());
            itemsConfig.set(path + ".commands", item.getCommands());
            itemsConfig.set(path + ".tpValue", item.getTpValue());
            itemsConfig.set(path + ".tpConsumeStack", item.isTpConsumeStack());
            itemsConfig.set(path + ".attackDamage", item.getAttackDamage());
            itemsConfig.set(path + ".nbtData", item.getNbtData());
            itemsConfig.set(path + ".category", item.getCategory());
            itemsConfig.set(path + ".requiredRebirthBlock", item.getRequiredRebirthBlock());
            itemsConfig.set(path + ".requiredRebirthLevel", item.getRequiredRebirthLevel() > 0 ? item.getRequiredRebirthLevel() : null);
            itemsConfig.set(path + ".requiredPermission", item.getRequiredPermission());
            itemsConfig.set(path + ".ownerOnly", item.isOwnerOnly());

            saveConfig();
        }
    }

    public void deleteItem(String id) {
        synchronized (LOCK) {
            itemsConfig.set("items." + id, null);
            saveConfig();
        }
    }

    private void saveConfig() {
        try {
            if (itemsFile.exists()) {
                File backupFile = new File(dataFolder, "custom_items.yml.backup");
                Files.copy(itemsFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            File tempFile = new File(dataFolder, "custom_items.yml.tmp");
            itemsConfig.save(tempFile);
            Files.move(tempFile.toPath(), itemsFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            Main.instance.getLogger().severe("[CustomItemStorage] Error guardando custom_items.yml: " + e.getMessage());
            e.printStackTrace();
            try {
                itemsConfig.save(itemsFile);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void backupCorruptFile() {
        if (itemsFile.exists()) {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            File backup = new File(dataFolder, "custom_items.yml.corrupt_" + timestamp);
            try {
                Files.copy(itemsFile.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Main.instance.getLogger().warning("[CustomItemStorage] Backup de archivo corrupto creado: " + backup.getName());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public CustomItem loadItem(String id) {
        String path = "items." + id;
        if (!itemsConfig.contains(path)) {
            return null;
        }

        CustomItem item = new CustomItem();
        item.setId(id);
        item.setMaterial(itemsConfig.getInt(path + ".material"));
        item.setDurabilityData((short) itemsConfig.getInt(path + ".durabilityData", 0));
        item.setDisplayName(CC.translate(itemsConfig.getString(path + ".displayName", "")));

        List<String> lore = new ArrayList<>();
        if (itemsConfig.contains(path + ".lore")) {
            if (itemsConfig.isList(path + ".lore")) {
                for (String line : itemsConfig.getStringList(path + ".lore")) {
                    lore.add(CC.translate(line != null ? line.replace('§', '&') : ""));
                }
            } else if (itemsConfig.isString(path + ".lore")) {
                String loreString = itemsConfig.getString(path + ".lore");
                if (loreString != null && !loreString.isEmpty()) {
                    for (String line : loreString.split("\\n")) {
                        lore.add(CC.translate(line.replace('§', '&')));
                    }
                }
            }
        }
        item.setLore(lore);
        item.setActive(itemsConfig.getBoolean(path + ".isActive", true));
        item.setMaxDurability(itemsConfig.getInt(path + ".maxDurability", -1));
        item.setUnbreakable(itemsConfig.getBoolean(path + ".unbreakable", false));
        item.setConsumable(itemsConfig.getBoolean(path + ".consumable", false));
        int delaySeconds = 0;
        if (itemsConfig.contains(path + ".delaySeconds")) {
            delaySeconds = itemsConfig.getInt(path + ".delaySeconds", 0);
        } else if (itemsConfig.contains(path + ".cooldownSeconds")) {
            delaySeconds = itemsConfig.getInt(path + ".cooldownSeconds", 0);
        }
        item.setDelaySeconds(delaySeconds);
        item.setExpirationSeconds(itemsConfig.getInt(path + ".expirationSeconds", 0));
        item.setCommands(itemsConfig.getStringList(path + ".commands"));
        item.setTpValue(itemsConfig.getInt(path + ".tpValue", 0));
        item.setTpConsumeStack(itemsConfig.getBoolean(path + ".tpConsumeStack", false));
        item.setAttackDamage(itemsConfig.getInt(path + ".attackDamage", -1));
        item.setNbtData(itemsConfig.getString(path + ".nbtData", null));
        item.setCategory(itemsConfig.getString(path + ".category", null));
        if (itemsConfig.contains(path + ".requiredRebirthBlock")) {
            item.setRequiredRebirthBlock(itemsConfig.getString(path + ".requiredRebirthBlock", null));
        } else {
            item.setRequiredRebirthBlock(null);
        }
        if (itemsConfig.contains(path + ".requiredRebirthLevel")) {
            item.setRequiredRebirthLevel(itemsConfig.getInt(path + ".requiredRebirthLevel", 0));
        }
        item.setRequiredPermission(itemsConfig.getString(path + ".requiredPermission", null));
        item.setOwnerOnly(itemsConfig.getBoolean(path + ".ownerOnly", false));

        if (itemsConfig.contains(path + ".bonusStat")) {
            HashMap<String, Double> bonusStat = new HashMap<>();
            for (String key : itemsConfig.getConfigurationSection(path + ".bonusStat").getKeys(false)) {
                bonusStat.put(key, itemsConfig.getDouble(path + ".bonusStat." + key));
            }
            item.setValueByStat(bonusStat);
        }

        if (itemsConfig.contains(path + ".operations")) {
            HashMap<String, String> operations = new HashMap<>();
            for (String key : itemsConfig.getConfigurationSection(path + ".operations").getKeys(false)) {
                operations.put(key, itemsConfig.getString(path + ".operations." + key));
            }
            item.setOperation(operations);
        }

        if (itemsConfig.contains(path + ".effects")) {
            HashMap<String, Double> effects = new HashMap<>();
            for (String key : itemsConfig.getConfigurationSection(path + ".effects").getKeys(false)) {
                effects.put(key, itemsConfig.getDouble(path + ".effects." + key));
            }
            item.setEffects(effects);
        }

        return item;
    }

    public Map<String, CustomItem> loadAllItems() {
        Map<String, CustomItem> items = new HashMap<>();

        if (!itemsConfig.contains("items")) {
            return items;
        }

        for (String id : itemsConfig.getConfigurationSection("items").getKeys(false)) {
            CustomItem item = loadItem(id);
            if (item != null) {
                items.put(id, item);
            }
        }

        return items;
    }

    public boolean itemExists(String id) {
        return itemsConfig.contains("items." + id);
    }

    public Set<String> getAllItemIds() {
        if (!itemsConfig.contains("items")) {
            return new HashSet<>();
        }
        return itemsConfig.getConfigurationSection("items").getKeys(false);
    }
}
