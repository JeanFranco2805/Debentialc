package org.debentialc.factions.menus;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.debentialc.factions.zones.FactionZone;
import org.debentialc.factions.zones.FactionZoneManager;
import org.debentialc.service.CC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FactionZoneEditMenu {

    public static SmartInventory createMenu(String zoneId) {
        return SmartInventory.builder()
                .id("faction_zone_edit_" + zoneId)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane((short) 7)));

                        FactionZone zone = FactionZoneManager.getInstance().getZone(zoneId);
                        if (zone == null) {
                            player.closeInventory();
                            return;
                        }

                        ItemStack info = new ItemStack(Material.BEACON);
                        ItemMeta infoMeta = info.getItemMeta();
                        infoMeta.setDisplayName(CC.translate("&c&lEditar Zona"));
                        List<String> infoLore = new ArrayList<>();
                        infoLore.add(CC.translate("&7ID: &f" + zone.getId()));
                        infoLore.add(CC.translate("&7Mundo: &f" + zone.getWorld()));
                        infoLore.add(CC.translate("&7Chunks: &f" + zone.getChunkCount()));
                        infoMeta.setLore(infoLore);
                        info.setItemMeta(infoMeta);
                        contents.set(0, 4, ClickableItem.empty(info));

                        ItemStack nameButton = new ItemStack(Material.NAME_TAG);
                        ItemMeta nameMeta = nameButton.getItemMeta();
                        nameMeta.setDisplayName(CC.translate("&e&lEditar Nombre"));
                        nameMeta.setLore(Arrays.asList(
                                CC.translate("&7Actual: &f" + zone.getDisplayName()),
                                "",
                                CC.translate("&a[CLICK PARA EDITAR]")
                        ));
                        nameButton.setItemMeta(nameMeta);
                        contents.set(2, 2, ClickableItem.of(nameButton, e -> {
                            FactionZoneInputManager.startEditName(player, zoneId);
                        }));

                        ItemStack costButton = new ItemStack(Material.DIAMOND);
                        ItemMeta costMeta = costButton.getItemMeta();
                        costMeta.setDisplayName(CC.translate("&b&lEditar Costo de Power"));
                        costMeta.setLore(Arrays.asList(
                                CC.translate("&7Actual: &f" + zone.getPowerCost()),
                                "",
                                CC.translate("&a[CLICK PARA EDITAR]")
                        ));
                        costButton.setItemMeta(costMeta);
                        contents.set(2, 4, ClickableItem.of(costButton, e -> {
                            FactionZoneInputManager.startEditCost(player, zoneId);
                        }));

                        ItemStack rarityButton = new ItemStack(Material.EMERALD);
                        ItemMeta rarityMeta = rarityButton.getItemMeta();
                        rarityMeta.setDisplayName(CC.translate("&a&lEditar Rareza"));
                        rarityMeta.setLore(Arrays.asList(
                                CC.translate("&7Actual: &f" + zone.getRarity()),
                                "",
                                CC.translate("&a[CLICK PARA EDITAR]")
                        ));
                        rarityButton.setItemMeta(rarityMeta);
                        contents.set(2, 6, ClickableItem.of(rarityButton, e -> {
                            FactionZoneInputManager.startEditRarity(player, zoneId);
                        }));

                        ItemStack backButton = new ItemStack(Material.ARROW);
                        ItemMeta backMeta = backButton.getItemMeta();
                        backMeta.setDisplayName(CC.translate("&b← Atrás"));
                        backButton.setItemMeta(backMeta);
                        contents.set(4, 4, ClickableItem.of(backButton, e -> FactionAdminZoneMenu.reopenLastPage(player)));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(5, 9)
                .title(CC.translate("&c&lEditar Zona"))
                .build();
    }

    public static void open(Player player, String zoneId) {
        createMenu(zoneId).open(player);
    }

    private static ItemStack createGlassPane(short color) {
        ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, color);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(CC.translate("&8"));
        glass.setItemMeta(meta);
        return glass;
    }
}
