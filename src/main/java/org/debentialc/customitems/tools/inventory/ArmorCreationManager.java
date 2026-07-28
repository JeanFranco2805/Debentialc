package org.debentialc.customitems.tools.inventory;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.debentialc.service.CC;
import org.debentialc.customitems.tools.ci.CustomArmor;
import org.debentialc.customitems.commands.RegisterItem;

import java.util.HashMap;
import java.util.UUID;

public class ArmorCreationManager {
    private static final HashMap<UUID, Boolean> playersCreatingArmor = new HashMap<>();

    public static void startArmorCreation(Player player) {
        playersCreatingArmor.put(player.getUniqueId(), true);
        player.sendMessage("");
        player.sendMessage(CC.translate(" &b────────────────────────────────"));
        player.sendMessage(CC.translate(" &b  &f&lCREAR ARMADURA CUSTOM          &b&l"));
        player.sendMessage(CC.translate(" &b"));
        player.sendMessage(CC.translate(" &b &7Escribe el ID de la armadura      &b&l"));
        player.sendMessage(CC.translate(" &b &c(Escribe 'cancelar' para abortar) &b&l"));
        player.sendMessage(CC.translate(" &b────────────────────────────────"));
        player.sendMessage("");
    }

    public static boolean isCreatingArmor(Player player) {
        return playersCreatingArmor.getOrDefault(player.getUniqueId(), false);
    }

    public static void processArmorCreation(Player player, String armorName) {
        ItemStack armor = player.getItemInHand();

        if (armor == null || armor.getTypeId() == 0) {
            player.sendMessage(CC.translate("&c✗ Debes sostener una armadura en la mano"));
            startArmorCreation(player);
            return;
        }

        if (armor.getItemMeta() == null) {
            player.sendMessage(CC.translate("&c✗ Esta armadura no tiene metadatos"));
            startArmorCreation(player);
            return;
        }

        if (armorName.isEmpty()) {
            player.sendMessage(CC.translate("&c✗ El ID no puede estar vacío"));
            startArmorCreation(player);
            return;
        }

        String armorId = armorName.toLowerCase().replace(" ", "_");
        if (RegisterItem.items.containsKey(armorId)) {
            player.sendMessage(CC.translate("&c✗ Ya existe una armadura con ese ID"));
            startArmorCreation(player);
            return;
        }

        CustomArmor customArmor = new CustomArmor()
                .setId(armorId)
                .setMaterial(armor.getTypeId())
                .setLore(armor.getItemMeta().getLore())
                .setDisplayName(armor.getItemMeta().getDisplayName());

        RegisterItem.items.put(armorId, customArmor);

        // Guardar en BD
        org.debentialc.customitems.tools.storage.CustomArmorStorage storage =
                org.debentialc.customitems.tools.storage.CustomArmorStorage.getInstance();
        storage.saveArmor(customArmor);

        player.sendMessage("");
        player.sendMessage(CC.translate("&a✓ Armadura creada exitosamente"));
        player.sendMessage(CC.translate("&7ID: &f" + armorId));
        player.sendMessage("");
        finishArmorCreation(player);
    }

    public static void cancelArmorCreation(Player player) {
        player.sendMessage("");
        player.sendMessage(CC.translate("&c✗ Creación cancelada"));
        player.sendMessage("");
        finishArmorCreation(player);
    }

    private static void finishArmorCreation(Player player) {
        playersCreatingArmor.put(player.getUniqueId(), false);
    }
}