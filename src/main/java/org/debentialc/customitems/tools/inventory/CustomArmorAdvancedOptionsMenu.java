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

                        // REBIRTH
                        ItemStack rebirthButton = new ItemStack(Material.ENDER_PEARL);
                        ItemMeta rebirthMeta = rebirthButton.getItemMeta();
                        rebirthMeta.setDisplayName(CC.translate("&5&lRebirth Requerido"));
                        List<String> rebirthLore = new ArrayList<>();
                        rebirthLore.add(CC.translate("&7Restringe el uso/equipado de la armadura"));
                        rebirthLore.add(CC.translate("&7según el progreso de rebirth."));
                        rebirthLore.add("");
                        if (armor.getRequiredRebirthBlock() != null && !armor.getRequiredRebirthBlock().isEmpty() && armor.getRequiredRebirthLevel() > 0) {
                            rebirthLore.add(CC.translate("&7Bloque: &f" + armor.getRequiredRebirthBlock()));
                            rebirthLore.add(CC.translate("&7Nivel local: &f" + armor.getRequiredRebirthLevel()));
                        } else {
                            rebirthLore.add(CC.translate("&cSin requisito de rebirth"));
                        }
                        rebirthLore.add("");
                        rebirthLore.add(CC.translate("&a[CLICK PARA CONFIGURAR]"));
                        rebirthLore.add(CC.translate("&7Escribe &c'eliminar' &7para quitar"));
                        rebirthMeta.setLore(rebirthLore);
                        rebirthButton.setItemMeta(rebirthMeta);
                        contents.set(2, 2, ClickableItem.of(rebirthButton, e -> {
                            player.closeInventory();
                            CustomRequirementInputManager.startRebirthBlockInput(player, "armor", armorId);
                        }));

                        // PERMISO
                        ItemStack permButton = new ItemStack(Material.TRIPWIRE_HOOK);
                        ItemMeta permMeta = permButton.getItemMeta();
                        permMeta.setDisplayName(CC.translate("&c&lPermiso Requerido"));
                        List<String> permLore = new ArrayList<>();
                        permLore.add(CC.translate("&7Restringe el uso/equipado de la armadura"));
                        permLore.add(CC.translate("&7a jugadores con un permiso."));
                        permLore.add("");
                        if (armor.getRequiredPermission() != null && !armor.getRequiredPermission().isEmpty()) {
                            permLore.add(CC.translate("&7Permiso: &f" + armor.getRequiredPermission()));
                        } else {
                            permLore.add(CC.translate("&cSin permiso requerido"));
                        }
                        permLore.add("");
                        permLore.add(CC.translate("&a[CLICK PARA CONFIGURAR]"));
                        permLore.add(CC.translate("&7Escribe &c'eliminar' &7para quitar"));
                        permMeta.setLore(permLore);
                        permButton.setItemMeta(permMeta);
                        contents.set(2, 6, ClickableItem.of(permButton, e -> {
                            player.closeInventory();
                            CustomRequirementInputManager.startPermissionInput(player, "armor", armorId);
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
