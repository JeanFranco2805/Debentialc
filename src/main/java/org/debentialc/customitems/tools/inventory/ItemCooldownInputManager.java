package org.debentialc.customitems.tools.inventory;

import org.bukkit.entity.Player;
import org.debentialc.customitems.commands.CustomItemCommand;
import org.debentialc.customitems.tools.ci.CustomItem;
import org.debentialc.customitems.tools.storage.CustomItemStorage;
import org.debentialc.service.CC;
import org.debentialc.boosters.core.BoosterParser;

import java.util.HashMap;
import java.util.UUID;

public class ItemCooldownInputManager {

    public static class CooldownInputState {
        public String itemId;

        public CooldownInputState(String itemId) {
            this.itemId = itemId;
        }
    }

    private static final HashMap<UUID, CooldownInputState> playersInputting = new HashMap<>();

    public static void startCooldownInput(Player player, String itemId) {
        playersInputting.put(player.getUniqueId(), new CooldownInputState(itemId));
        player.closeInventory();
        player.sendMessage("");
        player.sendMessage(CC.translate("&6&lConfigurar Cooldown"));
        player.sendMessage(CC.translate("&7Escribe el tiempo de cooldown"));
        player.sendMessage(CC.translate("&7Formatos: &f50m, 1h, 2d, 30s"));
        player.sendMessage(CC.translate("&7Escribe &c0 &7para desactivar"));
        player.sendMessage(CC.translate("&7Escribe &ccancelar &7para salir"));
        player.sendMessage("");
    }

    public static boolean isInputtingCooldown(Player player) {
        return playersInputting.containsKey(player.getUniqueId());
    }

    public static void processCooldownInput(Player player, String input) {
        CooldownInputState state = playersInputting.get(player.getUniqueId());
        if (state == null) return;

        if (input.equalsIgnoreCase("cancelar")) {
            finishCooldownInput(player);
            player.sendMessage(CC.translate("&c✗ Cancelado"));
            return;
        }

        if (!CustomItemCommand.items.containsKey(state.itemId)) {
            player.sendMessage(CC.translate("&c✗ Item no encontrado"));
            finishCooldownInput(player);
            return;
        }

        try {
            int seconds;
            if (input.trim().equals("0")) {
                seconds = 0;
            } else {
                seconds = (int) BoosterParser.parseTimeToSeconds(input);
            }

            CustomItem item = CustomItemCommand.items.get(state.itemId);
            item.setCooldownSeconds(seconds);
            CustomItemStorage.getInstance().saveItem(item);

            if (seconds > 0) {
                player.sendMessage(CC.translate("&a✓ Cooldown configurado: &f" + BoosterParser.formatSecondsToTime(seconds)));
            } else {
                player.sendMessage(CC.translate("&a✓ Cooldown desactivado"));
            }

        } catch (IllegalArgumentException e) {
            player.sendMessage(CC.translate("&c✗ Tiempo inválido: " + e.getMessage()));
        }

        finishCooldownInput(player);
        org.bukkit.Bukkit.getScheduler().scheduleSyncDelayedTask(org.debentialc.Main.instance, () ->
                CustomItemAdvancedOptionsMenu.createAdvancedOptionsMenu(state.itemId).open(player), 1L);
    }

    public static void cancelCooldownInput(Player player) {
        finishCooldownInput(player);
        player.sendMessage(CC.translate("&c✗ Cancelado"));
    }

    private static void finishCooldownInput(Player player) {
        playersInputting.remove(player.getUniqueId());
    }
}
