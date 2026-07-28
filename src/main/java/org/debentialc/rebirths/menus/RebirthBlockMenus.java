package org.debentialc.rebirths.menus;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.debentialc.rebirths.managers.RebirthBlockInputManager;
import org.debentialc.rebirths.managers.RebirthBlockManager;
import org.debentialc.rebirths.managers.RebirthManager;
import org.debentialc.rebirths.model.Rebirth;
import org.debentialc.rebirths.model.RebirthBlock;
import org.debentialc.service.CC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RebirthBlockMenus {

    public static SmartInventory createMainMenu() {
        return SmartInventory.builder()
                .id("rebirth_block_main")
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane((short) 11)));

                        ItemStack info = new ItemStack(Material.BOOKSHELF);
                        ItemMeta infoMeta = info.getItemMeta();
                        infoMeta.setDisplayName(CC.translate("&6&lBloques de Rebirths"));
                        List<String> infoLore = new ArrayList<>();
                        infoLore.add(CC.translate("&7Crea bloques y dentro de cada uno"));
                        infoLore.add(CC.translate("&7configura los rebirths"));
                        infoLore.add(CC.translate("&7Total bloques: &f" + RebirthBlockManager.getInstance().getAllBlocks().size()));
                        infoMeta.setLore(infoLore);
                        info.setItemMeta(infoMeta);
                        contents.set(0, 4, ClickableItem.empty(info));

                        ItemStack listButton = new ItemStack(Material.BOOK);
                        ItemMeta listMeta = listButton.getItemMeta();
                        listMeta.setDisplayName(CC.translate("&e&lVer Bloques"));
                        listMeta.setLore(Arrays.asList(
                                CC.translate("&7Lista de bloques"),
                                CC.translate("&7Entra para ver sus rebirths"),
                                "",
                                CC.translate("&e[CLICK PARA VER]")
                        ));
                        listButton.setItemMeta(listMeta);
                        contents.set(1, 3, ClickableItem.of(listButton, e -> {
                            createBlockListMenu(1).open(player);
                        }));

                        ItemStack createButton = new ItemStack(Material.EMERALD_BLOCK);
                        ItemMeta createMeta = createButton.getItemMeta();
                        createMeta.setDisplayName(CC.translate("&a&lCrear Bloque"));
                        createMeta.setLore(Arrays.asList(
                                CC.translate("&7Crea un nuevo bloque vacío"),
                                CC.translate("&7Luego agrega rebirths dentro"),
                                "",
                                CC.translate("&a[CLICK PARA CREAR]")
                        ));
                        createButton.setItemMeta(createMeta);
                        contents.set(1, 5, ClickableItem.of(createButton, e -> {
                            String nextId = RebirthBlockManager.getInstance().getNextBlockId();
                            RebirthBlock block = new RebirthBlock(nextId);
                            RebirthBlockManager.getInstance().saveBlock(block);
                            player.sendMessage(CC.translate("&a✓ Bloque &e" + nextId + " &acreado."));
                            createBlockListMenu(1).open(player);
                        }));

                        ItemStack back = new ItemStack(Material.ARROW);
                        ItemMeta backMeta = back.getItemMeta();
                        backMeta.setDisplayName(CC.translate("&b← Atrás"));
                        back.setItemMeta(backMeta);
                        contents.set(2, 4, ClickableItem.of(back, e -> RebirthAdminMenus.createMainMenu().open(player)));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(3, 9)
                .title(CC.translate("&6&lBloques de Rebirths"))
                .build();
    }

    public static SmartInventory createBlockListMenu(int page) {
        return SmartInventory.builder()
                .id("rebirth_block_list_" + page)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane((short) 11)));

                        List<RebirthBlock> blockList = new ArrayList<>(RebirthBlockManager.getInstance().getAllBlocks());
                        int pageSize = 21;
                        int totalPages = Math.max(1, (int) Math.ceil((double) blockList.size() / pageSize));
                        if (page < 1) return;
                        if (page > totalPages) return;

                        int start = (page - 1) * pageSize;
                        int end = Math.min(start + pageSize, blockList.size());

                        int row = 1;
                        int col = 1;
                        for (int i = start; i < end; i++) {
                            RebirthBlock block = blockList.get(i);
                            ItemStack item = new ItemStack(Material.BOOKSHELF);
                            ItemMeta meta = item.getItemMeta();
                            meta.setDisplayName(CC.translate("&e" + block.getName()));
                            List<String> lore = new ArrayList<>();
                            lore.add(CC.translate("&7ID: &f" + block.getId()));
                            lore.add(CC.translate("&7Rebirths: &f" + block.getRebirthIds().size()));
                            lore.add(CC.translate("&7VIP: " + (block.isVip() ? "&aSí" : "&cNo")));
                            lore.add("");
                            lore.add(CC.translate("&a[CLICK PARA ENTRAR]"));
                            meta.setLore(lore);
                            item.setItemMeta(meta);

                            String blockId = block.getId();
                            contents.set(row, col, ClickableItem.of(item, e -> {
                                createBlockMenu(blockId).open(player);
                            }));

                            col++;
                            if (col >= 8) {
                                col = 1;
                                row++;
                                if (row >= 4) break;
                            }
                        }

                        if (page > 1) {
                            ItemStack prev = new ItemStack(Material.ARROW);
                            ItemMeta prevMeta = prev.getItemMeta();
                            prevMeta.setDisplayName(CC.translate("&b← Anterior"));
                            prev.setItemMeta(prevMeta);
                            contents.set(4, 2, ClickableItem.of(prev, e -> createBlockListMenu(page - 1).open(player)));
                        }

                        ItemStack pageItem = new ItemStack(Material.PAPER);
                        ItemMeta pageMeta = pageItem.getItemMeta();
                        pageMeta.setDisplayName(CC.translate("&fPágina " + page + "/" + totalPages));
                        pageItem.setItemMeta(pageMeta);
                        contents.set(4, 4, ClickableItem.empty(pageItem));

                        if (page < totalPages) {
                            ItemStack next = new ItemStack(Material.ARROW);
                            ItemMeta nextMeta = next.getItemMeta();
                            nextMeta.setDisplayName(CC.translate("&bSiguiente →"));
                            next.setItemMeta(nextMeta);
                            contents.set(4, 6, ClickableItem.of(next, e -> createBlockListMenu(page + 1).open(player)));
                        }

                        ItemStack back = new ItemStack(Material.REDSTONE_BLOCK);
                        ItemMeta backMeta = back.getItemMeta();
                        backMeta.setDisplayName(CC.translate("&c← Atrás"));
                        back.setItemMeta(backMeta);
                        contents.set(4, 8, ClickableItem.of(back, e -> createMainMenu().open(player)));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(6, 9)
                .title(CC.translate("&6&lLista de Bloques"))
                .build();
    }

    public static SmartInventory createBlockMenu(String blockId) {
        RebirthBlock block = RebirthBlockManager.getInstance().getBlock(blockId);
        if (block == null) return null;

        return SmartInventory.builder()
                .id("rebirth_block_menu_" + blockId)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane((short) 11)));

                        ItemStack info = new ItemStack(Material.BOOKSHELF);
                        ItemMeta infoMeta = info.getItemMeta();
                        infoMeta.setDisplayName(CC.translate("&6&l" + block.getName()));
                        List<String> infoLore = new ArrayList<>();
                        infoLore.add(CC.translate("&7ID: &f" + block.getId()));
                        infoLore.add(CC.translate("&7Rebirths: &f" + block.getRebirthIds().size()));
                        infoMeta.setLore(infoLore);
                        info.setItemMeta(infoMeta);
                        contents.set(0, 4, ClickableItem.empty(info));

                        ItemStack rebirthsButton = new ItemStack(Material.BOOK);
                        ItemMeta rebirthsMeta = rebirthsButton.getItemMeta();
                        rebirthsMeta.setDisplayName(CC.translate("&e&lVer Rebirths"));
                        rebirthsMeta.setLore(Arrays.asList(
                                CC.translate("&7Rebirths dentro de este bloque"),
                                CC.translate("&7Total: &f" + block.getRebirthIds().size()),
                                "",
                                CC.translate("&e[CLICK PARA VER]")
                        ));
                        rebirthsButton.setItemMeta(rebirthsMeta);
                        contents.set(1, 1, ClickableItem.of(rebirthsButton, e -> {
                            createRebirthListMenu(blockId, 1).open(player);
                        }));

                        ItemStack createRebirthButton = new ItemStack(Material.EMERALD_BLOCK);
                        ItemMeta createRebirthMeta = createRebirthButton.getItemMeta();
                        createRebirthMeta.setDisplayName(CC.translate("&a&lCrear Rebirth"));
                        createRebirthMeta.setLore(Arrays.asList(
                                CC.translate("&7Agrega un rebirth a este bloque"),
                                "",
                                CC.translate("&a[CLICK PARA CREAR]")
                        ));
                        createRebirthButton.setItemMeta(createRebirthMeta);
                        contents.set(1, 3, ClickableItem.of(createRebirthButton, e -> {
                            Rebirth rebirth = RebirthManager.getInstance().createRebirthInBlock(blockId);
                            if (rebirth != null) {
                                player.sendMessage(CC.translate("&a✓ Rebirth &e" + rebirth.getId() + " &acreado en el bloque."));
                                createRebirthListMenu(blockId, 1).open(player);
                            }
                        }));

                        ItemStack nameButton = new ItemStack(Material.NAME_TAG);
                        ItemMeta nameMeta = nameButton.getItemMeta();
                        nameMeta.setDisplayName(CC.translate("&e&lCambiar Nombre"));
                        nameMeta.setLore(Arrays.asList(
                                CC.translate("&7Actual: &r" + block.getName()),
                                "",
                                CC.translate("&a[CLICK PARA EDITAR]")
                        ));
                        nameButton.setItemMeta(nameMeta);
                        contents.set(1, 5, ClickableItem.of(nameButton, e -> {
                            RebirthBlockInputManager.startInput(player, blockId, RebirthBlockInputManager.InputType.BLOCK_NAME, () -> createBlockMenu(blockId).open(player));
                        }));

                        ItemStack permissionButton = new ItemStack(Material.TRIPWIRE_HOOK);
                        ItemMeta permissionMeta = permissionButton.getItemMeta();
                        permissionMeta.setDisplayName(CC.translate("&c&lPermiso de Acceso"));
                        List<String> permissionLore = new ArrayList<>();
                        permissionLore.add(CC.translate("&7Permiso necesario para entrar"));
                        permissionLore.add(CC.translate("&7a este bloque en /rebirth"));
                        permissionLore.add("");
                        if (block.getRequiredPermission() != null && !block.getRequiredPermission().isEmpty()) {
                            permissionLore.add(CC.translate("&7Actual: &f" + block.getRequiredPermission()));
                        } else {
                            permissionLore.add(CC.translate("&cSin permiso (libre)"));
                        }
                        permissionLore.add("");
                        permissionLore.add(CC.translate("&a[CLICK PARA CONFIGURAR]"));
                        permissionMeta.setLore(permissionLore);
                        permissionButton.setItemMeta(permissionMeta);
                        contents.set(2, 1, ClickableItem.of(permissionButton, e -> {
                            RebirthBlockInputManager.startInput(player, blockId, RebirthBlockInputManager.InputType.BLOCK_PERMISSION, () -> createBlockMenu(blockId).open(player));
                        }));

                        ItemStack vipButton = new ItemStack(Material.DIAMOND);
                        ItemMeta vipMeta = vipButton.getItemMeta();
                        vipMeta.setDisplayName(CC.translate("&b&lVIP"));
                        List<String> vipLore = new ArrayList<>();
                        vipLore.add(CC.translate("&7Estado: " + (block.isVip() ? "&aACTIVADO" : "&cDESACTIVADO")));
                        vipLore.add("");
                        vipLore.add(CC.translate("&7Los bloques VIP solo se muestran"));
                        vipLore.add(CC.translate("&7en el menú VIP de /rebirth"));
                        vipLore.add("");
                        vipLore.add(CC.translate("&a[CLICK PARA CAMBIAR]"));
                        vipMeta.setLore(vipLore);
                        vipButton.setItemMeta(vipMeta);
                        contents.set(2, 2, ClickableItem.of(vipButton, e -> {
                            block.setVip(!block.isVip());
                            RebirthBlockManager.getInstance().saveBlock(block);
                            player.sendMessage(CC.translate("&a✓ Bloque &e" + block.getName() + " &aahora " + (block.isVip() ? "&aVIP" : "&cNO VIP")));
                            createBlockMenu(blockId).open(player);
                        }));

                        ItemStack deleteButton = new ItemStack(Material.ANVIL);
                        ItemMeta deleteMeta = deleteButton.getItemMeta();
                        deleteMeta.setDisplayName(CC.translate("&c&lEliminar Bloque"));
                        deleteMeta.setLore(Arrays.asList(
                                CC.translate("&7Elimina este bloque"),
                                CC.translate("&7Los rebirths no se eliminan"),
                                "",
                                CC.translate("&c[CLICK PARA ELIMINAR]")
                        ));
                        deleteButton.setItemMeta(deleteMeta);
                        contents.set(2, 4, ClickableItem.of(deleteButton, e -> {
                            createDeleteConfirmMenu(blockId).open(player);
                        }));

                        ItemStack back = new ItemStack(Material.ARROW);
                        ItemMeta backMeta = back.getItemMeta();
                        backMeta.setDisplayName(CC.translate("&b← Atrás"));
                        back.setItemMeta(backMeta);
                        contents.set(2, 8, ClickableItem.of(back, e -> {
                            if (block.isVip()) {
                                RebirthPlayerMenus.createVipBlocksMenu(player).open(player);
                            } else {
                                createBlockListMenu(1).open(player);
                            }
                        }));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(3, 9)
                .title(CC.translate("&6&lBloque " + blockId))
                .build();
    }

    public static SmartInventory createRebirthListMenu(String blockId, int page) {
        RebirthBlock block = RebirthBlockManager.getInstance().getBlock(blockId);
        if (block == null) return null;

        return SmartInventory.builder()
                .id("rebirth_block_rebirths_" + blockId + "_" + page)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane((short) 11)));

                        List<Rebirth> rebirthList = RebirthManager.getInstance().getRebirthsInBlock(blockId);
                        int pageSize = 21;
                        int totalPages = Math.max(1, (int) Math.ceil((double) rebirthList.size() / pageSize));
                        if (page < 1) return;
                        if (page > totalPages) return;

                        int start = (page - 1) * pageSize;
                        int end = Math.min(start + pageSize, rebirthList.size());

                        int row = 1;
                        int col = 1;
                        for (int i = start; i < end; i++) {
                            Rebirth rebirth = rebirthList.get(i);
                            ItemStack item = new ItemStack(Material.EXP_BOTTLE);
                            ItemMeta meta = item.getItemMeta();
                            meta.setDisplayName(CC.translate("&e" + rebirth.getDisplayName()));
                            List<String> lore = new ArrayList<>();
                            lore.add(CC.translate("&7ID: &f" + rebirth.getId()));
                            lore.add(CC.translate("&7Nivel requerido: &f" + rebirth.getRequiredLevel()));
                            lore.add(CC.translate("&7Bonus TPs: &f" + rebirth.getTpBonusPercent() + "%"));
                            lore.add(CC.translate("&7Zonas: &f" + rebirth.getAllowedRegions().size()));
                            lore.add(CC.translate("&7Items: &f" + rebirth.getRewardItems().size()));
                            lore.add(CC.translate("&7Comandos: &f" + rebirth.getRewardCommands().size()));
                            lore.add("");
                            lore.add(CC.translate("&a[CLICK PARA EDITAR]"));
                            meta.setLore(lore);
                            item.setItemMeta(meta);

                            contents.set(row, col, ClickableItem.of(item, e -> {
                                RebirthAdminMenus.createEditRebirthMenu(rebirth.getId(), () -> createRebirthListMenu(blockId, page).open(player)).open(player);
                            }));

                            col++;
                            if (col >= 8) {
                                col = 1;
                                row++;
                                if (row >= 4) break;
                            }
                        }

                        if (page > 1) {
                            ItemStack prev = new ItemStack(Material.ARROW);
                            ItemMeta prevMeta = prev.getItemMeta();
                            prevMeta.setDisplayName(CC.translate("&b← Anterior"));
                            prev.setItemMeta(prevMeta);
                            contents.set(4, 2, ClickableItem.of(prev, e -> createRebirthListMenu(blockId, page - 1).open(player)));
                        }

                        ItemStack pageItem = new ItemStack(Material.PAPER);
                        ItemMeta pageMeta = pageItem.getItemMeta();
                        pageMeta.setDisplayName(CC.translate("&fPágina " + page + "/" + totalPages));
                        pageItem.setItemMeta(pageMeta);
                        contents.set(4, 4, ClickableItem.empty(pageItem));

                        if (page < totalPages) {
                            ItemStack next = new ItemStack(Material.ARROW);
                            ItemMeta nextMeta = next.getItemMeta();
                            nextMeta.setDisplayName(CC.translate("&bSiguiente →"));
                            next.setItemMeta(nextMeta);
                            contents.set(4, 6, ClickableItem.of(next, e -> createRebirthListMenu(blockId, page + 1).open(player)));
                        }

                        ItemStack back = new ItemStack(Material.REDSTONE_BLOCK);
                        ItemMeta backMeta = back.getItemMeta();
                        backMeta.setDisplayName(CC.translate("&c← Atrás"));
                        back.setItemMeta(backMeta);
                        contents.set(4, 8, ClickableItem.of(back, e -> createBlockMenu(blockId).open(player)));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(6, 9)
                .title(CC.translate("&6&lRebirths del Bloque " + blockId))
                .build();
    }

    public static SmartInventory createDeleteConfirmMenu(String blockId) {
        RebirthBlock block = RebirthBlockManager.getInstance().getBlock(blockId);
        if (block == null) return null;

        return SmartInventory.builder()
                .id("rebirth_block_delete_" + blockId)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane((short) 14)));

                        ItemStack confirm = new ItemStack(Material.PAPER);
                        ItemMeta confirmMeta = confirm.getItemMeta();
                        confirmMeta.setDisplayName(CC.translate("&c&l¿Eliminar " + block.getName() + "?"));
                        confirmMeta.setLore(Arrays.asList(
                                CC.translate("&7Se eliminará el bloque &f" + blockId),
                                CC.translate("&7Los rebirths quedarán sin bloque")
                        ));
                        confirm.setItemMeta(confirmMeta);
                        contents.set(0, 4, ClickableItem.empty(confirm));

                        ItemStack yes = new ItemStack(Material.EMERALD_BLOCK);
                        ItemMeta yesMeta = yes.getItemMeta();
                        yesMeta.setDisplayName(CC.translate("&a&lSÍ, ELIMINAR"));
                        yes.setItemMeta(yesMeta);
                        contents.set(1, 3, ClickableItem.of(yes, e -> {
                            RebirthBlockManager.getInstance().deleteBlock(blockId);
                            player.sendMessage(CC.translate("&c✗ Bloque " + blockId + " eliminado."));
                            createBlockListMenu(1).open(player);
                        }));

                        ItemStack no = new ItemStack(Material.REDSTONE_BLOCK);
                        ItemMeta noMeta = no.getItemMeta();
                        noMeta.setDisplayName(CC.translate("&c&lCANCELAR"));
                        no.setItemMeta(noMeta);
                        contents.set(1, 5, ClickableItem.of(no, e -> createBlockMenu(blockId).open(player)));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(2, 9)
                .title(CC.translate("&c&lConfirmar Eliminación"))
                .build();
    }

    private static ItemStack createGlassPane(short color) {
        ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, color);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(CC.translate("&8"));
        glass.setItemMeta(meta);
        return glass;
    }
}
