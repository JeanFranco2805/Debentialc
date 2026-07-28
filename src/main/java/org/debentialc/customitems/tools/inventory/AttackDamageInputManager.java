package org.debentialc.customitems.tools.inventory;

import org.bukkit.entity.Player;
import org.debentialc.service.CC;
import org.debentialc.customitems.commands.CustomItemCommand;
import org.debentialc.customitems.tools.ci.CustomItem;
import org.debentialc.customitems.tools.nbt.NbtHandler;
import org.debentialc.customitems.tools.storage.CustomItemStorage;

import java.util.HashMap;
import java.util.UUID;

public class AttackDamageInputManager {

    public static class AttackDamageInputState {
        public String itemId;

        public AttackDamageInputState(String itemId) {
            this.itemId = itemId;
        }
    }

    private static final HashMap<UUID, AttackDamageInputState> playersInputting = new HashMap<>();

    public static void startAttackDamageInput(Player player, String itemId) {
        playersInputting.put(player.getUniqueId(), new AttackDamageInputState(itemId));

        player.closeInventory();
        player.sendMessage("");
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage(CC.translate("&6&l  Configurar Daño del Item"));
        player.sendMessage("");
        player.sendMessage(CC.translate("&7  Ingresa el valor de daño"));
        player.sendMessage(CC.translate("&7  Funciona en items de mod (NBT)"));
        player.sendMessage("");
        player.sendMessage(CC.translate("&7  Ejemplo: &f10"));
        player.sendMessage(CC.translate("&7  Ingresa &c0 &7para quitar el daño custom"));
        player.sendMessage(CC.translate("&7  Escribe &c'cancelar' &7para abortar"));
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage("");
    }

    public static boolean isInputtingAttackDamage(Player player) {
        return playersInputting.containsKey(player.getUniqueId());
    }

    public static void processAttackDamageInput(Player player, String input) {
        AttackDamageInputState state = playersInputting.get(player.getUniqueId());
        if (state == null) return;

        if (!CustomItemCommand.items.containsKey(state.itemId)) {
            player.sendMessage("");
            player.sendMessage(CC.translate("&c✗ Item no encontrado"));
            player.sendMessage("");
            finishAttackDamageInput(player);
            return;
        }

        int damage;
        try {
            damage = Integer.parseInt(input.trim());
            if (damage < 0) {
                player.sendMessage("");
                player.sendMessage(CC.translate("&c✗ El valor no puede ser negativo"));
                player.sendMessage("");
                startAttackDamageInput(player, state.itemId);
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("");
            player.sendMessage(CC.translate("&c✗ Número inválido"));
            player.sendMessage("");
            startAttackDamageInput(player, state.itemId);
            return;
        }

        CustomItem item = CustomItemCommand.items.get(state.itemId);

        if (damage == 0) {
            item.setAttackDamage(-1);
            rebuildNbtWithoutDamage(item);
        } else {
            item.setAttackDamage(damage);
            rebuildNbtWithDamage(item, damage);
        }

        CustomItemStorage storage = CustomItemStorage.getInstance();
        storage.saveItem(item);

        player.sendMessage("");
        if (damage == 0) {
            player.sendMessage(CC.translate("&a✓ Daño custom eliminado"));
        } else {
            player.sendMessage(CC.translate("&a✓ Daño configurado: &f" + damage));
        }
        player.sendMessage("");

        String itemId = state.itemId;
        finishAttackDamageInput(player);

        org.bukkit.Bukkit.getScheduler().scheduleSyncDelayedTask(org.debentialc.Main.instance, new Runnable() {
            public void run() {
                CustomItemMenus.openEditItemMenu(itemId).open(player);
            }
        }, 1L);
    }

    private static void rebuildNbtWithDamage(CustomItem item, int damage) {
        org.bukkit.inventory.ItemStack itemStack = new org.bukkit.inventory.ItemStack(item.getMaterial(), 1, item.getDurabilityData());

        if (item.getNbtData() != null && !item.getNbtData().isEmpty()) {
            NbtHandler nbt = new NbtHandler(itemStack);
            nbt.setCompoundFromString(item.getNbtData());
            itemStack = nbt.getItemStack();
        }

        NbtHandler nbt = new NbtHandler(itemStack);
        nbt.changeDamage(damage);

        if (item.isUnbreakable()) {
            nbt.setBoolean("Unbreakable", true);
        }

        item.setNbtData(nbt.getCompound() != null ? nbt.getCompound().toString() : null);
    }

    private static void rebuildNbtWithoutDamage(CustomItem item) {
        if (item.getNbtData() == null || item.getNbtData().isEmpty()) return;

        org.bukkit.inventory.ItemStack itemStack = new org.bukkit.inventory.ItemStack(item.getMaterial(), 1, item.getDurabilityData());
        NbtHandler nbt = new NbtHandler(itemStack);
        nbt.setCompoundFromString(item.getNbtData());

        net.minecraft.server.v1_7_R4.NBTTagCompound compound = nbt.getCompound();
        if (compound != null) {
            compound.remove("AttributeModifiers");
        }

        item.setNbtData(compound != null && !compound.isEmpty() ? compound.toString() : null);
    }

    public static void cancelAttackDamageInput(Player player) {
        player.sendMessage("");
        player.sendMessage(CC.translate("&c✗ Cancelado"));
        player.sendMessage("");
        finishAttackDamageInput(player);
    }

    private static void finishAttackDamageInput(Player player) {
        playersInputting.remove(player.getUniqueId());
    }
}