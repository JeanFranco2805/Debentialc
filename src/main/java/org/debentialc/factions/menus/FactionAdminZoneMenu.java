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
import java.util.Map;

public class FactionAdminZoneMenu {

    private static final java.util.Map<String, Integer> lastPage = new java.util.HashMap<>();

    public static void openMainMenu(Player player, int page) {
        lastPage.put(player.getName(), page);
        createMainMenu(page).open(player);
    }

    public static SmartInventory createMainMenu(int page) {
        return SmartInventory.builder()
                .id("faction_admin_zone_" + page)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane((short) 7)));

                        ItemStack info = new ItemStack(Material.BEACON);
                        ItemMeta infoMeta = info.getItemMeta();
                        infoMeta.setDisplayName(CC.translate("&6&lAdmin Zonas"));
                        List<String> infoLore = new ArrayList<>();
                        infoLore.add(CC.translate("&7Total zonas: &f" + FactionZoneManager.getInstance().getAllZones().size()));
                        infoMeta.setLore(infoLore);
                        info.setItemMeta(infoMeta);
                        contents.set(0, 4, ClickableItem.empty(info));

                        List<FactionZone> zoneList = new ArrayList<>(FactionZoneManager.getInstance().getAllZones().values());
                        int pageSize = 7;
                        int totalPages = Math.max(1, (int) Math.ceil((double) zoneList.size() / pageSize));
                        if (page < 1) return;
                        if (page > totalPages) return;

                        int start = (page - 1) * pageSize;
                        int end = Math.min(start + pageSize, zoneList.size());

                        int slot = 10;
                        for (int i = start; i < end; i++) {
                            FactionZone zone = zoneList.get(i);
                            ItemStack item = createZoneIcon(zone);
                            final FactionZone currentZone = zone;
                            contents.set(slot / 9, slot % 9, ClickableItem.of(item, e -> {
                                if (e.isRightClick()) {
                                    FactionZoneManager.getInstance().deleteZone(currentZone.getId());
                                    player.sendMessage(CC.translate("&c✓ Zona &f" + currentZone.getId() + " &celiminada."));
                                    openMainMenu(player, page);
                                } else {
                                    FactionZoneEditMenu.open(player, currentZone.getId());
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

                        ItemStack createButton = new ItemStack(Material.EMERALD_BLOCK);
                        ItemMeta createMeta = createButton.getItemMeta();
                        createMeta.setDisplayName(CC.translate("&a&lCrear Zona"));
                        createMeta.setLore(Arrays.asList(
                                CC.translate("&7Crea una zona desde una región de WorldGuard"),
                                CC.translate("&a[CLICK PARA CREAR]")
                        ));
                        createButton.setItemMeta(createMeta);
                        contents.set(2, 1, ClickableItem.of(createButton, e -> FactionZoneInputManager.startCreate(player)));

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
                .title(CC.translate("&c&lAdmin Zonas"))
                .build();
    }

    public static void reopenLastPage(Player player) {
        Integer page = lastPage.get(player.getName());
        openMainMenu(player, page != null ? page : 1);
    }

    private static ItemStack createZoneIcon(FactionZone zone) {
        Material mat = Material.GRASS;
        try {
            mat = Material.valueOf("MAP");
        } catch (Exception ignored) {
        }
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CC.translate("&e" + zone.getDisplayName()));
        List<String> lore = new ArrayList<>();
        lore.add(CC.translate("&7ID: &f" + zone.getId()));
        lore.add(CC.translate("&7Mundo: &f" + zone.getWorld()));
        lore.add(CC.translate("&7Chunks: &f" + zone.getChunkCount()));
        lore.add(CC.translate("&7Costo power: &f" + zone.getPowerCost()));
        lore.add(CC.translate("&7Rareza: &f" + zone.getRarity()));
        lore.add("");
        lore.add(CC.translate("&a[CLICK] Editar"));
        lore.add(CC.translate("&c[CLICK DERECHO] Eliminar"));
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
