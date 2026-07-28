package org.debentialc.customitems.tools.inventory;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.debentialc.service.CC;
import org.debentialc.customitems.tools.ci.CustomItem;
import org.debentialc.customitems.commands.CustomItemCommand;
import org.debentialc.customitems.tools.storage.CustomItemStorage;

import java.util.HashMap;
import java.util.UUID;

public class ItemCreationManager {
    private static final HashMap<UUID, Boolean> playersCreatingItem = new HashMap<>();
    private static CustomItemStorage itemStorage = CustomItemStorage.getInstance();

    public static void startItemCreation(Player player) {
        playersCreatingItem.put(player.getUniqueId(), true);
        player.sendMessage("");
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage(CC.translate("&c&l  Crear Item Custom"));
        player.sendMessage("");
        player.sendMessage(CC.translate("&7  Ingresa el ID del item"));
        player.sendMessage(CC.translate("&7  Escribe &c'cancelar' &7para abortar"));
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage("");
    }

    public static boolean isCreatingItem(Player player) {
        return playersCreatingItem.getOrDefault(player.getUniqueId(), false);
    }

    public static void processItemCreation(Player player, String itemName) {
        ItemStack item = player.getItemInHand();

        if (item == null || item.getTypeId() == 0) {
            player.sendMessage("");
            player.sendMessage(CC.translate("&c✗ Sostén un item en la mano"));
            player.sendMessage("");
            startItemCreation(player);
            return;
        }

        if (item.getItemMeta() == null) {
            player.sendMessage("");
            player.sendMessage(CC.translate("&c✗ Item sin metadatos"));
            player.sendMessage("");
            startItemCreation(player);
            return;
        }

        if (itemName.isEmpty()) {
            player.sendMessage("");
            player.sendMessage(CC.translate("&c✗ ID no puede estar vacío"));
            player.sendMessage("");
            startItemCreation(player);
            return;
        }

        String itemId = itemName.toLowerCase().replace(" ", "_");
        if (CustomItemCommand.items.containsKey(itemId)) {
            player.sendMessage("");
            player.sendMessage(CC.translate("&c✗ ID ya existe"));
            player.sendMessage("");
            startItemCreation(player);
            return;
        }

        CustomItem customItem = new CustomItem()
                .setId(itemId)
                .setMaterial(item.getTypeId())
                .setLore(item.getItemMeta().getLore())
                .setDisplayName(item.getItemMeta().getDisplayName());

        CustomItemCommand.items.put(itemId, customItem);
        itemStorage.saveItem(customItem);

        player.sendMessage("");
        player.sendMessage(CC.translate("&a✓ Item creado: &f" + itemId));
        player.sendMessage("");
        finishItemCreation(player);
    }

    public static void cancelItemCreation(Player player) {
        player.sendMessage("");
        player.sendMessage(CC.translate("&c✗ Cancelado"));
        player.sendMessage("");
        finishItemCreation(player);
    }

    private static void finishItemCreation(Player player) {
        playersCreatingItem.put(player.getUniqueId(), false);
    }
}