package org.debentialc.customitems.tools.inventory;

import org.bukkit.entity.Player;
import org.debentialc.service.CC;
import org.debentialc.customitems.tools.ci.CustomItem;
import org.debentialc.customitems.tools.ci.CustomArmor;
import org.debentialc.customitems.commands.CustomItemCommand;
import org.debentialc.customitems.commands.RegisterItem;
import org.debentialc.customitems.tools.storage.CustomItemStorage;
import org.debentialc.customitems.tools.storage.CustomArmorStorage;

import java.util.HashMap;
import java.util.UUID;

public class ItemIdChangeManager {

    public static class MaterialIdChangeState {
        public String itemId;
        public String type;
        public int oldMaterialId;
        public short oldData;

        public MaterialIdChangeState(String itemId, String type, int oldMaterialId, short oldData) {
            this.itemId = itemId;
            this.type = type;
            this.oldMaterialId = oldMaterialId;
            this.oldData = oldData;
        }
    }

    private static final HashMap<UUID, MaterialIdChangeState> playersChangingMaterialId = new HashMap<>();

    public static void startMaterialIdChange(Player player, String itemId, String type) {
        int oldMaterialId = 0;
        short oldData = 0;

        if ("item".equals(type) && CustomItemCommand.items.containsKey(itemId)) {
            CustomItem ci = CustomItemCommand.items.get(itemId);
            oldMaterialId = ci.getMaterial();
            oldData = ci.getDurabilityData();
        } else if ("armor".equals(type) && RegisterItem.items.containsKey(itemId)) {
            CustomArmor ca = RegisterItem.items.get(itemId);
            oldMaterialId = ca.getMaterial();
        }

        playersChangingMaterialId.put(player.getUniqueId(), new MaterialIdChangeState(itemId, type, oldMaterialId, oldData));

        String currentDisplay = oldData > 0 ? oldMaterialId + "/" + oldData : String.valueOf(oldMaterialId);

        player.closeInventory();
        player.sendMessage("");
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage(CC.translate("&6&l  Cambiar Material ID"));
        player.sendMessage("");
        player.sendMessage(CC.translate("&7  Material actual: &f" + currentDisplay));
        player.sendMessage(CC.translate("&7  Ingresa el nuevo ID del material"));
        player.sendMessage("");
        player.sendMessage(CC.translate("&7  Formato aceptado:"));
        player.sendMessage(CC.translate("&f    345        &7(solo el ID)"));
        player.sendMessage(CC.translate("&f    345/5      &7(ID con data value)"));
        player.sendMessage(CC.translate("&f    567/50     &7(item de mod con data)"));
        player.sendMessage("");
        player.sendMessage(CC.translate("&7  Escribe &c'cancelar' &7para abortar"));
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage("");
    }

    public static boolean isChangingMaterialId(Player player) {
        return playersChangingMaterialId.containsKey(player.getUniqueId());
    }

    public static void processMaterialIdChange(Player player, String input) {
        MaterialIdChangeState state = playersChangingMaterialId.get(player.getUniqueId());
        if (state == null) return;

        String materialInput = input.trim();

        if (materialInput.isEmpty()) {
            player.sendMessage("");
            player.sendMessage(CC.translate("&c✗ El Material ID no puede estar vacío"));
            player.sendMessage("");
            startMaterialIdChange(player, state.itemId, state.type);
            return;
        }

        if (!materialInput.matches("\\d+(/\\d+)?")) {
            player.sendMessage("");
            player.sendMessage(CC.translate("&c✗ Formato inválido"));
            player.sendMessage(CC.translate("&7  Formato correcto: 345 o 345/5 o 567/50"));
            player.sendMessage("");
            startMaterialIdChange(player, state.itemId, state.type);
            return;
        }

        int newMaterialId;
        short newData = 0;

        try {
            if (materialInput.contains("/")) {
                String[] parts = materialInput.split("/");
                newMaterialId = Integer.parseInt(parts[0]);
                newData = Short.parseShort(parts[1]);
            } else {
                newMaterialId = Integer.parseInt(materialInput);
            }

            if (newMaterialId <= 0) {
                player.sendMessage("");
                player.sendMessage(CC.translate("&c✗ El Material ID debe ser mayor a 0"));
                player.sendMessage("");
                startMaterialIdChange(player, state.itemId, state.type);
                return;
            }

        } catch (NumberFormatException e) {
            player.sendMessage("");
            player.sendMessage(CC.translate("&c✗ El Material ID debe ser un número válido"));
            player.sendMessage("");
            startMaterialIdChange(player, state.itemId, state.type);
            return;
        }

        if (newMaterialId == state.oldMaterialId && newData == state.oldData) {
            player.sendMessage("");
            player.sendMessage(CC.translate("&c✗ El nuevo Material ID es igual al anterior"));
            player.sendMessage("");
            startMaterialIdChange(player, state.itemId, state.type);
            return;
        }

        if ("item".equals(state.type)) {
            changeMaterialIdItem(player, state.itemId, newMaterialId, newData, materialInput);
        } else if ("armor".equals(state.type)) {
            changeMaterialIdArmor(player, state.itemId, newMaterialId, materialInput);
        }

        finishMaterialIdChange(player);
    }

