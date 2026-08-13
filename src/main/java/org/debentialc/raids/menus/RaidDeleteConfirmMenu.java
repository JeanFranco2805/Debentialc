package org.debentialc.raids.menus;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.debentialc.raids.managers.RaidManager;
import org.debentialc.raids.managers.RaidStorageManager;
import org.debentialc.raids.models.Raid;
import org.debentialc.service.CC;

import java.util.Arrays;

/**
 * Menú visual de confirmación para eliminar una raid
 */
public class RaidDeleteConfirmMenu {

    public static SmartInventory createDeleteConfirmMenu(String raidId) {
        Raid raid = RaidManager.getRaidById(raidId);
        if (raid == null) return null;

        return SmartInventory.builder()
                .id("raid_delete_" + raidId)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(RaidMainMenu.createGlassPane((short) 14)));

                        // Advertencia
                        ItemStack warnItem = new ItemStack(Material.REDSTONE_BLOCK);
                        ItemMeta warnMeta = warnItem.getItemMeta();
                        warnMeta.setDisplayName(CC.translate("&c&l¿Eliminar Raid?"));
                        warnMeta.setLore(Arrays.asList(
                                CC.translate("&7Esta acción no puede deshacerse"),
                                CC.translate("&7La raid &f" + raid.getDisplayName() + " &7será"),
                                CC.translate("&7eliminada permanentemente"),
                                CC.translate("&7Oleadas: &f" + raid.getTotalWaves())
                        ));
                        warnItem.setItemMeta(warnMeta);
                        contents.set(1, 4, ClickableItem.empty(warnItem));

                        // SÍ
                        ItemStack yesButton = new ItemStack(Material.EMERALD_BLOCK);
                        ItemMeta yesMeta = yesButton.getItemMeta();
                        yesMeta.setDisplayName(CC.translate("&a&lSÍ, ELIMINAR"));
                        yesButton.setItemMeta(yesMeta);
                        contents.set(2, 3, ClickableItem.of(yesButton, e -> {
                            RaidManager.deleteRaid(raidId);
                            RaidStorageManager.deleteRaidFiles(raidId);
                            RaidStorageManager.saveAllRaids();
                            player.sendMessage(CC.translate("&a✓ Raid eliminada: " + raid.getDisplayName()));
                            RaidListMenu.createRaidListMenu(1).open(player);
                        }));

                        // NO
                        ItemStack noButton = new ItemStack(Material.REDSTONE_BLOCK);
                        ItemMeta noMeta = noButton.getItemMeta();
                        noMeta.setDisplayName(CC.translate("&c&lCANCELAR"));
                        noButton.setItemMeta(noMeta);
                        contents.set(2, 5, ClickableItem.of(noButton, e -> {
                            RaidConfigMenu.createRaidConfigMenu(raidId).open(player);
                        }));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(4, 9)
                .title(CC.translate("&c&lConfirmar Eliminación"))
                .build();
    }
}