package org.debentialc.customitems.tools.inventory;

import org.bukkit.entity.Player;
import org.debentialc.customitems.commands.CustomItemCommand;
import org.debentialc.customitems.commands.RegisterItem;
import org.debentialc.service.CC;

import java.util.HashMap;
import java.util.UUID;

public class DurabilityInputManager {

    public static class DurabilityInputState {
        public String itemId;
        public String type; // "item" o "armor"

        public DurabilityInputState(String itemId, String type) {
            this.itemId = itemId;
            this.type = type;
        }
    }

    private static final HashMap<UUID, DurabilityInputState> playersInputting = new HashMap<>();

    public static void startDurabilityInput(Player player, String itemId, String type) {
        playersInputting.put(player.getUniqueId(), new DurabilityInputState(itemId, type));

        player.closeInventory();
        player.sendMessage("");
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage(CC.translate("&3&l  Modificar Durabilidad"));
        player.sendMessage("");
        player.sendMessage(CC.translate("&7  Ingresa la durabilidad"));
        player.sendMessage(CC.translate("&7  Ejemplo: &f100 &7o &f500"));
        player.sendMessage(CC.translate("&7  Escribe &c'cancelar' &7para abortar"));
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage("");
    }

    public static boolean isInputtingDurability(Player player) {
        return playersInputting.containsKey(player.getUniqueId());
    }

    public static void processDurabilityInput(Player player, String input) {
        DurabilityInputState state = playersInputting.get(player.getUniqueId());
        if (state == null) return;

        int durability;
        try {
            durability = Integer.parseInt(input);
            if (durability < 0) {
                player.sendMessage("");
                player.sendMessage(CC.translate("&c✗ La durabilidad no puede ser negativa"));
                player.sendMessage("");
                startDurabilityInput(player, state.itemId, state.type);
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("");
            player.sendMessage(CC.translate("&c✗ Número inválido"));
            player.sendMessage("");
            startDurabilityInput(player, state.itemId, state.type);
            return;
        }

        // Aplicar durabilidad según el tipo
        if ("item".equals(state.type)) {
            applyDurabilityToItem(player, state.itemId, durability);
        } else if ("armor".equals(state.type)) {
            applyDurabilityToArmor(player, state.itemId, durability);
        }

        player.sendMessage("");
        player.sendMessage(CC.translate("&a✓ Durabilidad configurada"));
        player.sendMessage(CC.translate("&7Durabilidad: &f" + durability));
        player.sendMessage("");

        String type = state.type;
        String itemId = state.itemId;
        finishDurabilityInput(player);

        org.bukkit.Bukkit.getScheduler().scheduleSyncDelayedTask(org.debentialc.Main.instance, () -> {
            if ("item".equals(type)) {
                org.debentialc.customitems.tools.inventory.CustomItemMenus.openEditItemMenu(itemId).open(player);
            } else {
                org.debentialc.customitems.tools.inventory.CustomArmorMenus.openEditArmorMenu(itemId).open(player);
            }
        }, 1L);
    }

    /**
     * Aplica durabilidad a un item custom
     */
    private static void applyDurabilityToItem(Player player, String itemId, int durability) {
        if (!CustomItemCommand.items.containsKey(itemId)) {
            player.sendMessage(CC.translate("&c✗ Item no encontrado"));
            return;
        }

        org.debentialc.customitems.tools.ci.CustomItem item = CustomItemCommand.items.get(itemId);

        // Guardar durabilidad máxima en el objeto CustomItem
        item.setMaxDurability(durability);

        org.debentialc.customitems.tools.storage.CustomItemStorage.getInstance().saveItem(item);

        player.sendMessage(CC.translate("&7Nota: La durabilidad se aplicará cuando"));
        player.sendMessage(CC.translate("&7se entregue el item al jugador"));
    }

    /**
     * Aplica durabilidad a una armadura custom
     */
    private static void applyDurabilityToArmor(Player player, String armorId, int durability) {
        if (!RegisterItem.items.containsKey(armorId)) {
            player.sendMessage(CC.translate("&c✗ Armadura no encontrada"));
            return;
        }

        org.debentialc.customitems.tools.ci.CustomArmor armor = RegisterItem.items.get(armorId);

        // Guardar durabilidad máxima en el objeto CustomArmor
        armor.setMaxDurability(durability);

        org.debentialc.customitems.tools.storage.CustomArmorStorage storage = org.debentialc.customitems.tools.storage.CustomArmorStorage.getInstance();
        storage.saveArmor(armor);

        RegisterItem.items.put(armorId, armor);

        player.sendMessage(CC.translate("&7Nota: La durabilidad se aplicará cuando"));
        player.sendMessage(CC.translate("&7se entregue la armadura al jugador"));
    }

    public static void cancelDurabilityInput(Player player) {
        player.sendMessage("");
        player.sendMessage(CC.translate("&c✗ Cancelado"));
        player.sendMessage("");
        finishDurabilityInput(player);
    }

    private static void finishDurabilityInput(Player player) {
        playersInputting.remove(player.getUniqueId());
    }
}