package org.debentialc.customitems.tools.inventory;

import org.bukkit.entity.Player;
import org.debentialc.service.CC;
import org.debentialc.customitems.commands.CustomItemCommand;
import org.debentialc.customitems.tools.ci.CustomItem;
import org.debentialc.customitems.tools.storage.CustomItemStorage;

import java.util.HashMap;
import java.util.UUID;

public class CategoryInputManager {

    private static final HashMap<UUID, String> playersChangingCategory = new HashMap<>();

    private static final HashMap<UUID, String> playersCreatingCategory = new HashMap<>();

    public static void startCategoryChange(Player player, String itemId) {
        CustomItem item = CustomItemCommand.items.get(itemId);
        String currentCategory = item != null && item.getCategory() != null ? item.getCategory() : "Sin categoría";
        playersChangingCategory.put(player.getUniqueId(), itemId);

        player.closeInventory();
        player.sendMessage("");
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage(CC.translate("&6&l  Cambiar Categoría"));
        player.sendMessage("");
        player.sendMessage(CC.translate("&7  Categoría actual: &f" + currentCategory));
        player.sendMessage(CC.translate("&7  Escribe la nueva categoría"));
        player.sendMessage("");
        player.sendMessage(CC.translate("&7  Ejemplos: &fmisiones&7, &feventos&7, &fconsumibles"));
        player.sendMessage(CC.translate("&7  Escribe &c'cancelar' &7para abortar"));
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage("");
    }

    public static void startCategoryCreation(Player player) {
        playersCreatingCategory.put(player.getUniqueId(), "creating");
        player.closeInventory();
        player.sendMessage("");
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage(CC.translate("&6&l  Crear Nueva Categoría"));
        player.sendMessage("");
        player.sendMessage(CC.translate("&7  Escribe el nombre de la categoría"));
        player.sendMessage("");
        player.sendMessage(CC.translate("&7  Ejemplos: &fmisiones&7, &feventos&7, &fconsumibles"));
        player.sendMessage(CC.translate("&7  Escribe &c'cancelar' &7para abortar"));
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage("");
    }

    public static boolean isChangingCategory(Player player) {
        return playersChangingCategory.containsKey(player.getUniqueId()) || playersCreatingCategory.containsKey(player.getUniqueId());
    }

    public static void processCategoryInput(Player player, String input) {
        if (playersCreatingCategory.containsKey(player.getUniqueId())) {
            processCategoryCreation(player, input);
            return;
        }

        String itemId = playersChangingCategory.get(player.getUniqueId());
        if (itemId == null) return;

        CustomItem item = CustomItemCommand.items.get(itemId);
        if (item == null) {
            player.sendMessage(CC.translate("&c✗ Item no encontrado."));
            finishCategoryChange(player);
            return;
        }

        String newCategory = normalizeCategory(input);
        item.setCategory(newCategory);
        CustomItemCommand.items.put(itemId, item);
        CustomItemStorage.getInstance().saveItem(item);

        player.sendMessage("");
        player.sendMessage(CC.translate("&a✓ Categoría actualizada a &f" + (newCategory != null ? newCategory : "Sin categoría")));
        player.sendMessage("");

        finishCategoryChange(player);
        org.bukkit.Bukkit.getScheduler().scheduleSyncDelayedTask(org.debentialc.Main.instance, () -> {
            CustomItemMenus.openEditItemMenu(itemId, newCategory).open(player);
        }, 1L);
    }

    private static void processCategoryCreation(Player player, String input) {
        String newCategory = normalizeCategory(input);
        if (newCategory == null) {
            player.sendMessage(CC.translate("&c✗ Nombre de categoría inválido."));
            finishCategoryCreation(player);
            return;
        }

        player.sendMessage("");
        player.sendMessage(CC.translate("&a✓ Categoría '&f" + newCategory + "&a' creada."));
        player.sendMessage(CC.translate("&7  Ahora podés asignar items desde 'Sin categoría'."));
        player.sendMessage("");
        finishCategoryCreation(player);

        org.bukkit.Bukkit.getScheduler().scheduleSyncDelayedTask(org.debentialc.Main.instance, () -> {
            CustomItemMenus.openItemListMenu(1, newCategory).open(player);
        }, 1L);
    }

    private static String normalizeCategory(String input) {
        String category = input.trim();
        if (category.isEmpty() || category.equalsIgnoreCase("sin categoria") || category.equalsIgnoreCase("ninguna")) {
            return null;
        }
        return category.toLowerCase();
    }

    public static void cancelCategoryChange(Player player) {
        player.sendMessage("");
        player.sendMessage(CC.translate("&c✗ Cancelado"));
        player.sendMessage("");
        finishCategoryChange(player);
        finishCategoryCreation(player);
    }

    private static void finishCategoryChange(Player player) {
        playersChangingCategory.remove(player.getUniqueId());
    }

    private static void finishCategoryCreation(Player player) {
        playersCreatingCategory.remove(player.getUniqueId());
    }
}
