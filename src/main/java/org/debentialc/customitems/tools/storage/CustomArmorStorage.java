package org.debentialc.customitems.tools.storage;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.debentialc.Main;
import org.debentialc.customitems.commands.RegisterItem;
import org.debentialc.customitems.tools.ci.CustomArmor;
import org.debentialc.service.CC;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;

public class CustomArmorStorage {

    private static CustomArmorStorage instance;
    private static final Object LOCK = new Object();

    private File dataFolder;
    private File armorFile;
    private FileConfiguration armorConfig;

    public static CustomArmorStorage getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new CustomArmorStorage();
                }
            }
        }
        return instance;
    }

    public static void destroyInstance() {
        synchronized (LOCK) {
            instance = null;
        }
    }

    public CustomArmorStorage() {
        this.dataFolder = new File(Main.instance.getDataFolder(), "armors");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.armorFile = new File(dataFolder, "custom_armors.yml");
        reload();
    }

    public String getArmorFilePath() {
        return armorFile.getAbsolutePath();
    }

    public void reload() {
        if (!armorFile.exists()) {
            try {
                armorFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            this.armorConfig = YamlConfiguration.loadConfiguration(armorFile);
        } catch (Exception e) {
            Main.instance.getLogger().severe("[CustomArmorStorage] Error cargando custom_armors.yml: " + e.getMessage());
            backupCorruptFile();
            this.armorConfig = new YamlConfiguration();
        }
    }

    /**
     * Carga todas las armaduras del disco y las pone en RegisterItem.items.
     * Este es el método que DEBE llamarse en onEnable().
     */
    public void initialLoad() {
        reload();
        Map<String, CustomArmor> loaded = loadAllArmors();
        RegisterItem.items.putAll(loaded);
        Main.instance.getLogger().info("[CustomArmorStorage] Cargadas " + loaded.size() + " armaduras custom.");
    }

    public void saveArmor(CustomArmor armor) {
        synchronized (LOCK) {
            String path = "armors." + armor.getId();

            armorConfig.set(path + ".id", armor.getId());
            armorConfig.set(path + ".material", armor.getMaterial());
            armorConfig.set(path + ".displayName", armor.getDisplayName() != null ? armor.getDisplayName().replace('§', '&') : null);
            armorConfig.set(path + ".isArmor", armor.isArmor());
            armorConfig.set(path + ".maxDurability", armor.getMaxDurability());
            armorConfig.set(path + ".unbreakable", armor.isUnbreakable());
            armorConfig.set(path + ".requiredRebirthBlock", armor.getRequiredRebirthBlock());
            armorConfig.set(path + ".requiredRebirthLevel", armor.getRequiredRebirthLevel() > 0 ? armor.getRequiredRebirthLevel() : null);
            armorConfig.set(path + ".requiredPermission", armor.getRequiredPermission());

            List<String> lore = armor.getLore();
            if (lore != null) {
                List<String> safeLore = new ArrayList<String>();
                for (String line : lore) {
                    safeLore.add(line != null ? line.replace('§', '&') : "");
                }
                armorConfig.set(path + ".lore", safeLore);
            } else {
                armorConfig.set(path + ".lore", new ArrayList<String>());
            }

            if (armor.getValueByStat() != null && !armor.getValueByStat().isEmpty()) {
                armorConfig.set(path + ".bonusStat", null);
                for (Map.Entry<String, Double> entry : armor.getValueByStat().entrySet()) {
                    armorConfig.set(path + ".bonusStat." + entry.getKey(), entry.getValue());
                }
            } else {
                armorConfig.set(path + ".bonusStat", null);
            }

            if (armor.getOperation() != null && !armor.getOperation().isEmpty()) {
                armorConfig.set(path + ".operations", null);
                for (Map.Entry<String, String> entry : armor.getOperation().entrySet()) {
                    armorConfig.set(path + ".operations." + entry.getKey(), entry.getValue());
                }
            } else {
                armorConfig.set(path + ".operations", null);
            }

            if (armor.getEffects() != null && !armor.getEffects().isEmpty()) {
                armorConfig.set(path + ".effects", null);
                for (Map.Entry<String, Double> entry : armor.getEffects().entrySet()) {
                    armorConfig.set(path + ".effects." + entry.getKey(), entry.getValue());
                }
            } else {
                armorConfig.set(path + ".effects", null);
            }

            armorConfig.set(path + ".ownerOnly", armor.isOwnerOnly());
            armorConfig.set(path + ".expirationSeconds", armor.getExpirationSeconds() > 0 ? armor.getExpirationSeconds() : null);

            RegisterItem.items.put(armor.getId(), armor);
            saveConfig();
        }
    }

    public void deleteArmor(String id) {
        synchronized (LOCK) {
            armorConfig.set("armors." + id, null);
            saveConfig();
            RegisterItem.items.remove(id);
        }
    }

    private void saveConfig() {
        try {
            if (armorFile.exists()) {
                File backupFile = new File(dataFolder, "custom_armors.yml.backup");
                Files.copy(armorFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            File tempFile = new File(dataFolder, "custom_armors.yml.tmp");
            armorConfig.save(tempFile);
            Files.move(tempFile.toPath(), armorFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            Main.instance.getLogger().severe("[CustomArmorStorage] Error guardando custom_armors.yml: " + e.getMessage());
            e.printStackTrace();
            try {
                armorConfig.save(armorFile);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void backupCorruptFile() {
        if (armorFile.exists()) {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            File backup = new File(dataFolder, "custom_armors.yml.corrupt_" + timestamp);
            try {
                Files.copy(armorFile.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Main.instance.getLogger().warning("[CustomArmorStorage] Backup de archivo corrupto creado: " + backup.getName());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public CustomArmor loadArmor(String id) {
        String path = "armors." + id;
        if (!armorConfig.contains(path)) return null;

        CustomArmor armor = new CustomArmor();
        armor.setId(id);
        armor.setMaterial(armorConfig.getInt(path + ".material", 0));
        armor.setDisplayName(CC.translate(armorConfig.getString(path + ".displayName", "")));
        armor.setMaxDurability(armorConfig.getInt(path + ".maxDurability", -1));
        armor.setUnbreakable(armorConfig.getBoolean(path + ".unbreakable", false));
        if (armorConfig.contains(path + ".requiredRebirthBlock")) {
            armor.setRequiredRebirthBlock(armorConfig.getString(path + ".requiredRebirthBlock", null));
        } else {
            armor.setRequiredRebirthBlock(null);
        }
        if (armorConfig.contains(path + ".requiredRebirthLevel")) {
            armor.setRequiredRebirthLevel(armorConfig.getInt(path + ".requiredRebirthLevel", 0));
        }
        armor.setRequiredPermission(armorConfig.getString(path + ".requiredPermission", null));

        List<String> lore = new ArrayList<String>();
        for (String line : armorConfig.getStringList(path + ".lore")) {
            lore.add(CC.translate(line != null ? line.replace('§', '&') : ""));
        }
        armor.setLore(lore.isEmpty() ? null : lore);

        if (armorConfig.contains(path + ".bonusStat")) {
            HashMap<String, Double> bonusStat = new HashMap<String, Double>();
            for (String key : armorConfig.getConfigurationSection(path + ".bonusStat").getKeys(false)) {
                bonusStat.put(key, armorConfig.getDouble(path + ".bonusStat." + key));
            }
            armor.setValueByStat(bonusStat);
        }

        if (armorConfig.contains(path + ".operations")) {
            HashMap<String, String> operations = new HashMap<String, String>();
            for (String key : armorConfig.getConfigurationSection(path + ".operations").getKeys(false)) {
                operations.put(key, armorConfig.getString(path + ".operations." + key));
            }
            armor.setOperation(operations);
        }

        if (armorConfig.contains(path + ".effects")) {
            HashMap<String, Double> effects = new HashMap<String, Double>();
            for (String key : armorConfig.getConfigurationSection(path + ".effects").getKeys(false)) {
                effects.put(key, armorConfig.getDouble(path + ".effects." + key));
            }
            armor.setEffects(effects);
        }

        armor.setOwnerOnly(armorConfig.getBoolean(path + ".ownerOnly", false));
        armor.setExpirationSeconds(armorConfig.getInt(path + ".expirationSeconds", 0));

        return armor;
    }

    public Map<String, CustomArmor> loadAllArmors() {
        Map<String, CustomArmor> armors = new HashMap<String, CustomArmor>();
        if (!armorConfig.contains("armors")) return armors;
        for (String id : armorConfig.getConfigurationSection("armors").getKeys(false)) {
            CustomArmor armor = loadArmor(id);
            if (armor != null) armors.put(id, armor);
        }
        return armors;
    }
}
