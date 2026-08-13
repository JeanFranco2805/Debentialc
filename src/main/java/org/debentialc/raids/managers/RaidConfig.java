package org.debentialc.raids.managers;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.debentialc.utils.ItemStackResolver;

import java.io.File;
import java.io.IOException;

/**
 * Configuración del menú de raids para usuarios.
 * Archivo: plugins/Debentialc/raids_menu.yml
 */
public class RaidConfig {

    private static File configFile;
    private static FileConfiguration config;

    public static void load() {
        configFile = new File("plugins/Debentialc/raids_menu.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            config = YamlConfiguration.loadConfiguration(configFile);
            setDefaults();
            save();
        } else {
            config = YamlConfiguration.loadConfiguration(configFile);
        }
    }

    private static void setDefaults() {
        config.set("menu.title", "&6Raids Disponibles");
        config.set("menu.rows", 3);
        config.set("menu.item_id", "MAGMA_CREAM");
        config.set("menu.border_item_id", "STAINED_GLASS_PANE:7");
    }

    public static void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            System.err.println("[Raids] Error al guardar raids_menu.yml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static String getMenuTitle() {
        return config.getString("menu.title", "&6Raids Disponibles");
    }

    public static int getMenuRows() {
        return Math.max(1, Math.min(6, config.getInt("menu.rows", 3)));
    }

    public static ItemStack getMenuItem() {
        return parseItem(config.getString("menu.item_id", "MAGMA_CREAM"));
    }

    public static ItemStack getBorderItem() {
        return parseItem(config.getString("menu.border_item_id", "STAINED_GLASS_PANE:7"));
    }

    /**
     * Parsea un string de item. Formatos soportados:
     * - "MAGMA_CREAM"
     * - "WOOL:14"
     * - "351:14"
     * - "6207" (ID numérico de item)
     * - "6207:2"
     */
    public static ItemStack parseItem(String value) {
        if (value == null || value.trim().isEmpty()) {
            return new ItemStack(Material.MAGMA_CREAM);
        }

        String[] parts = value.trim().split(":");

        // Intentar ID numérico primero
        try {
            int typeId = Integer.parseInt(parts[0].trim());
            short data = 0;
            if (parts.length > 1) {
                data = Short.parseShort(parts[1].trim());
            }
            if (typeId > 0) {
                ItemStack resolved = ItemStackResolver.fromNumericId(typeId, data);
                if (resolved != null) {
                    return resolved;
                }
                return new ItemStack(typeId, 1, data);
            }
        } catch (NumberFormatException ignored) {
        }

        Material material = Material.getMaterial(parts[0].toUpperCase());

        if (material == null) {
            System.err.println("[Raids] Material/ID inválido en raids_menu.yml: " + value);
            return new ItemStack(Material.MAGMA_CREAM);
        }

        if (parts.length > 1) {
            try {
                short data = Short.parseShort(parts[1]);
                return new ItemStack(material, 1, data);
            } catch (NumberFormatException e) {
                System.err.println("[Raids] Data inválida en raids_menu.yml: " + value);
                return new ItemStack(material);
            }
        }

        return new ItemStack(material);
    }

    /**
     * Valida si un string es un ID de item válido (numérico o material de Bukkit).
     */
    public static boolean isValidItemId(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        String[] parts = value.trim().split(":");
        try {
            int typeId = Integer.parseInt(parts[0].trim());
            return typeId > 0;
        } catch (NumberFormatException ignored) {
        }
        return Material.getMaterial(parts[0].toUpperCase()) != null;
    }
}
