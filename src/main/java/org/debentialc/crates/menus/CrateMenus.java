package org.debentialc.crates.menus;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.debentialc.crates.events.CrateEditInventory;
import org.debentialc.crates.managers.CrateManager;
import org.debentialc.crates.models.Crate;
import org.debentialc.service.CC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CrateMenus {

    private static final Map<String, Integer> lastPage = new java.util.HashMap<>();

    public static void openMainMenu(Player player, int page) {
        lastPage.put(player.getName(), page);
        createMainMenu(page).open(player);
    }

    public static SmartInventory createMainMenu(int page) {
        return SmartInventory.builder()
                .id("crate_main_" + page)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane((short) 7)));

                        ItemStack info = new ItemStack(Material.ENDER_CHEST);
                        ItemMeta infoMeta = info.getItemMeta();
                        infoMeta.setDisplayName(CC.translate("&6&lCrates"));
                        List<String> infoLore = new ArrayList<>();
                        infoLore.add(CC.translate("&7Total crates: &f" + CrateManager.getInstance().getAllCrates().size()));
                        infoMeta.setLore(infoLore);
                        info.setItemMeta(infoMeta);
                        contents.set(0, 4, ClickableItem.empty(info));

                        List<Crate> crateList = new ArrayList<>(CrateManager.getInstance().getAllCrates().values());
                        int pageSize = 7;
                        int totalPages = Math.max(1, (int) Math.ceil((double) crateList.size() / pageSize));
                        if (page < 1) return;
                        if (page > totalPages) return;

                        int start = (page - 1) * pageSize;
                        int end = Math.min(start + pageSize, crateList.size());

                        int slot = 10;
                        for (int i = start; i < end; i++) {
                            Crate crate = crateList.get(i);
                            ItemStack item = createCrateIcon(crate);
                            final int currentSlot = slot;
                            final Crate currentCrate = crate;
                            contents.set(currentSlot / 9, currentSlot % 9, ClickableItem.of(item, e -> {
                                if (e.isRightClick()) {
                                    CrateManager.getInstance().deleteCrate(currentCrate.getId());
                                    player.sendMessage(CC.translate("&c✓ Crate " + currentCrate.getId() + " eliminada."));
                                    openMainMenu(player, page);
                                } else {
                                    CrateEditInventory.open(player, currentCrate);
                                }
                            }));
                            slot++;
                        }

                        if (page > 1) {
                            ItemStack prev = new ItemStack(Material.ARROW);
                            ItemMeta prevMeta = prev.getItemMeta();
                            prevMeta.setDisplayName(CC.translate("&b← Anterior"));
                            prev.setItemMeta(prevMeta);
                            contents.set(5, 2, ClickableItem.of(prev, e -> openMainMenu(player, page - 1)));
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
                            contents.set(5, 6, ClickableItem.of(next, e -> openMainMenu(player, page + 1)));
                        }

                        ItemStack rarityButton = new ItemStack(Material.DIAMOND);
                        ItemMeta rarityMeta = rarityButton.getItemMeta();
                        rarityMeta.setDisplayName(CC.translate("&b&lRarezas"));
                        rarityMeta.setLore(Arrays.asList(CC.translate("&7Click para ver rarezas")));
                        rarityButton.setItemMeta(rarityMeta);
                        contents.set(2, 1, ClickableItem.of(rarityButton, e -> CrateRarityMenu.open(player, 1)));

                        ItemStack closeButton = new ItemStack(Material.REDSTONE_BLOCK);
                        ItemMeta closeMeta = closeButton.getItemMeta();
                        closeMeta.setDisplayName(CC.translate("&c&lCerrar"));
                        closeButton.setItemMeta(closeMeta);
                        contents.set(5, 8, ClickableItem.of(closeButton, e -> player.closeInventory()));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(6, 9)
                .title(CC.translate("&c&lAdmin Crates"))
                .build();
    }

    public static void reopenLastPage(Player player) {
        Integer page = lastPage.get(player.getName());
        openMainMenu(player, page != null ? page : 1);
    }

    private static ItemStack createCrateIcon(Crate crate) {
        Material material = Material.CHEST;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CC.translate("&e" + crate.getDisplayName()));
        List<String> lore = new ArrayList<>();
        lore.add(CC.translate("&7ID: &f" + crate.getId()));
        lore.add(CC.translate("&7Items: &f" + crate.getItems().size()));
        if (crate.hasLocation()) {
            lore.add(CC.translate("&7Ubicación: &f" + crate.getWorld() + " " + crate.getX() + "," + crate.getY() + "," + crate.getZ()));
        }
        lore.add("");
        lore.add(CC.translate("&a[CLICK IZQUIERDO] Editar items"));
        lore.add(CC.translate("&c[CLICK DERECHO] Eliminar crate"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createGlassPane(short color) {
        ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, color);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(CC.translate("&8"));
        glass.setItemMeta(meta);
        return glass;
    }
}
