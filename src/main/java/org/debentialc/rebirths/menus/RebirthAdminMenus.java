package org.debentialc.rebirths.menus;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.InventoryListener;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.debentialc.rebirths.managers.RebirthBlockManager;
import org.debentialc.rebirths.managers.RebirthInputManager;
import org.debentialc.rebirths.managers.RebirthManager;
import org.debentialc.rebirths.model.Rebirth;
import org.debentialc.rebirths.model.RebirthBlock;
import org.debentialc.service.CC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RebirthAdminMenus {

    private static final Map<UUID, Integer> playersEditingItems = new HashMap<>();
    private static final Map<UUID, Runnable> playersItemsBackAction = new HashMap<>();

    public static Integer getEditingRebirthId(Player player) {
        return playersEditingItems.get(player.getUniqueId());
    }

    public static void setEditingItems(Player player, int rebirthId) {
        playersEditingItems.put(player.getUniqueId(), rebirthId);
    }

    public static void removeEditingItems(Player player) {
        playersEditingItems.remove(player.getUniqueId());
    }

    public static Runnable getBackAction(Player player) {
        return playersItemsBackAction.get(player.getUniqueId());
    }

    public static void setItemsBackAction(Player player, Runnable backAction) {
        playersItemsBackAction.put(player.getUniqueId(), backAction);
    }

    public static void removeItemsBackAction(Player player) {
        playersItemsBackAction.remove(player.getUniqueId());
    }

    public static SmartInventory createMainMenu() {
        return SmartInventory.builder()
                .id("rebirth_admin_main")
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane((short) 7)));

                        ItemStack infoItem = new ItemStack(Material.NETHER_STAR);
                        ItemMeta infoMeta = infoItem.getItemMeta();
                        infoMeta.setDisplayName(CC.translate("&6&lConfiguración de Rebirths"));
                        List<String> infoLore = new ArrayList<>();
                        infoLore.add(CC.translate("&7Gestiona los rebirths del servidor"));
                        infoLore.add(CC.translate("&7Total rebirths: &f" + RebirthManager.getInstance().getAllRebirths().size()));
                        infoLore.add(CC.translate("&7Total bloques: &f" + RebirthBlockManager.getInstance().getAllBlocks().size()));
                        infoMeta.setLore(infoLore);
                        infoItem.setItemMeta(infoMeta);
                        contents.set(0, 4, ClickableItem.empty(infoItem));

                        ItemStack listButton = new ItemStack(Material.BOOK);
                        ItemMeta listMeta = listButton.getItemMeta();
                        listMeta.setDisplayName(CC.translate("&e&lVer Rebirths"));
                        listMeta.setLore(Arrays.asList(
                                CC.translate("&7Lista de bloques de rebirths"),
                                CC.translate("&7Dentro de cada bloque verás sus rebirths"),
                                CC.translate("&7Edita requisitos, bonus y zonas"),
                                "",
                                CC.translate("&e[CLICK PARA VER]")
                        ));
                        listButton.setItemMeta(listMeta);
                        contents.set(1, 3, ClickableItem.of(listButton, e -> {
                            RebirthBlockMenus.createBlockListMenu(1).open(player);
                        }));

                        ItemStack createButton = new ItemStack(Material.EMERALD_BLOCK);
                        ItemMeta createMeta = createButton.getItemMeta();
                        createMeta.setDisplayName(CC.translate("&a&lCrear Rebirth"));
                        createMeta.setLore(Arrays.asList(
                                CC.translate("&7Los rebirths se crean dentro de un bloque."),
                                CC.translate("&7Gestiona los bloques primero."),
                                "",
                                CC.translate("&a[CLICK PARA IR A BLOQUES]")
                        ));
                        createButton.setItemMeta(createMeta);
                        contents.set(1, 5, ClickableItem.of(createButton, e -> {
                            RebirthBlockMenus.createMainMenu().open(player);
                        }));

                        ItemStack blocksButton = new ItemStack(Material.BOOKSHELF);
                        ItemMeta blocksMeta = blocksButton.getItemMeta();
                        blocksMeta.setDisplayName(CC.translate("&6&lBloques de Rebirths"));
                        blocksMeta.setLore(Arrays.asList(
                                CC.translate("&7Crea bloques de rebirths"),
                                CC.translate("&7Cada bloque contiene sus rebirths"),
                                CC.translate("&7y define si guardan nivel"),
                                "",
                                CC.translate("&e[CLICK PARA GESTIONAR]")
                        ));
                        blocksButton.setItemMeta(blocksMeta);
                        contents.set(2, 3, ClickableItem.of(blocksButton, e -> {
                            RebirthBlockMenus.createMainMenu().open(player);
                        }));

                        ItemStack closeButton = new ItemStack(Material.REDSTONE_BLOCK);
                        ItemMeta closeMeta = closeButton.getItemMeta();
                        closeMeta.setDisplayName(CC.translate("&c&lCerrar"));
                        closeButton.setItemMeta(closeMeta);
                        contents.set(2, 4, ClickableItem.of(closeButton, e -> player.closeInventory()));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(3, 9)
                .title(CC.translate("&c&lAdmin Rebirths"))
                .build();
    }

    public static SmartInventory createRebirthListMenu(int page) {
        return SmartInventory.builder()
                .id("rebirth_admin_list_" + page)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane((short) 7)));

                        List<Rebirth> rebirthList = new ArrayList<>(RebirthManager.getInstance().getAllRebirths());
                        int pageSize = 21;
                        int totalPages = Math.max(1, (int) Math.ceil((double) rebirthList.size() / pageSize));
                        if (page < 1) return;
                        if (page > totalPages && totalPages > 0) return;

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
                            RebirthBlock block = RebirthBlockManager.getInstance().getBlockForRebirth(rebirth.getId());
                            if (block != null) {
                                lore.add(CC.translate("&7Bloque: &f" + block.getName()));
                            } else {
                                lore.add(CC.translate("&7Bloque: &7Ninguno"));
                            }
                            lore.add("");
                            lore.add(CC.translate("&a[CLICK PARA EDITAR]"));
                            meta.setLore(lore);
                            item.setItemMeta(meta);

                            contents.set(row, col, ClickableItem.of(item, e -> {
                                createEditRebirthMenu(rebirth.getId(), () -> createRebirthListMenu(page).open(player)).open(player);
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
                            contents.set(4, 2, ClickableItem.of(prev, e -> createRebirthListMenu(page - 1).open(player)));
                        }

                        ItemStack pageItem = new ItemStack(Material.PAPER);
                        ItemMeta pageMeta = pageItem.getItemMeta();
                        pageMeta.setDisplayName(CC.translate("&fPágina " + page + "/" + Math.max(1, totalPages)));
                        pageItem.setItemMeta(pageMeta);
                        contents.set(4, 4, ClickableItem.empty(pageItem));

                        if (page < totalPages) {
                            ItemStack next = new ItemStack(Material.ARROW);
                            ItemMeta nextMeta = next.getItemMeta();
                            nextMeta.setDisplayName(CC.translate("&bSiguiente →"));
                            next.setItemMeta(nextMeta);
                            contents.set(4, 6, ClickableItem.of(next, e -> createRebirthListMenu(page + 1).open(player)));
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
                .title(CC.translate("&c&lLista de Rebirths"))
                .build();
    }

    public static SmartInventory createEditRebirthMenu(int rebirthId) {
        return createEditRebirthMenu(rebirthId, null);
    }

    public static SmartInventory createEditRebirthMenu(int rebirthId, Runnable backAction) {
        Rebirth rebirth = RebirthManager.getInstance().getRebirth(rebirthId);
        if (rebirth == null) return null;

        return SmartInventory.builder()
                .id("rebirth_admin_edit_" + rebirthId)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane((short) 7)));

                        RebirthBlock assignedBlock = RebirthBlockManager.getInstance().getBlockForRebirth(rebirth.getId());

                        ItemStack info = new ItemStack(Material.NETHER_STAR);
                        ItemMeta infoMeta = info.getItemMeta();
                        infoMeta.setDisplayName(CC.translate("&6&l" + rebirth.getDisplayName()));
                        List<String> infoLore = new ArrayList<>();
                        infoLore.add(CC.translate("&7ID: &f" + rebirth.getId()));
                        infoLore.add(CC.translate("&7Nivel requerido: &f" + rebirth.getRequiredLevel()));
                        infoLore.add(CC.translate("&7Bonus TPs: &f" + rebirth.getTpBonusPercent() + "%"));
                        infoLore.add(CC.translate("&7Multiplicador: &fx" + String.format("%.2f", rebirth.getMultiplier())));
                        if (assignedBlock != null) {
                            infoLore.add(CC.translate("&7Bloque: &f" + assignedBlock.getName()));
                            infoLore.add(CC.translate("&7Guarda nivel: " + (assignedBlock.isSaveLevel() ? "&aSí" : "&cNo")));
                        } else {
                            infoLore.add(CC.translate("&7Bloque: &7Ninguno (reinicia con jrmcrei)"));
                        }
                        infoMeta.setLore(infoLore);
                        info.setItemMeta(infoMeta);
                        contents.set(0, 4, ClickableItem.empty(info));

                        ItemStack nameButton = new ItemStack(Material.NAME_TAG);
                        ItemMeta nameMeta = nameButton.getItemMeta();
                        nameMeta.setDisplayName(CC.translate("&e&lCambiar Nombre"));
                        nameMeta.setLore(Arrays.asList(
                                CC.translate("&7Actual: &r" + rebirth.getDisplayName()),
                                "",
                                CC.translate("&a[CLICK PARA EDITAR]")
                        ));
                        Runnable reopenEditMenu = () -> createEditRebirthMenu(rebirthId, backAction).open(player);

                        nameButton.setItemMeta(nameMeta);
                        contents.set(1, 1, ClickableItem.of(nameButton, e -> {
                            RebirthInputManager.startInput(player, rebirthId, RebirthInputManager.InputType.DISPLAY_NAME, reopenEditMenu);
                        }));

                        ItemStack levelButton = new ItemStack(Material.DIAMOND_SWORD);
                        ItemMeta levelMeta = levelButton.getItemMeta();
                        levelMeta.setDisplayName(CC.translate("&b&lNivel Requerido"));
                        levelMeta.setLore(Arrays.asList(
                                CC.translate("&7Actual: &f" + rebirth.getRequiredLevel()),
                                "",
                                CC.translate("&a[CLICK PARA EDITAR]")
                        ));
                        levelButton.setItemMeta(levelMeta);
                        contents.set(1, 3, ClickableItem.of(levelButton, e -> {
                            RebirthInputManager.startInput(player, rebirthId, RebirthInputManager.InputType.REQUIRED_LEVEL, reopenEditMenu);
                        }));

                        ItemStack tpButton = new ItemStack(Material.GOLD_NUGGET);
                        ItemMeta tpMeta = tpButton.getItemMeta();
                        tpMeta.setDisplayName(CC.translate("&6&lBonus TPs"));
                        tpMeta.setLore(Arrays.asList(
                                CC.translate("&7Actual: &f" + rebirth.getTpBonusPercent() + "%"),
                                "",
                                CC.translate("&a[CLICK PARA EDITAR]")
                        ));
                        tpButton.setItemMeta(tpMeta);
                        contents.set(1, 5, ClickableItem.of(tpButton, e -> {
                            RebirthInputManager.startInput(player, rebirthId, RebirthInputManager.InputType.TP_BONUS_PERCENT, reopenEditMenu);
                        }));

                        ItemStack zonesButton = new ItemStack(Material.GRASS);
                        ItemMeta zonesMeta = zonesButton.getItemMeta();
                        zonesMeta.setDisplayName(CC.translate("&a&lZonas"));
                        zonesMeta.setLore(Arrays.asList(
                                CC.translate("&7Regiones de WorldGuard permitidas"),
                                CC.translate("&7Total: &f" + rebirth.getAllowedRegions().size()),
                                "",
                                CC.translate("&a[CLICK PARA GESTIONAR]")
                        ));
                        zonesButton.setItemMeta(zonesMeta);
                        contents.set(1, 7, ClickableItem.of(zonesButton, e -> {
                            createZonesMenu(rebirthId, reopenEditMenu).open(player);
                        }));

                        ItemStack itemsButton = new ItemStack(Material.CHEST);
                        ItemMeta itemsMeta = itemsButton.getItemMeta();
                        itemsMeta.setDisplayName(CC.translate("&d&lItems de Recompensa"));
                        List<String> itemsLore = new ArrayList<>();
                        itemsLore.add(CC.translate("&7Items que recibe el jugador"));
                        itemsLore.add(CC.translate("&7al desbloquear este rebirth"));
                        itemsLore.add(CC.translate("&7Total: &f" + rebirth.getRewardItems().size()));
                        itemsLore.add("");
                        itemsLore.add(CC.translate("&a[CLICK PARA GESTIONAR]"));
                        itemsMeta.setLore(itemsLore);
                        itemsButton.setItemMeta(itemsMeta);
                        contents.set(2, 1, ClickableItem.of(itemsButton, e -> {
                            createItemsMenu(rebirthId, reopenEditMenu).open(player);
                        }));

                        ItemStack commandsButton = new ItemStack(Material.COMMAND);
                        ItemMeta commandsMeta = commandsButton.getItemMeta();
                        commandsMeta.setDisplayName(CC.translate("&5&lComandos"));
                        List<String> commandsLore = new ArrayList<>();
                        commandsLore.add(CC.translate("&7Comandos ejecutados al desbloquear"));
                        commandsLore.add(CC.translate("&7Usa &f%player% &7para el nombre"));
                        commandsLore.add(CC.translate("&7Total: &f" + rebirth.getRewardCommands().size()));
                        commandsLore.add("");
                        commandsLore.add(CC.translate("&a[CLICK PARA GESTIONAR]"));
                        commandsMeta.setLore(commandsLore);
                        commandsButton.setItemMeta(commandsMeta);
                        contents.set(2, 2, ClickableItem.of(commandsButton, e -> {
                            createCommandsMenu(rebirthId, reopenEditMenu).open(player);
                        }));

                        ItemStack deleteButton = new ItemStack(Material.ANVIL);
                        ItemMeta deleteMeta = deleteButton.getItemMeta();
                        deleteMeta.setDisplayName(CC.translate("&c&lEliminar Rebirth"));
                        deleteMeta.setLore(Arrays.asList(
                                CC.translate("&7Elimina este rebirth permanentemente"),
                                "",
                                CC.translate("&c[CLICK PARA ELIMINAR]")
                        ));
                        deleteButton.setItemMeta(deleteMeta);
                        contents.set(2, 4, ClickableItem.of(deleteButton, e -> {
                            createDeleteConfirmMenu(rebirthId).open(player);
                        }));

                        ItemStack back = new ItemStack(Material.ARROW);
                        ItemMeta backMeta = back.getItemMeta();
                        backMeta.setDisplayName(CC.translate("&b← Atrás"));
                        back.setItemMeta(backMeta);
                        contents.set(2, 8, ClickableItem.of(back, e -> {
                            if (backAction != null) {
                                backAction.run();
                            } else {
                                createRebirthListMenu(1).open(player);
                            }
                        }));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(3, 9)
                .title(CC.translate("&c&lEditar Rebirth " + rebirthId))
                .build();
    }

    public static SmartInventory createZonesMenu(int rebirthId) {
        return createZonesMenu(rebirthId, null);
    }

    public static SmartInventory createZonesMenu(int rebirthId, Runnable backAction) {
        Rebirth rebirth = RebirthManager.getInstance().getRebirth(rebirthId);
        if (rebirth == null) return null;

        return SmartInventory.builder()
                .id("rebirth_admin_zones_" + rebirthId)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane((short) 7)));

                        Runnable reopenZonesMenu = () -> createZonesMenu(rebirthId, backAction).open(player);

                        ItemStack info = new ItemStack(Material.GRASS);
                        ItemMeta infoMeta = info.getItemMeta();
                        infoMeta.setDisplayName(CC.translate("&a&lZonas de &e" + rebirth.getDisplayName()));
                        List<String> infoLore = new ArrayList<>();
                        infoLore.add(CC.translate("&7Regiones permitidas para este rebirth:"));
                        if (rebirth.getAllowedRegions().isEmpty()) {
                            infoLore.add(CC.translate("&7Ninguna configurada"));
                        } else {
                            for (String region : rebirth.getAllowedRegions()) {
                                infoLore.add(CC.translate("&f• " + region));
                            }
                        }
                        infoMeta.setLore(infoLore);
                        info.setItemMeta(infoMeta);
                        contents.set(0, 4, ClickableItem.empty(info));

                        ItemStack addButton = new ItemStack(Material.EMERALD_BLOCK);
                        ItemMeta addMeta = addButton.getItemMeta();
                        addMeta.setDisplayName(CC.translate("&a&lAgregar Región"));
                        addMeta.setLore(Arrays.asList(
                                CC.translate("&7Añade una región de WorldGuard"),
                                "",
                                CC.translate("&a[CLICK PARA AGREGAR]")
                        ));
                        addButton.setItemMeta(addMeta);
                        contents.set(1, 2, ClickableItem.of(addButton, e -> {
                            RebirthInputManager.startInput(player, rebirthId, RebirthInputManager.InputType.ADD_REGION, reopenZonesMenu);
                        }));

                        ItemStack removeButton = new ItemStack(Material.REDSTONE_BLOCK);
                        ItemMeta removeMeta = removeButton.getItemMeta();
                        removeMeta.setDisplayName(CC.translate("&c&lRemover Región"));
                        removeMeta.setLore(Arrays.asList(
                                CC.translate("&7Remueve una región existente"),
                                "",
                                CC.translate("&c[CLICK PARA REMOVER]")
                        ));
                        removeButton.setItemMeta(removeMeta);
                        contents.set(1, 6, ClickableItem.of(removeButton, e -> {
                            RebirthInputManager.startInput(player, rebirthId, RebirthInputManager.InputType.REMOVE_REGION, reopenZonesMenu);
                        }));

                        ItemStack back = new ItemStack(Material.ARROW);
                        ItemMeta backMeta = back.getItemMeta();
                        backMeta.setDisplayName(CC.translate("&b← Atrás"));
                        back.setItemMeta(backMeta);
                        contents.set(2, 4, ClickableItem.of(back, e -> {
                            if (backAction != null) {
                                backAction.run();
                            } else {
                                createEditRebirthMenu(rebirthId).open(player);
                            }
                        }));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(3, 9)
                .title(CC.translate("&a&lZonas Rebirth " + rebirthId))
                .build();
    }

    public static SmartInventory createDeleteConfirmMenu(int rebirthId) {
        Rebirth rebirth = RebirthManager.getInstance().getRebirth(rebirthId);
        if (rebirth == null) return null;

        return SmartInventory.builder()
                .id("rebirth_admin_delete_" + rebirthId)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane((short) 14)));

                        ItemStack confirm = new ItemStack(Material.PAPER);
                        ItemMeta confirmMeta = confirm.getItemMeta();
                        confirmMeta.setDisplayName(CC.translate("&c&l¿Eliminar " + rebirth.getDisplayName() + "?"));
                        confirmMeta.setLore(Arrays.asList(
                                CC.translate("&7Esta acción no se puede deshacer"),
                                CC.translate("&7Se eliminará el rebirth &f" + rebirthId)
                        ));
                        confirm.setItemMeta(confirmMeta);
                        contents.set(0, 4, ClickableItem.empty(confirm));

                        ItemStack yes = new ItemStack(Material.EMERALD_BLOCK);
                        ItemMeta yesMeta = yes.getItemMeta();
                        yesMeta.setDisplayName(CC.translate("&a&lSÍ, ELIMINAR"));
                        yes.setItemMeta(yesMeta);
                        contents.set(1, 3, ClickableItem.of(yes, e -> {
                            RebirthManager.getInstance().deleteRebirth(rebirthId);
                            player.sendMessage(CC.translate("&c✗ Rebirth " + rebirthId + " eliminado."));
                            createRebirthListMenu(1).open(player);
                        }));

                        ItemStack no = new ItemStack(Material.REDSTONE_BLOCK);
                        ItemMeta noMeta = no.getItemMeta();
                        noMeta.setDisplayName(CC.translate("&c&lCANCELAR"));
                        no.setItemMeta(noMeta);
                        contents.set(1, 5, ClickableItem.of(no, e -> createEditRebirthMenu(rebirthId).open(player)));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(2, 9)
                .title(CC.translate("&c&lConfirmar Eliminación"))
                .build();
    }

    public static SmartInventory createItemsMenu(int rebirthId) {
        return createItemsMenu(rebirthId, null);
    }

    public static SmartInventory createItemsMenu(int rebirthId, Runnable backAction) {
        Rebirth rebirth = RebirthManager.getInstance().getRebirth(rebirthId);
        if (rebirth == null) return null;

        return SmartInventory.builder()
                .id("rebirth_admin_items_" + rebirthId)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane((short) 10)));

                        setEditingItems(player, rebirthId);
                        setItemsBackAction(player, backAction);

                        ItemStack info = new ItemStack(Material.CHEST);
                        ItemMeta infoMeta = info.getItemMeta();
                        infoMeta.setDisplayName(CC.translate("&d&lItems de &e" + rebirth.getDisplayName()));
                        List<String> infoLore = new ArrayList<>();
                        infoLore.add(CC.translate("&7Items actuales: &f" + rebirth.getRewardItems().size()));
                        infoLore.add("");
                        infoLore.add(CC.translate("&7Arrastra items desde tu inventario"));
                        infoLore.add(CC.translate("&7a cualquier slot vacío de este menú."));
                        infoMeta.setLore(infoLore);
                        info.setItemMeta(infoMeta);
                        contents.set(0, 4, ClickableItem.empty(info));

                        List<org.bukkit.inventory.ItemStack> items = rebirth.getRewardItems();
                        int slot = 10;
                        for (int i = 0; i < items.size() && slot < 35; i++) {
                            org.bukkit.inventory.ItemStack item = items.get(i);
                            ItemStack display = item.clone();
                            ItemMeta meta = display.getItemMeta();
                            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                            lore.add("");
                            lore.add(CC.translate("&c[CLICK PARA ELIMINAR]"));
                            meta.setLore(lore);
                            display.setItemMeta(meta);

                            final int index = i;
                            contents.set(slot / 9, slot % 9, ClickableItem.of(display, e -> {
                                items.remove(index);
                                RebirthManager.getInstance().saveRebirth(rebirth);
                                player.sendMessage(CC.translate("&c✗ Item eliminado de la recompensa."));
                                createItemsMenu(rebirthId, backAction).open(player);
                            }));
                            slot++;
                            if (slot % 9 == 8) slot += 2;
                        }

                        ItemStack addButton = new ItemStack(Material.EMERALD_BLOCK);
                        ItemMeta addMeta = addButton.getItemMeta();
                        addMeta.setDisplayName(CC.translate("&a&lAgregar Item en Mano"));
                        addMeta.setLore(Arrays.asList(
                                CC.translate("&7Añade el item que sostienes"),
                                CC.translate("&7a las recompensas de este rebirth"),
                                "",
                                CC.translate("&a[CLICK PARA AGREGAR]")
                        ));
                        addButton.setItemMeta(addMeta);
                        contents.set(4, 2, ClickableItem.of(addButton, e -> {
                            org.bukkit.inventory.ItemStack hand = player.getItemInHand();
                            if (hand == null || hand.getType() == Material.AIR) {
                                player.sendMessage(CC.translate("&c✗ Debes sostener un item en la mano."));
                                return;
                            }
                            items.add(hand.clone());
                            RebirthManager.getInstance().saveRebirth(rebirth);
                            player.sendMessage(CC.translate("&a✓ Item agregado a la recompensa."));
                            createItemsMenu(rebirthId, backAction).open(player);
                        }));

                        ItemStack back = new ItemStack(Material.ARROW);
                        ItemMeta backMeta = back.getItemMeta();
                        backMeta.setDisplayName(CC.translate("&b← Atrás"));
                        back.setItemMeta(backMeta);
                        contents.set(4, 6, ClickableItem.of(back, e -> {
                            if (backAction != null) {
                                backAction.run();
                            } else {
                                createEditRebirthMenu(rebirthId).open(player);
                            }
                        }));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(6, 9)
                .title(CC.translate("&d&lItems Rebirth " + rebirthId))
                .listener(new InventoryListener<>(InventoryCloseEvent.class, event -> {
                    Player closingPlayer = (Player) event.getPlayer();
                    removeEditingItems(closingPlayer);
                    removeItemsBackAction(closingPlayer);
                }))
                .build();
    }

    public static SmartInventory createCommandsMenu(int rebirthId) {
        return createCommandsMenu(rebirthId, null);
    }

    public static SmartInventory createCommandsMenu(int rebirthId, Runnable backAction) {
        Rebirth rebirth = RebirthManager.getInstance().getRebirth(rebirthId);
        if (rebirth == null) return null;

        return SmartInventory.builder()
                .id("rebirth_admin_commands_" + rebirthId)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane((short) 5)));

                        Runnable reopenCommandsMenu = () -> createCommandsMenu(rebirthId, backAction).open(player);

                        ItemStack info = new ItemStack(Material.COMMAND);
                        ItemMeta infoMeta = info.getItemMeta();
                        infoMeta.setDisplayName(CC.translate("&5&lComandos de &e" + rebirth.getDisplayName()));
                        List<String> infoLore = new ArrayList<>();
                        infoLore.add(CC.translate("&7Comandos actuales: &f" + rebirth.getRewardCommands().size()));
                        infoLore.add(CC.translate("&7%player% = nombre del jugador"));
                        infoMeta.setLore(infoLore);
                        info.setItemMeta(infoMeta);
                        contents.set(0, 4, ClickableItem.empty(info));

                        List<String> commands = rebirth.getRewardCommands();
                        int slot = 10;
                        for (int i = 0; i < commands.size() && slot < 35; i++) {
                            String cmd = commands.get(i);
                            ItemStack paper = new ItemStack(Material.PAPER);
                            ItemMeta meta = paper.getItemMeta();
                            meta.setDisplayName(CC.translate("&e#" + (i + 1)));
                            meta.setLore(Arrays.asList(
                                    CC.translate("&f" + cmd),
                                    "",
                                    CC.translate("&c[CLICK PARA ELIMINAR]")
                            ));
                            paper.setItemMeta(meta);

                            final int index = i;
                            contents.set(slot / 9, slot % 9, ClickableItem.of(paper, e -> {
                                commands.remove(index);
                                RebirthManager.getInstance().saveRebirth(rebirth);
                                player.sendMessage(CC.translate("&c✗ Comando eliminado."));
                                reopenCommandsMenu.run();
                            }));
                            slot++;
                            if (slot % 9 == 8) slot += 2;
                        }

                        ItemStack addButton = new ItemStack(Material.EMERALD_BLOCK);
                        ItemMeta addMeta = addButton.getItemMeta();
                        addMeta.setDisplayName(CC.translate("&a&lAgregar Comando"));
                        addMeta.setLore(Arrays.asList(
                                CC.translate("&7Añade un comando a ejecutar"),
                                CC.translate("&7al desbloquear este rebirth"),
                                CC.translate("&7Usa %player% para el nombre"),
                                "",
                                CC.translate("&a[CLICK PARA AGREGAR]")
                        ));
                        addButton.setItemMeta(addMeta);
                        contents.set(4, 2, ClickableItem.of(addButton, e -> {
                            RebirthInputManager.startInput(player, rebirthId, RebirthInputManager.InputType.ADD_COMMAND, reopenCommandsMenu);
                        }));

                        ItemStack back = new ItemStack(Material.ARROW);
                        ItemMeta backMeta = back.getItemMeta();
                        backMeta.setDisplayName(CC.translate("&b← Atrás"));
                        back.setItemMeta(backMeta);
                        contents.set(4, 6, ClickableItem.of(back, e -> {
                            if (backAction != null) {
                                backAction.run();
                            } else {
                                createEditRebirthMenu(rebirthId).open(player);
                            }
                        }));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(6, 9)
                .title(CC.translate("&5&lComandos Rebirth " + rebirthId))
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
