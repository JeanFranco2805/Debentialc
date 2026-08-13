package org.debentialc.raids.menus;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.debentialc.raids.managers.RaidCategoryManager;
import org.debentialc.raids.managers.RaidConfig;
import org.debentialc.raids.managers.RaidManager;
import org.debentialc.raids.models.Raid;
import org.debentialc.service.CC;

import java.util.*;

/**
 * Menú visual con lista paginada de raids
 */
public class RaidListMenu {

    public static SmartInventory createRaidListMenu(int page) {
        return SmartInventory.builder()
                .id("raid_list_" + page)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        List<Raid> raids = RaidManager.getAllRaids();
                        int pageSize = 21;
                        int totalPages = Math.max(1, (int) Math.ceil((double) raids.size() / pageSize));

                        contents.fillBorders(ClickableItem.empty(RaidMainMenu.createGlassPane((short) 1)));

                        if (raids.isEmpty()) {
                            ItemStack emptyItem = new ItemStack(Material.ANVIL);
                            ItemMeta emptyMeta = emptyItem.getItemMeta();
                            emptyMeta.setDisplayName(CC.translate("&cNo hay raids creadas"));
                            emptyMeta.setLore(Arrays.asList(
                                    CC.translate("&7Crea una raid desde el menú principal"),
                                    CC.translate("&7o con /raid create")
                            ));
                            emptyItem.setItemMeta(emptyMeta);
                            contents.set(2, 4, ClickableItem.empty(emptyItem));
                        } else {
                            int start = (page - 1) * pageSize;
                            int end = Math.min(start + pageSize, raids.size());

                            int row = 1;
                            int col = 1;

                            for (int i = start; i < end; i++) {
                                Raid raid = raids.get(i);

                                ItemStack raidItem;
                                if (raid.getMenuItem() != null && !raid.getMenuItem().isEmpty()) {
                                    raidItem = RaidConfig.parseItem(raid.getMenuItem()).clone();
                                } else {
                                    Material mat = raid.isEnabled() ? Material.DIAMOND_SWORD : Material.IRON_SWORD;
                                    raidItem = new ItemStack(mat);
                                }
                                ItemMeta raidMeta = raidItem.getItemMeta();
                                raidMeta.setDisplayName(CC.translate("&6&l" + raid.getDisplayName()));

                                List<String> lore = new ArrayList<>();
                                lore.add(CC.translate("&7ID: &f" + raid.getRaidId()));
                                lore.add(CC.translate("&7Descripción: &f" + (raid.getDescription() != null ? raid.getDescription() : "Sin descripción")));
                                lore.add(CC.translate("&7Oleadas: &f" + raid.getTotalWaves()));
                                lore.add(CC.translate("&7Jugadores: &f" + raid.getMinPlayers() + "-" + raid.getMaxPlayers()));
                                lore.add(CC.translate("&7Categoría: &f" + RaidCategoryManager.getDisplayName(raid.getCategory())));
                                lore.add(CC.translate("&7Cooldown: &f" + (raid.getCooldownSeconds() / 60) + " min"));
                                lore.add(CC.translate("&7Estado: " + (raid.isEnabled() ? "&a✓ Habilitada" : "&c✗ Deshabilitada")));
                                lore.add(CC.translate("&7Configurada: " + (raid.isConfigured() ? "&a✓ Sí" : "&c✗ No")));
                                lore.add("");
                                lore.add(CC.translate("&a[CLICK PARA EDITAR]"));

                                raidMeta.setLore(lore);
                                raidItem.setItemMeta(raidMeta);

                                final String raidId = raid.getRaidId();
                                contents.set(row, col, ClickableItem.of(raidItem, e -> {
                                    RaidConfigMenu.createRaidConfigMenu(raidId).open(player);
                                }));

                                col++;
                                if (col >= 8) {
                                    col = 1;
                                    row++;
                                    if (row >= 4) break;
                                }
                            }
                        }

                        // Paginación
                        if (page > 1) {
                            ItemStack prevButton = new ItemStack(Material.ARROW);
                            ItemMeta prevMeta = prevButton.getItemMeta();
                            prevMeta.setDisplayName(CC.translate("&b← Anterior"));
                            prevButton.setItemMeta(prevMeta);
                            contents.set(4, 2, ClickableItem.of(prevButton, e -> {
                                createRaidListMenu(page - 1).open(player);
                            }));
                        }

                        ItemStack pageItem = new ItemStack(Material.BOOK);
                        ItemMeta pageMeta = pageItem.getItemMeta();
                        pageMeta.setDisplayName(CC.translate("&f&lPágina " + page + "/" + totalPages));
                        pageItem.setItemMeta(pageMeta);
                        contents.set(4, 4, ClickableItem.empty(pageItem));

                        if (page < totalPages) {
                            ItemStack nextButton = new ItemStack(Material.ARROW);
                            ItemMeta nextMeta = nextButton.getItemMeta();
                            nextMeta.setDisplayName(CC.translate("&bSiguiente →"));
                            nextButton.setItemMeta(nextMeta);
                            contents.set(4, 6, ClickableItem.of(nextButton, e -> {
                                createRaidListMenu(page + 1).open(player);
                            }));
                        }

                        // Atrás
                        ItemStack backButton = new ItemStack(Material.REDSTONE_BLOCK);
                        ItemMeta backMeta = backButton.getItemMeta();
                        backMeta.setDisplayName(CC.translate("&c← Atrás"));
                        backButton.setItemMeta(backMeta);
                        contents.set(4, 8, ClickableItem.of(backButton, e -> {
                            RaidAdminCategoryMenu.open(player);
                        }));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(5, 9)
                .title(CC.translate("&6&lRaids #" + page))
                .build();
    }
}