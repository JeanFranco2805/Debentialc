package org.debentialc.customitems.tools.storage;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.debentialc.Main;
import org.debentialc.customitems.commands.RegisterItem;
import org.debentialc.customitems.tools.ci.CustomArmor;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CustomArmorStorage {

    private static CustomArmorStorage instance;

    public static CustomArmorStorage getInstance() {
        if (instance == null) {
            instance = new CustomArmorStorage();
        }
        return instance;
    }

    public static void destroyInstance() {
        instance = null;
    }

    private File dataFolder;
    private File armorFile;
    private FileConfiguration armorConfig;

    public CustomArmorStorage() {
        this.dataFolder = new File(Main.instance.getDataFolder(), "armors");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.armorFile = new File(dataFolder, "custom_armors.yml");
        reload();
    }

    public void reload() {
        if (!armorFile.exists()) {
            try {
                armorFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.armorConfig = YamlConfiguration.loadConfiguration(armorFile);
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
        String path = "armors." + armor.getId();

        // Campos básicos
        armorConfig.set(path + ".id",            armor.getId());
        armorConfig.set(path + ".material",      armor.getMaterial());
        armorConfig.set(path + ".displayName",   armor.getDisplayName());
        armorConfig.set(path + ".isArmor",       armor.isArmor());
        armorConfig.set(path + ".maxDurability", armor.getMaxDurability());
        armorConfig.set(path + ".unbreakable",   armor.isUnbreakable());

        // Lore: puede ser null — guardamos lista vacía para evitar
        // que al leer con getStringList() devuelva null y revienten los menús
        List<String> lore = armor.getLore();
        armorConfig.set(path + ".lore", lore != null ? lore : new ArrayList<String>());

        // bonusStat (HashMap<String, Double>)
        if (armor.getValueByStat() != null && !armor.getValueByStat().isEmpty()) {
            for (Map.Entry<String, Double> entry : armor.getValueByStat().entrySet()) {
                armorConfig.set(path + ".bonusStat." + entry.getKey(), entry.getValue());
            }
        } else {
            // Limpiar la sección si está vacía para no dejar datos viejos
            armorConfig.set(path + ".bonusStat", null);
        }

        // operations (HashMap<String, String>)
        if (armor.getOperation() != null && !armor.getOperation().isEmpty()) {
            for (Map.Entry<String, String> entry : armor.getOperation().entrySet()) {
                armorConfig.set(path + ".operations." + entry.getKey(), entry.getValue());
            }
        } else {
            armorConfig.set(path + ".operations", null);
        }

        // effects (HashMap<String, Double>)
        if (armor.getEffects() != null && !armor.getEffects().isEmpty()) {
            for (Map.Entry<String, Double> entry : armor.getEffects().entrySet()) {
                armorConfig.set(path + ".effects." + entry.getKey(), entry.getValue());
            }
        } else {
            armorConfig.set(path + ".effects", null);
        }

        try {
            armorConfig.save(armorFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Mantener en memoria también
        RegisterItem.items.put(armor.getId(), armor);
    }

    public void deleteArmor(String id) {
        armorConfig.set("armors." + id, null);
        try {
            armorConfig.save(armorFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        RegisterItem.items.remove(id);
    }

    public CustomArmor loadArmor(String id) {
        String path = "armors." + id;
        if (!armorConfig.contains(path)) return null;

        CustomArmor armor = new CustomArmor();
        armor.setId(id);
        armor.setMaterial(armorConfig.getInt(path + ".material", 0));
        armor.setDisplayName(armorConfig.getString(path + ".displayName", ""));
        armor.setMaxDurability(armorConfig.getInt(path + ".maxDurability", -1));
        armor.setUnbreakable(armorConfig.getBoolean(path + ".unbreakable", false));

        // Lore — getStringList nunca devuelve null, devuelve lista vacía
        List<String> lore = armorConfig.getStringList(path + ".lore");
        armor.setLore(lore.isEmpty() ? null : lore);

        // bonusStat
        if (armorConfig.contains(path + ".bonusStat")) {
            HashMap<String, Double> bonusStat = new HashMap<String, Double>();
            for (String key : armorConfig.getConfigurationSection(path + ".bonusStat").getKeys(false)) {
                bonusStat.put(key, armorConfig.getDouble(path + ".bonusStat." + key));
            }
            armor.setValueByStat(bonusStat);
        }

        // operations
        if (armorConfig.contains(path + ".operations")) {
            HashMap<String, String> operations = new HashMap<String, String>();
            for (String key : armorConfig.getConfigurationSection(path + ".operations").getKeys(false)) {
                operations.put(key, armorConfig.getString(path + ".operations." + key));
            }
            armor.setOperation(operations);
        }

        // effects
        if (armorConfig.contains(path + ".effects")) {
            HashMap<String, Double> effects = new HashMap<String, Double>();
            for (String key : armorConfig.getConfigurationSection(path + ".effects").getKeys(false)) {
                effects.put(key, armorConfig.getDouble(path + ".effects." + key));
            }
            armor.setEffects(effects);
        }

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