    private static void changeMaterialIdItem(Player player, String itemId, int newMaterialId, short newData, String originalInput) {
        if (!CustomItemCommand.items.containsKey(itemId)) {
            player.sendMessage("");
            player.sendMessage(CC.translate("&c✗ Item no encontrado"));
            player.sendMessage("");
            return;
        }

        CustomItem item = CustomItemCommand.items.get(itemId);
        int oldMaterialId = item.getMaterial();
        short oldData = item.getDurabilityData();
        String oldDisplay = oldData > 0 ? oldMaterialId + "/" + oldData : String.valueOf(oldMaterialId);

        item.setMaterial(newMaterialId);
        item.setDurabilityData(newData);

        CustomItemStorage storage = CustomItemStorage.getInstance();
        storage.saveItem(item);

        player.sendMessage("");
        player.sendMessage(CC.translate("&a✓ Material ID cambiado exitosamente"));
        player.sendMessage(CC.translate("&7Anterior: &f" + oldDisplay));
        player.sendMessage(CC.translate("&7Nuevo: &f" + originalInput));
        player.sendMessage("");

        org.bukkit.Bukkit.getScheduler().scheduleSyncDelayedTask(org.debentialc.Main.instance, new Runnable() {
            public void run() {
                CustomItemMenus.openEditItemMenu(itemId).open(player);
            }
        }, 1L);
    }

    private static void changeMaterialIdArmor(Player player, String armorId, int newMaterialId, String originalInput) {
        if (!RegisterItem.items.containsKey(armorId)) {
            player.sendMessage("");
            player.sendMessage(CC.translate("&c✗ Armadura no encontrada"));
            player.sendMessage("");
            return;
        }

        CustomArmor armor = RegisterItem.items.get(armorId);
        int oldMaterialId = armor.getMaterial();

        armor.setMaterial(newMaterialId);

        CustomArmorStorage storage = CustomArmorStorage.getInstance();
        storage.saveArmor(armor);

        RegisterItem.items.put(armorId, armor);

        player.sendMessage("");
        player.sendMessage(CC.translate("&a✓ Material ID cambiado exitosamente"));
        player.sendMessage(CC.translate("&7Anterior: &f" + oldMaterialId));
        player.sendMessage(CC.translate("&7Nuevo: &f" + originalInput));
        player.sendMessage("");

        org.bukkit.Bukkit.getScheduler().scheduleSyncDelayedTask(org.debentialc.Main.instance, new Runnable() {
            public void run() {
                CustomArmorMenus.openEditArmorMenu(armorId).open(player);
            }
        }, 1L);
    }

    public static void cancelMaterialIdChange(Player player) {
        player.sendMessage("");
        player.sendMessage(CC.translate("&c✗ Cancelado"));
        player.sendMessage("");
        finishMaterialIdChange(player);
    }

    private static void finishMaterialIdChange(Player player) {
        playersChangingMaterialId.remove(player.getUniqueId());
    }
}