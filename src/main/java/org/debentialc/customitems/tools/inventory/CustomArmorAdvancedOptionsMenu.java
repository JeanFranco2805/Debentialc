package org.debentialc.customitems.tools.inventory;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.debentialc.customitems.commands.RegisterItem;
import org.debentialc.customitems.tools.ci.CustomArmor;
import org.debentialc.service.CC;
import org.debentialc.boosters.core.BoosterParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Menú de opciones avanzadas para armaduras custom.
 * Incluye requisitos de rebirth y permiso.
 */
public class CustomArmorAdvancedOptionsMenu {

    public static SmartInventory createAdvancedOptionsMenu(String armorId) {
        CustomArmor armor = RegisterItem.items.get(armorId);
        if (armor == null) return null;

        return SmartInventory.builder()
                .id("ca_advanced_" + armorId)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane()));

                        // TÍTULO
                        ItemStack titleItem = new ItemStack(Material.PAPER);
                        ItemMeta titleMeta = titleItem.getItemMeta();
                        titleMeta.setDisplayName(CC.translate("&d&lOpciones Avanzadas"));
                        titleMeta.setLore(Arrays.asList(
                                CC.translate("&7Armadura: &f" + armorId),
                                CC.translate("&7Configura requisitos especiales")
                        ));
                        titleItem.setItemMeta(titleMeta);
                        contents.set(0, 4, ClickableItem.empty(titleItem));

                        // PROPIETARIO
                        ItemStack ownerButton = new ItemStack(Material.NAME_TAG);
                        ItemMeta ownerMeta = ownerButton.getItemMeta();
                        ownerMeta.setDisplayName(CC.translate("&a&lPropietario"));
                        List<String> ownerLore = new ArrayList<>();
                        ownerLore.add(CC.translate("&7Solo el jugador que recibe la armadura"));
                        ownerLore.add(CC.translate("&7podrá equiparla/usarla."));
                        ownerLore.add("");
                        ownerLore.add(CC.translate(armor.isOwnerOnly() ? "&a✓ ACTIVADO" : "&c✗ DESACTIVADO"));
                        ownerLore.add("");
                        ownerLore.add(CC.translate("&a[CLICK PARA ALTERNAR]"));
                        ownerMeta.setLore(ownerLore);
                        ownerButton.setItemMeta(ownerMeta);
                        contents.set(2, 4, ClickableItem.of(ownerButton, e -> {
                            armor.setOwnerOnly(!armor.isOwnerOnly());
                            org.debentialc.customitems.tools.storage.CustomArmorStorage.getInstance().saveArmor(armor);
                            player.sendMessage("");
                            player.sendMessage(CC.translate(armor.isOwnerOnly() ? "&a✓ La armadura ahora tiene propietario" : "&c✓ La armadura ya no tiene propietario"));
                            player.sendMessage("");
                            createAdvancedOptionsMenu(armorId).open(player);
                        }));

                        // COOLDOWN / EXPIRACIÓN
                        ItemStack cooldownButton = new ItemStack(Material.WATCH);
                        ItemMeta cooldownMeta = cooldownButton.getItemMeta();
                        cooldownMeta.setDisplayName(CC.translate("&4&lCooldown"));
                        List<String> cooldownLore = new ArrayList<>();
                        cooldownLore.add(CC.translate("&7Tiempo de vida real de la armadura"));
                        cooldownLore.add(CC.translate("&7Al expirar desaparece de inventarios/cofres/etc"));
                        cooldownLore.add("");
                        if (armor.getExpirationSeconds() > 0) {
                            cooldownLore.add(CC.translate("&7Actual: &f" + BoosterParser.formatSecondsToTime(armor.getExpirationSeconds())));
                        } else {
                            cooldownLore.add(CC.translate("&cSin cooldown de expiración"));
                        }
                        cooldownLore.add("");
                        cooldownLore.add(CC.translate("&a[CLICK PARA CONFIGURAR]"));
                        cooldownLore.add(CC.translate("&7Formato: 30m, 1h, 2d, 7d, 30s"));
                        cooldownMeta.setLore(cooldownLore);
                        cooldownButton.setItemMeta(cooldownMeta);
                        contents.set(2, 5, ClickableItem.of(cooldownButton, e -> {
                            ArmorExpirationInputManager.startExpirationInput(player, armorId);
                        }));

                        // BOTÓN ATRÁS
                        ItemStack backButton = new ItemStack(Material.ARROW);
                        ItemMeta backMeta = backButton.getItemMeta();
                        backMeta.setDisplayName(CC.translate("&b← Atrás"));
                        backButton.setItemMeta(backMeta);
                        contents.set(4, 4, ClickableItem.of(backButton, e -> {
                            CustomArmorMenus.openEditArmorMenu(armorId).open(player);
                        }));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(5, 9)
                .title(CC.translate("&d&lOpciones"))
                .build();
    }

    private static ItemStack createGlassPane() {
        ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(CC.translate("&8"));
        glass.setItemMeta(glassMeta);
        return glass;
    }
}
