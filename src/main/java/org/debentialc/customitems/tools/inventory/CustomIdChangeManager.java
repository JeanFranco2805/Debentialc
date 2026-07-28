package org.debentialc.customitems.tools.inventory;

import org.bukkit.entity.Player;
import org.debentialc.service.CC;
import org.debentialc.customitems.commands.CustomItemCommand;
import org.debentialc.customitems.commands.RegisterItem;
import org.debentialc.customitems.tools.ci.CustomArmor;
import org.debentialc.customitems.tools.ci.CustomItem;
import org.debentialc.customitems.tools.storage.CustomArmorStorage;
import org.debentialc.customitems.tools.storage.CustomItemStorage;

import java.util.HashMap;
import java.util.UUID;

/**
 * Gestiona el cambio del ID interno (system ID) de items y armaduras custom.
 * Este ID es el identificador bajo el cual se guardan en los mapas y en el YAML.
 */
public class CustomIdChangeManager {

    public static class CustomIdChangeState {
        public String oldId;
        public String type; // "item" o "armor"

        public CustomIdChangeState(String oldId, String type) {
            this.oldId = oldId;
            this.type = type;
        }
    }

    private static final HashMap<UUID, CustomIdChangeState> playersChangingId = new HashMap<>();

    public static void startIdChange(Player player, String oldId, String type) {
        playersChangingId.put(player.getUniqueId(), new CustomIdChangeState(oldId, type));

        player.closeInventory();
        player.sendMessage("");
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage(CC.translate("&6&l  Cambiar ID Custom"));
        player.sendMessage("");
        player.sendMessage(CC.translate("&7  ID actual: &f" + oldId));
        player.sendMessage(CC.translate("&7  Ingresa el nuevo ID del sistema"));
        player.sendMessage("");
        player.sendMessage(CC.translate("&7  Escribe &c'cancelar' &7para abortar"));
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage("");
    }

    public static boolean isChangingId(Player player) {
        return playersChangingId.containsKey(player.getUniqueId());
    }

    public static void processIdChange(Player player, String input) {
        CustomIdChangeState state = playersChangingId.get(player.getUniqueId());
        if (state == null) return;

        String newId = input.trim();
        if (newId.isEmpty()) {
            player.sendMessage("");
            player.sendMessage(CC.translate("&c✗ El ID no puede estar vacío"));
            player.sendMessage("");
            startIdChange(player, state.oldId, state.type);
            return;
        }

        if (newId.equalsIgnoreCase(state.oldId)) {
            player.sendMessage("");
            player.sendMessage(CC.translate("&c✗ El nuevo ID es igual al anterior"));
            player.sendMessage("");
            startIdChange(player, state.oldId, state.type);
            return;
        }

        if ("item".equals(state.type)) {
            changeItemId(player, state.oldId, newId);
        } else if ("armor".equals(state.type)) {
            changeArmorId(player, state.oldId, newId);
        }

        finishIdChange(player);
    }

    private static void changeItemId(Player player, String oldId, String newId) {
        if (!CustomItemCommand.items.containsKey(oldId)) {
            player.sendMessage("");
            player.sendMessage(CC.translate("&c✗ Item no encontrado"));
            player.sendMessage("");
            return;
        }
        if (CustomItemCommand.items.containsKey(newId)) {
            player.sendMessage("");
            player.sendMessage(CC.translate("&c✗ Ya existe un item con el ID &f" + newId));
            player.sendMessage("");
            startIdChange(player, oldId, "item");
            return;
        }

        CustomItem item = CustomItemCommand.items.remove(oldId);
        item.setId(newId);
        CustomItemCommand.items.put(newId, item);

        CustomItemStorage storage = CustomItemStorage.getInstance();
        storage.deleteItem(oldId);
        storage.saveItem(item);

        player.sendMessage("");
        player.sendMessage(CC.translate("&a✓ ID del item cambiado exitosamente"));
        player.sendMessage(CC.translate("&7Anterior: &f" + oldId));
        player.sendMessage(CC.translate("&7Nuevo: &f" + newId));
        player.sendMessage("");

        org.bukkit.Bukkit.getScheduler().scheduleSyncDelayedTask(org.debentialc.Main.instance, new Runnable() {
            public void run() {
                CustomItemMenus.openEditItemMenu(newId).open(player);
            }
        }, 1L);
    }

    private static void changeArmorId(Player player, String oldId, String newId) {
        if (!RegisterItem.items.containsKey(oldId)) {
            player.sendMessage("");
            player.sendMessage(CC.translate("&c✗ Armadura no encontrada"));
            player.sendMessage("");
            return;
        }
        if (RegisterItem.items.containsKey(newId)) {
            player.sendMessage("");
            player.sendMessage(CC.translate("&c✗ Ya existe una armadura con el ID &f" + newId));
            player.sendMessage("");
            startIdChange(player, oldId, "armor");
            return;
        }

        CustomArmor armor = RegisterItem.items.remove(oldId);
        armor.setId(newId);
        RegisterItem.items.put(newId, armor);

        CustomArmorStorage storage = CustomArmorStorage.getInstance();
        storage.deleteArmor(oldId);
        storage.saveArmor(armor);

        player.sendMessage("");
        player.sendMessage(CC.translate("&a✓ ID de la armadura cambiado exitosamente"));
        player.sendMessage(CC.translate("&7Anterior: &f" + oldId));
        player.sendMessage(CC.translate("&7Nuevo: &f" + newId));
        player.sendMessage("");

        org.bukkit.Bukkit.getScheduler().scheduleSyncDelayedTask(org.debentialc.Main.instance, new Runnable() {
            public void run() {
                CustomArmorMenus.openEditArmorMenu(newId).open(player);
            }
        }, 1L);
    }

    public static void cancelIdChange(Player player) {
        player.sendMessage("");
        player.sendMessage(CC.translate("&c✗ Cancelado"));
        player.sendMessage("");
        finishIdChange(player);
    }

    private static void finishIdChange(Player player) {
        playersChangingId.remove(player.getUniqueId());
    }
}
