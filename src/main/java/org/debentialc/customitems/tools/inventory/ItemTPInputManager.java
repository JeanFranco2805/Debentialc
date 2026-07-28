package org.debentialc.customitems.tools.inventory;

import org.bukkit.entity.Player;
import org.debentialc.service.CC;
import org.debentialc.customitems.tools.ci.CustomItem;
import org.debentialc.customitems.commands.CustomItemCommand;
import org.debentialc.customitems.tools.storage.CustomItemStorage;

import java.util.HashMap;
import java.util.UUID;

/**
 * Gestiona el input de valores de TP para items
 */
public class ItemTPInputManager {

    public static class TPInputState {
        public String itemId;

        public TPInputState(String itemId) {
            this.itemId = itemId;
        }
    }

    private static final HashMap<UUID, TPInputState> playersInputting = new HashMap<>();

    public static void startTPValueInput(Player player, String itemId) {
        playersInputting.put(player.getUniqueId(), new TPInputState(itemId));

        player.sendMessage("");
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage(CC.translate("&b&l  Valor de TP"));
        player.sendMessage("");
        player.sendMessage(CC.translate("&7  Ingresa cuántos TPs otorga"));
        player.sendMessage(CC.translate("&7  el item al ser consumido"));
        player.sendMessage("");
        player.sendMessage(CC.translate("&7  Ejemplo: &f100"));
        player.sendMessage(CC.translate("&7  Escribe &c'cancelar' &7para abortar"));
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage("");
    }

    public static boolean isInputtingTPValue(Player player) {
        return playersInputting.containsKey(player.getUniqueId());
    }

    public static void processTPValueInput(Player player, String input) {
        TPInputState state = playersInputting.get(player.getUniqueId());
        if (state == null) return;

        if (!CustomItemCommand.items.containsKey(state.itemId)) {
            player.sendMessage("");
            player.sendMessage(CC.translate("&c✗ Item no encontrado"));
            player.sendMessage("");
            finishTPValueInput(player);
            return;
        }

        int tpValue;
        try {
            tpValue = Integer.parseInt(input.trim());
            if (tpValue < 0) {
                player.sendMessage("");
                player.sendMessage(CC.translate("&c✗ El valor no puede ser negativo"));
                player.sendMessage("");
                startTPValueInput(player, state.itemId);
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("");
            player.sendMessage(CC.translate("&c✗ Número inválido"));
            player.sendMessage("");
            startTPValueInput(player, state.itemId);
            return;
        }

        CustomItem item = CustomItemCommand.items.get(state.itemId);
        item.setTpValue(tpValue);

        CustomItemStorage storage = CustomItemStorage.getInstance();
        storage.saveItem(item);

        player.sendMessage("");
        player.sendMessage(CC.translate("&a✓ Valor de TP configurado"));
        player.sendMessage(CC.translate("&7TP: &f" + tpValue));
        player.sendMessage("");

        String itemId = state.itemId;
        finishTPValueInput(player);

        org.bukkit.Bukkit.getScheduler().scheduleSyncDelayedTask(
                org.debentialc.Main.instance,
                () -> {
                    CustomItemAdvancedOptionsMenu.createTPConfigMenu(itemId).open(player);
                },
                1L
        );
    }

    public static void cancelTPValueInput(Player player) {
        player.sendMessage("");
        player.sendMessage(CC.translate("&c✗ Cancelado"));
        player.sendMessage("");
        finishTPValueInput(player);
    }

    private static void finishTPValueInput(Player player) {
        playersInputting.remove(player.getUniqueId());
    }
}