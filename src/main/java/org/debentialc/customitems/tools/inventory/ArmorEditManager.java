package org.debentialc.customitems.tools.inventory;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.debentialc.service.CC;
import org.debentialc.customitems.tools.ci.CustomArmor;
import org.debentialc.customitems.tools.storage.CustomArmorStorage;
import org.debentialc.customitems.commands.RegisterItem;
import org.debentialc.customitems.tools.durability.CustomDurabilityManager;
import org.debentialc.customitems.tools.nbt.NbtHandler;

import java.util.HashMap;
import java.util.UUID;

public class ArmorEditManager {

    public static class ArmorEditState {
        public String armorId;
        public String editType;
        public int lineNumber;

        public ArmorEditState(String armorId, String editType) {
            this.armorId = armorId;
            this.editType = editType;
        }

        public ArmorEditState(String armorId, String editType, int lineNumber) {
            this.armorId = armorId;
            this.editType = editType;
            this.lineNumber = lineNumber;
        }
    }

    private static final HashMap<UUID, ArmorEditState> playersEditing = new HashMap<UUID, ArmorEditState>();

    public static void startArmorEdit(Player player, String armorId, String editType) {
        playersEditing.put(player.getUniqueId(), new ArmorEditState(armorId, editType));

        player.closeInventory();
        player.sendMessage("");
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage(CC.translate("&b&l  Editar Armadura"));
        player.sendMessage("");
        player.sendMessage(CC.translate("&7  Tipo: &f" + editType.toUpperCase()));
        player.sendMessage(CC.translate("&7  Ingresa el nuevo valor"));
        player.sendMessage(CC.translate("&7  Escribe &c'cancelar' &7para abortar"));
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage("");
    }

    public static boolean isEditingArmor(Player player) {
        return playersEditing.containsKey(player.getUniqueId());
    }

    public static void processArmorEdit(Player player, String input) {
        ArmorEditState state = playersEditing.get(player.getUniqueId());
        if (state == null) return;

        if (!RegisterItem.items.containsKey(state.armorId)) {
            player.sendMessage("");
            player.sendMessage(CC.translate("&c✗ Armadura no encontrada"));
            player.sendMessage("");
            finishArmorEdit(player);
            return;
        }

        CustomArmor armor = RegisterItem.items.get(state.armorId);
        CustomArmorStorage storage = CustomArmorStorage.getInstance();

        if ("rename".equals(state.editType.toLowerCase())) {
            armor.setDisplayName(CC.translate(input));
            storage.saveArmor(armor);
            player.sendMessage("");
            player.sendMessage(CC.translate("&a✓ Nombre actualizado"));
            player.sendMessage("");
        } else if ("addline".equals(state.editType.toLowerCase())) {
            java.util.List<String> lore = armor.getLore();
            if (lore == null) {
                lore = new java.util.ArrayList<String>();
            }
            lore.add(CC.translate(input));
            armor.setLore(lore);
            storage.saveArmor(armor);
            player.sendMessage("");
            player.sendMessage(CC.translate("&a✓ Línea agregada"));
            player.sendMessage("");
        } else if ("setline".equals(state.editType.toLowerCase())) {
            java.util.List<String> lore = armor.getLore();
            if (lore == null || state.lineNumber > lore.size() || state.lineNumber < 1) {
                player.sendMessage("");
                player.sendMessage(CC.translate("&c✗ Número de línea inválido"));
                player.sendMessage("");
                finishArmorEdit(player);
                return;
            }
            lore.set(state.lineNumber - 1, CC.translate(input));
            armor.setLore(lore);
            storage.saveArmor(armor);
            player.sendMessage("");
            player.sendMessage(CC.translate("&a✓ Línea actualizada"));
            player.sendMessage("");
        }

        finishArmorEdit(player);
        org.bukkit.Bukkit.getScheduler().scheduleSyncDelayedTask(org.debentialc.Main.instance, new Runnable() {
            public void run() {
                CustomArmorMenus.openEditArmorMenu(state.armorId).open(player);
            }
        }, 1L);
    }

    public static void cancelArmorEdit(Player player) {
        player.sendMessage("");
        player.sendMessage(CC.translate("&c✗ Cancelado"));
        player.sendMessage("");
        finishArmorEdit(player);
    }

    private static void finishArmorEdit(Player player) {
        playersEditing.remove(player.getUniqueId());
    }

    public static void giveCustomArmor(Player player, String armorId) {
        if (!RegisterItem.items.containsKey(armorId)) {
            player.sendMessage("");
            player.sendMessage(CC.translate("&c✗ Armadura no encontrada"));
            player.sendMessage("");
            return;
        }

        CustomArmor customArmor = RegisterItem.items.get(armorId);

        ItemStack itemStack = new ItemStack(customArmor.getMaterial());
        org.bukkit.inventory.meta.ItemMeta meta = itemStack.getItemMeta();
        meta.setDisplayName(customArmor.getDisplayName());
        if (customArmor.getLore() != null) {
            meta.setLore(customArmor.getLore());
        }
        itemStack.setItemMeta(meta);

        if (customArmor.isUnbreakable()) {
            NbtHandler nbt = new NbtHandler(itemStack);
            nbt.setBoolean("Unbreakable", true);
            ItemStack withNbt = nbt.getItemStack();
            ItemStack restored = rebuildMeta(withNbt, customArmor.getDisplayName(), customArmor.getLore());
            itemStack = restored;
        }

        itemStack = applyIdentificationTags(itemStack, customArmor);

        if (customArmor.getMaxDurability() > 0) {
            CustomDurabilityManager.setCustomMaxDurability(itemStack, customArmor.getMaxDurability());
        }

        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItem(player.getLocation(), itemStack);
            player.sendMessage("");
            player.sendMessage(CC.translate("&a✓ Armadura entregada (soltada)"));
            player.sendMessage("");
        } else {
            player.getInventory().addItem(itemStack);
            player.sendMessage("");
            player.sendMessage(CC.translate("&a✓ Armadura entregada"));
            player.sendMessage("");
        }
    }

    private static ItemStack applyIdentificationTags(ItemStack itemStack, CustomArmor customArmor) {
        NbtHandler nbt = new NbtHandler(itemStack);
        nbt.setString("debentialc_id", customArmor.getId());
        nbt.setString("debentialc_type", "armor");
        if (customArmor.getRequiredRebirthBlock() != null && !customArmor.getRequiredRebirthBlock().isEmpty()) {
            nbt.setString("debentialc_rebirth_block", customArmor.getRequiredRebirthBlock());
        }
        if (customArmor.getRequiredRebirthLevel() > 0) {
            nbt.setInteger("debentialc_rebirth_level", customArmor.getRequiredRebirthLevel());
        }
        if (customArmor.getRequiredPermission() != null && !customArmor.getRequiredPermission().isEmpty()) {
            nbt.setString("debentialc_permission", customArmor.getRequiredPermission());
        }

        ItemStack tagged = nbt.getItemStack();
        ItemMeta meta = tagged.getItemMeta();
        if (meta != null) {
            if (customArmor.getDisplayName() != null) meta.setDisplayName(customArmor.getDisplayName());
            if (customArmor.getLore() != null) meta.setLore(customArmor.getLore());
            tagged.setItemMeta(meta);
        }
        return tagged;
    }

    private static ItemStack rebuildMeta(ItemStack item, String displayName, java.util.List<String> lore) {
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        if (displayName != null) meta.setDisplayName(displayName);
        if (lore != null) meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}