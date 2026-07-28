package org.debentialc.customitems.tools.durability;

import net.minecraft.server.v1_7_R4.NBTTagCompound;
import org.bukkit.craftbukkit.v1_7_R4.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.debentialc.service.CC;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class CustomDurabilityManager {

    private static final short SYNTHETIC_MAX = 1000;

    private static Field craftHandleField;

    static {
        try {
            craftHandleField = CraftItemStack.class.getDeclaredField("handle");
            craftHandleField.setAccessible(true);
        } catch (Exception e) {
            craftHandleField = null;
        }
    }

    private static net.minecraft.server.v1_7_R4.ItemStack getCraftHandle(ItemStack item) {
        if (craftHandleField != null && item instanceof CraftItemStack) {
            try {
                return (net.minecraft.server.v1_7_R4.ItemStack) craftHandleField.get(item);
            } catch (Exception ignored) {}
        }
        return null;
    }

    public static void setUnbreakable(ItemStack item, boolean unbreakable) {
        if (item == null || item.getTypeId() == 0) return;

        net.minecraft.server.v1_7_R4.ItemStack handle = getCraftHandle(item);

        if (handle != null) {
            NBTTagCompound tag = handle.hasTag() ? handle.getTag() : new NBTTagCompound();
            tag.setBoolean("Unbreakable", unbreakable);
            handle.setTag(tag);
        } else {
            net.minecraft.server.v1_7_R4.ItemStack nms = CraftItemStack.asNMSCopy(item);
            NBTTagCompound tag = nms.hasTag() ? nms.getTag() : new NBTTagCompound();
            tag.setBoolean("Unbreakable", unbreakable);
            nms.setTag(tag);
            ItemStack rebuilt = CraftItemStack.asBukkitCopy(nms);
            item.setItemMeta(rebuilt.getItemMeta());
        }
    }

    public static boolean isUnbreakable(ItemStack item) {
        if (item == null || item.getTypeId() == 0) return false;

        net.minecraft.server.v1_7_R4.ItemStack handle = getCraftHandle(item);
        if (handle != null) {
            return handle.hasTag() && handle.getTag().getBoolean("Unbreakable");
        }

        net.minecraft.server.v1_7_R4.ItemStack nms = CraftItemStack.asNMSCopy(item);
        return nms.hasTag() && nms.getTag().getBoolean("Unbreakable");
    }

    public static void setCustomMaxDurability(ItemStack item, int maxDurability) {
        setCustomDurability(item, maxDurability, maxDurability);
    }

    public static void setCustomDurability(ItemStack item, int current, int max) {
        if (item == null || item.getTypeId() == 0) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        current = Math.max(0, Math.min(current, max));

        List<String> lore = meta.getLore();
        if (lore == null) lore = new ArrayList<String>();

        List<String> newLore = new ArrayList<String>();
        for (String line : lore) {
            String clean = line.replaceAll("\u00a7[0-9a-fk-or]", "");
            if (!clean.matches("\\d+/\\d+ \\(\\d+%\\)")) {
                newLore.add(line);
            }
        }

        double pct = (double) current / (double) max * 100.0;
        newLore.add(0, CC.translate("&a" + current + "/" + max + " (" + String.format("%.0f", pct) + "%)"));

        meta.setLore(newLore);
        item.setItemMeta(meta);

        syncVisualBar(item, current, max);
    }

    public static int getCustomDurability(ItemStack item) {
        if (item == null || item.getTypeId() == 0) return 0;
        if (!item.hasItemMeta()) return 0;
        List<String> lore = item.getItemMeta().getLore();
        if (lore == null) return 0;
        for (String line : lore) {
            String clean = line.replaceAll("\u00a7[0-9a-fk-or]", "");
            if (clean.matches("\\d+/\\d+ \\(\\d+%\\)")) {
                try {
                    return Integer.parseInt(clean.split("/")[0].trim());
                } catch (NumberFormatException ignored) {}
            }
        }
        return 0;
    }

    public static int getCustomMaxDurability(ItemStack item) {
        if (item == null || item.getTypeId() == 0) return 0;
        if (!item.hasItemMeta()) return 0;
        List<String> lore = item.getItemMeta().getLore();
        if (lore == null) return 0;
        for (String line : lore) {
            String clean = line.replaceAll("\u00a7[0-9a-fk-or]", "");
            if (clean.matches("\\d+/\\d+ \\(\\d+%\\)")) {
                try {
                    return Integer.parseInt(clean.split("/")[1].split(" ")[0].trim());
                } catch (NumberFormatException ignored) {}
            }
        }
        return 0;
    }

    public static boolean hasCustomDurability(ItemStack item) {
        if (item == null || item.getTypeId() == 0) return false;
        if (!item.hasItemMeta()) return false;
        List<String> lore = item.getItemMeta().getLore();
        if (lore == null) return false;
        for (String line : lore) {
            String clean = line.replaceAll("\u00a7[0-9a-fk-or]", "");
            if (clean.matches("\\d+/\\d+ \\(\\d+%\\)")) return true;
        }
        return false;
    }

    public static boolean damageItem(ItemStack item, int damage) {
        if (!hasCustomDurability(item)) return false;

        int current = getCustomDurability(item);
        int max = getCustomMaxDurability(item);
        current -= damage;

        if (current <= 0) {
            // Custom durability takes precedence over the Unbreakable NBT tag.
            // If the item has a custom durability bar and it reaches 0, it must break.
            setCustomDurability(item, 0, max);
            return true;
        }

        setCustomDurability(item, current, max);
        return false;
    }

    public static void repairItem(ItemStack item, int amount) {
        if (!hasCustomDurability(item)) return;
        int current = getCustomDurability(item);
        int max = getCustomMaxDurability(item);
        setCustomDurability(item, Math.min(current + amount, max), max);
    }

    public static void repairItemFull(ItemStack item) {
        if (!hasCustomDurability(item)) return;
        int max = getCustomMaxDurability(item);
        setCustomDurability(item, max, max);
    }

    public static boolean isModItem(ItemStack item) {
        if (item == null) return false;
        return item.getType().getMaxDurability() == 0;
    }

    private static void syncVisualBar(ItemStack item, int current, int max) {
        double pct = (double) current / (double) max;
        short vanillaMax = item.getType().getMaxDurability();
        if (vanillaMax > 0) {
            item.setDurability((short) (vanillaMax - (int) (vanillaMax * pct)));
        } else {
            item.setDurability((short) (SYNTHETIC_MAX - (int) (SYNTHETIC_MAX * pct)));
        }
    }

    public static void syncVisualDurabilityForModItem(ItemStack item, int current, int max) {
        syncVisualBar(item, current, max);
    }

    public static void removeDurabilityFromLore(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();
        if (lore == null) return;
        List<String> newLore = new ArrayList<String>();
        for (String line : lore) {
            String clean = line.replaceAll("\u00a7[0-9a-fk-or]", "");
            if (!clean.matches("\\d+/\\d+ \\(\\d+%\\)")) newLore.add(line);
        }
        meta.setLore(newLore);
        item.setItemMeta(meta);
    }

    public static String getDurabilityText(ItemStack item) {
        if (!hasCustomDurability(item)) return "";
        int current = getCustomDurability(item);
        int max = getCustomMaxDurability(item);
        double pct = (double) current / (double) max * 100.0;
        return CC.translate("&a" + current + "/" + max + " (" + String.format("%.0f", pct) + "%)");
    }

    public static void addDurabilityToLore(ItemStack item) {}

    public static void updateDurabilityLore(ItemStack item) {
        if (!hasCustomDurability(item)) return;
        int current = getCustomDurability(item);
        int max = getCustomMaxDurability(item);
        setCustomDurability(item, current, max);
    }
}