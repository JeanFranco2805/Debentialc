package org.debentialc.crates.menus;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.debentialc.crates.managers.CrateManager;
import org.debentialc.crates.models.CrateRarity;
import org.debentialc.service.CC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CrateRarityMenu {

    public static SmartInventory createMenu(int page) {
        return SmartInventory.builder()
                .id("crate_rarities_" + page)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane((short) 3)));

                        ItemStack info = new ItemStack(Material.DIAMOND);
                        ItemMeta infoMeta = info.getItemMeta();
                        infoMeta.setDisplayName(CC.translate("&b&lRarezas"));
                        info.setItemMeta(infoMeta);
                        contents.set(0, 4, ClickableItem.empty(info));

                        List<CrateRarity> rarityList = new ArrayList<>(CrateManager.getInstance().getAllRarities().values());
                        int pageSize = 7;
                        int totalPages = Math.max(1, (int) Math.ceil((double) rarityList.size() / pageSize));
                        if (page < 1) return;
                        if (page > totalPages) return;

                        int start = (page - 1) * pageSize;
                        int end = Math.min(start + pageSize, rarityList.size());

                        int slot = 10;
                        for (int i = start; i < end; i++) {
                            CrateRarity rarity = rarityList.get(i);
                            ItemStack item = new ItemStack(Material.PAPER);
                            ItemMeta meta = item.getItemMeta();
                            meta.setDisplayName(CC.translate(rarity.getColor() + rarity.getDisplayName()));
                            List<String> lore = new ArrayList<>();
                            lore.add(CC.translate("&7ID: &f" + rarity.getId()));
                            lore.add(CC.translate("&7Peso: &f" + rarity.getWeight()));
                            lore.add(CC.translate("&7Anunciar: &f" + (rarity.isAnnounce() ? "Sí" : "No")));
                            meta.setLore(lore);
                            item.setItemMeta(meta);
                            contents.set(slot / 9, slot % 9, ClickableItem.empty(item));
                            slot++;
                        }

                        if (page > 1) {
                            ItemStack prev = new ItemStack(Material.ARROW);
                            ItemMeta prevMeta = prev.getItemMeta();
                            prevMeta.setDisplayName(CC.translate("&b← Anterior"));
                            prev.setItemMeta(prevMeta);
                            contents.set(5, 2, ClickableItem.of(prev, e -> createMenu(page - 1).open(player)));
                        }

                        ItemStack pageItem = new ItemStack(Material.PAPER);
                        ItemMeta pageMeta = pageItem.getItemMeta();
                        pageMeta.setDisplayName(CC.translate("&fPágina " + page + "/" + totalPages));
                        pageItem.setItemMeta(pageMeta);
                        contents.set(5, 4, ClickableItem.empty(pageItem));

                        if (page < totalPages) {
                            ItemStack next = new ItemStack(Material.ARROW);
                            ItemMeta nextMeta = next.getItemMeta();
                            nextMeta.setDisplayName(CC.translate("&bSiguiente →"));
                            next.setItemMeta(nextMeta);
                            contents.set(5, 6, ClickableItem.of(next, e -> createMenu(page + 1).open(player)));
                        }

                        ItemStack help = new ItemStack(Material.BOOK);
                        ItemMeta helpMeta = help.getItemMeta();
                        helpMeta.setDisplayName(CC.translate("&e&l¿Cómo gestionar rarezas?"));
                        List<String> helpLore = new ArrayList<>();
                        helpLore.add(CC.translate("&7Crear: &f/crate rarity create <id> [display] [peso] [color] [announce]"));
                        helpLore.add(CC.translate("&7Editar: &f/crate rarity edit <id> [display] [peso] [color]"));
                        helpLore.add(CC.translate("&7Probabilidad: &f/crate rarity setweight <id> <peso>"));
                        helpLore.add(CC.translate("&7Borrar: &f/crate rarity delete <id>"));
                        helpMeta.setLore(helpLore);
                        help.setItemMeta(helpMeta);
                        contents.set(5, 7, ClickableItem.empty(help));

                        ItemStack back = new ItemStack(Material.REDSTONE_BLOCK);
                        ItemMeta backMeta = back.getItemMeta();
                        backMeta.setDisplayName(CC.translate("&c← Atrás"));
                        back.setItemMeta(backMeta);
                        contents.set(5, 0, ClickableItem.of(back, e -> CrateMenus.reopenLastPage(player)));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(6, 9)
                .title(CC.translate("&b&lRarezas"))
                .build();
    }

    public static void open(Player player, int page) {
        createMenu(page).open(player);
    }

    private static ItemStack createGlassPane(short color) {
        ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, color);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(CC.translate("&8"));
        glass.setItemMeta(meta);
        return glass;
    }
}
