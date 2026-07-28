package org.debentialc.customitems.tools.inventory;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.debentialc.customitems.commands.CustomItemCommand;
import org.debentialc.service.CC;
import org.debentialc.customitems.tools.ci.CustomItem;
import org.debentialc.customitems.tools.nbt.NbtHandler;
import org.debentialc.customitems.tools.storage.CustomItemStorage;

import java.util.*;

public class CustomItemMenus {

    public static SmartInventory createMainMenu() {
        return SmartInventory.builder()
                .id("ci_menu_main")
                .provider(new InventoryProvider() {
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane()));

                        ItemStack createButton = new ItemStack(Material.EMERALD_BLOCK);
                        ItemMeta createMeta = createButton.getItemMeta();
                        createMeta.setDisplayName(CC.translate("&a&lCrear Item"));
                        createMeta.setLore(Arrays.asList(
                                CC.translate("&7Crea un nuevo item custom"),
                                CC.translate("&7Debes sostener el item en mano")
                        ));
                        createButton.setItemMeta(createMeta);
                        contents.set(1, 1, ClickableItem.of(createButton, e -> {
                            player.closeInventory();
                            ItemCreationManager.startItemCreation(player);
                        }));

                        ItemStack listButton = new ItemStack(Material.BOOK);
                        ItemMeta listMeta = listButton.getItemMeta();
                        listMeta.setDisplayName(CC.translate("&c&lListar Items"));
                        listMeta.setLore(Arrays.asList(
                                CC.translate("&7Ve todos los items registrados"),
                                CC.translate("&7Y edítalos desde el menú")
                        ));
                        listButton.setItemMeta(listMeta);
                        contents.set(1, 4, ClickableItem.of(listButton, e -> {
                            openCategoryMenu(1).open(player);
                        }));

                        ItemStack helpButton = new ItemStack(Material.PAPER);
                        ItemMeta helpMeta = helpButton.getItemMeta();
                        helpMeta.setDisplayName(CC.translate("&e&lAyuda"));
                        helpMeta.setLore(Arrays.asList(
                                CC.translate("&7Abre el menú de ayuda"),
                                CC.translate("&7Con todos los comandos disponibles")
                        ));
                        helpButton.setItemMeta(helpMeta);
                        contents.set(1, 7, ClickableItem.of(helpButton, e -> {
                            openHelpMenu().open(player);
                        }));

                        ItemStack closeButton = new ItemStack(Material.REDSTONE_BLOCK);
                        ItemMeta closeMeta = closeButton.getItemMeta();
                        closeMeta.setDisplayName(CC.translate("&r&lCerrar"));
                        closeButton.setItemMeta(closeMeta);
                        contents.set(2, 4, ClickableItem.of(closeButton, e -> {
                            player.closeInventory();
                        }));
                    }

                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(3, 9)
                .title(CC.translate("&c&lGestión de Items Custom"))
                .build();
    }

    public static SmartInventory openCategoryMenu(int page) {
        return SmartInventory.builder()
                .id("ci_category_menu_" + page)
                .provider(new InventoryProvider() {
                    public void init(Player player, InventoryContents contents) {
                        Map<String, String> categories = new LinkedHashMap<>();
                        boolean hasUncategorized = false;
                        for (CustomItem item : CustomItemCommand.items.values()) {
                            if (item.getCategory() == null || item.getCategory().trim().isEmpty()) {
                                hasUncategorized = true;
                            } else {
                                String lower = item.getCategory().toLowerCase();
                                if (!categories.containsKey(lower)) {
                                    categories.put(lower, capitalize(item.getCategory()));
                                }
                            }
                        }

                        int pageSize = 21;
                        List<String> categoryList = new ArrayList<>(categories.values());
                        if (hasUncategorized) {
                            categoryList.add(0, "Sin categoría");
                        }
                        categoryList.add(0, "Todos");

                        int totalPages = (int) Math.ceil((double) categoryList.size() / pageSize);
                        if (page < 1) return;
                        if (page > totalPages && totalPages > 0) return;

                        contents.fillBorders(ClickableItem.empty(createGlassPane()));

                        ItemStack newCategoryButton = new ItemStack(Material.EMERALD_BLOCK);
                        ItemMeta newCategoryMeta = newCategoryButton.getItemMeta();
                        newCategoryMeta.setDisplayName(CC.translate("&a&l+ Nueva Categoría"));
                        newCategoryMeta.setLore(Arrays.asList(
                                CC.translate("&7Crea una categoría vacía"),
                                CC.translate("&ay asigna items después"),
                                CC.translate("&a[CLICK PARA CREAR]")
                        ));
                        newCategoryButton.setItemMeta(newCategoryMeta);
                        contents.set(0, 0, ClickableItem.of(newCategoryButton, e -> {
                            player.closeInventory();
                            CategoryInputManager.startCategoryCreation(player);
                        }));

                        int start = (page - 1) * pageSize;
                        int end = Math.min(start + pageSize, categoryList.size());

                        int row = 1;
                        int col = 1;

                        for (int i = start; i < end; i++) {
                            String category = categoryList.get(i);
                            ItemStack displayItem = new ItemStack(Material.BOOK);
                            ItemMeta meta = displayItem.getItemMeta();
                            meta.setDisplayName(CC.translate("&e&l" + category));

                            int count;
                            String targetCategory;
                            if (category.equals("Todos")) {
                                count = CustomItemCommand.items.size();
                                targetCategory = null;
                            } else if (category.equals("Sin categoría")) {
                                count = (int) CustomItemCommand.items.values().stream()
                                        .filter(item -> item.getCategory() == null || item.getCategory().trim().isEmpty())
                                        .count();
                                targetCategory = "";
                            } else {
                                count = (int) CustomItemCommand.items.values().stream()
                                        .filter(item -> category.equalsIgnoreCase(item.getCategory()))
                                        .count();
                                targetCategory = category.toLowerCase();
                            }

                            List<String> lore = new ArrayList<>();
                            lore.add(CC.translate("&7Items: &f" + count));
                            lore.add("");
                            lore.add(CC.translate("&a[CLICK PARA VER]"));
                            meta.setLore(lore);
                            displayItem.setItemMeta(meta);

                            String finalCategory = targetCategory;
                            contents.set(row, col, ClickableItem.of(displayItem, e -> {
                                openItemListMenu(1, finalCategory).open(player);
                            }));

                            col++;
                            if (col >= 8) {
                                col = 1;
                                row++;
                                if (row >= 4) break;
                            }
                        }

                        if (page > 1) {
                            ItemStack prevButton = new ItemStack(Material.ARROW);
                            ItemMeta prevMeta = prevButton.getItemMeta();
                            prevMeta.setDisplayName(CC.translate("&b← Anterior"));
                            prevButton.setItemMeta(prevMeta);
                            contents.set(4, 2, ClickableItem.of(prevButton, e -> {
                                openCategoryMenu(page - 1).open(player);
                            }));
                        }

                        ItemStack pageButton = new ItemStack(Material.BOOK);
                        ItemMeta pageMeta = pageButton.getItemMeta();
                        pageMeta.setDisplayName(CC.translate("&f&lPágina " + page + "/" + Math.max(1, totalPages)));
                        pageButton.setItemMeta(pageMeta);
                        contents.set(4, 4, ClickableItem.empty(pageButton));

                        if (page < totalPages) {
                            ItemStack nextButton = new ItemStack(Material.ARROW);
                            ItemMeta nextMeta = nextButton.getItemMeta();
                            nextMeta.setDisplayName(CC.translate("&bSiguiente →"));
                            nextButton.setItemMeta(nextMeta);
                            contents.set(4, 6, ClickableItem.of(nextButton, e -> {
                                openCategoryMenu(page + 1).open(player);
                            }));
                        }

                        ItemStack backButton = new ItemStack(Material.REDSTONE_BLOCK);
                        ItemMeta backMeta = backButton.getItemMeta();
                        backMeta.setDisplayName(CC.translate("&r← Atrás"));
                        backButton.setItemMeta(backMeta);
                        contents.set(4, 8, ClickableItem.of(backButton, e -> {
                            createMainMenu().open(player);
                        }));
                    }

                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(6, 9)
                .title(CC.translate("&c&lCategorías de Items"))
                .build();
    }

    public static SmartInventory openItemListMenu(int page, String category) {
        return SmartInventory.builder()
                .id("ci_list_menu_" + page + "_" + (category != null ? category : "all"))
                .provider(new InventoryProvider() {
                    public void init(Player player, InventoryContents contents) {
                        List<String> ids = new ArrayList<>();
                        for (Map.Entry<String, CustomItem> entry : CustomItemCommand.items.entrySet()) {
                            CustomItem item = entry.getValue();
                            if (category == null) {
                                ids.add(entry.getKey());
                            } else if (category.isEmpty()) {
                                if (item.getCategory() == null || item.getCategory().trim().isEmpty()) {
                                    ids.add(entry.getKey());
                                }
                            } else {
                                if (category.equalsIgnoreCase(item.getCategory())) {
                                    ids.add(entry.getKey());
                                }
                            }
                        }

                        int pageSize = 21;
                        int totalPages = (int) Math.ceil((double) ids.size() / pageSize);

                        if (page < 1) return;
                        if (page > totalPages && totalPages > 0) return;

                        contents.fillBorders(ClickableItem.empty(createGlassPane()));

                        Collections.sort(ids);
                        int start = (page - 1) * pageSize;
                        int end = Math.min(start + pageSize, ids.size());

                        int row = 1;
                        int col = 1;

                        for (int i = start; i < end; i++) {
                            String id = ids.get(i);
                            CustomItem customItem = CustomItemCommand.items.get(id);

                            ItemStack displayItem = new ItemStack(customItem.getMaterial(), 1, customItem.getDurabilityData());
                            ItemMeta meta = displayItem.getItemMeta();
                            meta.setDisplayName(CC.translate("&e" + id));

                            List<String> lore = new ArrayList<>();
                            lore.add(CC.translate("&7Nombre: &f" + customItem.getDisplayName()));
                            lore.add(CC.translate("&7Categoría: &f" + (customItem.getCategory() != null ? customItem.getCategory() : "Sin categoría")));
                            lore.add(CC.translate("&7Stats: &f" + customItem.getValueByStat().size()));
                            lore.add(CC.translate("&7Efectos: &f" + customItem.getEffects().size()));
                            lore.add(CC.translate("&7Lore: &f" + (customItem.getLore() != null ? customItem.getLore().size() + " líneas" : "Sin lore")));
                            lore.add("");
                            lore.add(CC.translate("&a[CLICK PARA EDITAR]"));

                            meta.setLore(lore);
                            displayItem.setItemMeta(meta);

                            String finalId = id;
                            contents.set(row, col, ClickableItem.of(displayItem, e -> {
                                openEditItemMenu(finalId, category).open(player);
                            }));

                            col++;
                            if (col >= 8) {
                                col = 1;
                                row++;
                                if (row >= 4) break;
                            }
                        }

                        if (page > 1) {
                            ItemStack prevButton = new ItemStack(Material.ARROW);
                            ItemMeta prevMeta = prevButton.getItemMeta();
                            prevMeta.setDisplayName(CC.translate("&b← Anterior"));
                            prevButton.setItemMeta(prevMeta);
                            contents.set(4, 2, ClickableItem.of(prevButton, e -> {
                                openItemListMenu(page - 1, category).open(player);
                            }));
                        }

                        ItemStack pageButton = new ItemStack(Material.BOOK);
                        ItemMeta pageMeta = pageButton.getItemMeta();
                        pageMeta.setDisplayName(CC.translate("&f&lPágina " + page + "/" + Math.max(1, totalPages)));
                        pageButton.setItemMeta(pageMeta);
                        contents.set(4, 4, ClickableItem.empty(pageButton));

                        if (page < totalPages) {
                            ItemStack nextButton = new ItemStack(Material.ARROW);
                            ItemMeta nextMeta = nextButton.getItemMeta();
                            nextMeta.setDisplayName(CC.translate("&bSiguiente →"));
                            nextButton.setItemMeta(nextMeta);
                            contents.set(4, 6, ClickableItem.of(nextButton, e -> {
                                openItemListMenu(page + 1, category).open(player);
                            }));
                        }

                        ItemStack backButton = new ItemStack(Material.REDSTONE_BLOCK);
                        ItemMeta backMeta = backButton.getItemMeta();
                        backMeta.setDisplayName(CC.translate("&r← Atrás"));
                        backButton.setItemMeta(backMeta);
                        contents.set(4, 8, ClickableItem.of(backButton, e -> {
                            openCategoryMenu(1).open(player);
                        }));
                    }

                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(6, 9)
                .title(CC.translate("&c&lItems Custom #" + page))
                .build();
    }

    public static SmartInventory openEditItemMenu(String itemId) {
        return openEditItemMenu(itemId, null);
    }

    public static SmartInventory openEditItemMenu(String itemId, String category) {
        CustomItem item = CustomItemCommand.items.get(itemId);
        if (item == null) return null;

        return SmartInventory.builder()
                .id("ci_edit_" + itemId)
                .provider(new InventoryProvider() {
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane()));

                        ItemStack infoItem = new ItemStack(Material.PAPER);
                        ItemMeta infoMeta = infoItem.getItemMeta();
                        infoMeta.setDisplayName(CC.translate("&c&l" + itemId));
                        List<String> infoLore = new ArrayList<>();
                        infoLore.add(CC.translate("&7ID: &f" + item.getId()));
                        infoLore.add(CC.translate("&7Nombre: &f" + item.getDisplayName()));
                        infoLore.add(CC.translate("&7Material: &f" + item.getMaterial() +
                                (item.getDurabilityData() > 0 ? "/" + item.getDurabilityData() : "")));
                        infoLore.add(CC.translate("&7Estado: &f" + (item.isActive() ? "Activo" : "Inactivo")));
                        infoMeta.setLore(infoLore);
                        infoItem.setItemMeta(infoMeta);
                        contents.set(1, 1, ClickableItem.of(infoItem, e -> {
                            player.closeInventory();
                            CustomIdChangeManager.startIdChange(player, itemId, "item");
                        }));

                        ItemStack renameButton = new ItemStack(Material.NAME_TAG);
                        ItemMeta renameMeta = renameButton.getItemMeta();
                        renameMeta.setDisplayName(CC.translate("&e&lRenombrar"));
                        renameMeta.setLore(Arrays.asList(
                                CC.translate("&7Nombre actual:"),
                                CC.translate("&f" + item.getDisplayName()),
                                "",
                                CC.translate("&a[CLICK PARA RENOMBRAR]")
                        ));
                        renameButton.setItemMeta(renameMeta);
                        contents.set(1, 2, ClickableItem.of(renameButton, e -> {
                            ItemEditManager.startItemEdit(player, itemId, "rename");
                        }));

                        ItemStack loreButton = new ItemStack(Material.BOOK_AND_QUILL);
                        ItemMeta loreMeta = loreButton.getItemMeta();
                        loreMeta.setDisplayName(CC.translate("&bEditar Lore"));
                        List<String> loreLoreList = new ArrayList<>();
                        loreLoreList.add(CC.translate("&7Líneas: &f" + (item.getLore() != null ? item.getLore().size() : 0)));
                        loreLoreList.add("");
                        loreLoreList.add(CC.translate("&a[CLICK PARA EDITAR]"));
                        loreLoreList.add(CC.translate("&7Usa Pastebin para lore largo"));
                        loreMeta.setLore(loreLoreList);
                        loreButton.setItemMeta(loreMeta);
                        contents.set(1, 3, ClickableItem.of(loreButton, e -> {
                            org.debentialc.customitems.tools.pastebin.PastebinLoreManager.startPastebinInput(player, itemId, "item");
                        }));

                        ItemStack statsButton = new ItemStack(Material.REDSTONE);
                        ItemMeta statsMeta = statsButton.getItemMeta();
                        statsMeta.setDisplayName(CC.translate("&aAñadir Stats"));
                        List<String> statsLore = new ArrayList<>();
                        statsLore.add(CC.translate("&7Stats actuales: &f" + item.getValueByStat().size()));
                        item.getValueByStat().forEach((stat, value) -> {
                            String op = item.getOperation().getOrDefault(stat, "+");
                            statsLore.add(CC.translate("&f  " + stat + " " + op + value));
                        });
                        statsLore.add("");
                        statsLore.add(CC.translate("&a[CLICK PARA AGREGAR]"));
                        statsMeta.setLore(statsLore);
                        statsButton.setItemMeta(statsMeta);
                        contents.set(1, 4, ClickableItem.of(statsButton, e -> {
                            BonusFlowManager.startBonusFlow(player, itemId);
                            CustomItemBonusMenus.createStatSelectionMenu(itemId).open(player);
                        }));

                        ItemStack effectsButton = new ItemStack(Material.REDSTONE_TORCH_ON);
                        ItemMeta effectsMeta = effectsButton.getItemMeta();
                        effectsMeta.setDisplayName(CC.translate("&6Agregar Efectos"));
                        List<String> effectsLore = new ArrayList<>();
                        effectsLore.add(CC.translate("&7Efectos actuales: &f" + item.getEffects().size()));
                        item.getEffects().forEach((effect, value) -> {
                            effectsLore.add(CC.translate("&f  " + effect + ": " + (value * 100) + "%"));
                        });
                        effectsLore.add("");
                        effectsLore.add(CC.translate("&a[CLICK PARA AGREGAR]"));
                        effectsMeta.setLore(effectsLore);
                        effectsButton.setItemMeta(effectsMeta);
                        contents.set(1, 5, ClickableItem.of(effectsButton, e -> {
                            CustomItemEffectsMenu.createEffectSelectionMenu(itemId).open(player);
                        }));

                        ItemStack categoryButton = new ItemStack(Material.CHEST);
                        ItemMeta categoryMeta = categoryButton.getItemMeta();
                        categoryMeta.setDisplayName(CC.translate("&d&lCategoría"));
                        List<String> categoryLore = new ArrayList<>();
                        categoryLore.add(CC.translate("&7Actual: &f" + (item.getCategory() != null ? item.getCategory() : "Sin categoría")));
                        categoryLore.add("");
                        categoryLore.add(CC.translate("&a[CLICK PARA CAMBIAR]"));
                        categoryMeta.setLore(categoryLore);
                        categoryButton.setItemMeta(categoryMeta);
                        contents.set(1, 6, ClickableItem.of(categoryButton, e -> {
                            player.closeInventory();
                            CategoryInputManager.startCategoryChange(player, itemId);
                        }));

                        ItemStack durabilityButton = new ItemStack(Material.DIAMOND_PICKAXE);
                        ItemMeta durabilityMeta = durabilityButton.getItemMeta();
                        durabilityMeta.setDisplayName(CC.translate("&d&lDurabilidad"));
                        List<String> durabilityLore = new ArrayList<>();
                        durabilityLore.add(CC.translate("&7Modifica la durabilidad del item"));
                        durabilityLore.add("");
                        durabilityLore.add(CC.translate("&7Actual: &f" + (item.getMaxDurability() > 0 ? item.getMaxDurability() : "Sin configurar")));
                        durabilityLore.add("");
                        durabilityLore.add(CC.translate("&a[CLICK PARA CONFIGURAR]"));
                        durabilityMeta.setLore(durabilityLore);
                        durabilityButton.setItemMeta(durabilityMeta);
                        contents.set(2, 1, ClickableItem.of(durabilityButton, e -> {
                            DurabilityInputManager.startDurabilityInput(player, itemId, "item");
                        }));

                        ItemStack unbreakableButton = new ItemStack(Material.BEDROCK);
                        ItemMeta unbreakableMeta = unbreakableButton.getItemMeta();
                        unbreakableMeta.setDisplayName(CC.translate("&b&lIrrompible"));
                        List<String> unbreakableLore = new ArrayList<>();
                        unbreakableLore.add(CC.translate("&7El item no recibirá daño"));
                        unbreakableLore.add(CC.translate("&7Aplica via NBT para items de mod"));
                        unbreakableLore.add("");
                        if (item.isUnbreakable()) {
                            unbreakableLore.add(CC.translate("&a✓ ACTIVADO"));
                            unbreakableLore.add("");
                            unbreakableLore.add(CC.translate("&c[CLICK PARA DESACTIVAR]"));
                        } else {
                            unbreakableLore.add(CC.translate("&c✗ DESACTIVADO"));
                            unbreakableLore.add("");
                            unbreakableLore.add(CC.translate("&a[CLICK PARA ACTIVAR]"));
                        }
                        unbreakableMeta.setLore(unbreakableLore);
                        unbreakableButton.setItemMeta(unbreakableMeta);
                        contents.set(2, 2, ClickableItem.of(unbreakableButton, e -> {
                            boolean newState = !item.isUnbreakable();
                            item.setUnbreakable(newState);
                            applyUnbreakableToNbt(item, newState);

                            CustomItemStorage storage = CustomItemStorage.getInstance();
                            storage.saveItem(item);

                            player.sendMessage("");
                            if (newState) {
                                player.sendMessage(CC.translate("&a✓ Item ahora es irrompible (NBT + vanilla)"));
                            } else {
                                player.sendMessage(CC.translate("&c✗ Item ya no es irrompible"));
                            }
                            player.sendMessage("");

                            openEditItemMenu(itemId).open(player);
                        }));

                        ItemStack damageButton = new ItemStack(Material.DIAMOND_SWORD);
                        ItemMeta damageMeta = damageButton.getItemMeta();
                        damageMeta.setDisplayName(CC.translate("&c&lDaño Custom"));
                        List<String> damageLore = new ArrayList<>();
                        damageLore.add(CC.translate("&7Modifica el daño via NBT"));
                        damageLore.add(CC.translate("&7Funciona en items de mod"));
                        damageLore.add("");
                        if (item.getAttackDamage() >= 0) {
                            damageLore.add(CC.translate("&7Daño actual: &f" + item.getAttackDamage()));
                            damageLore.add("");
                            damageLore.add(CC.translate("&a[CLICK PARA MODIFICAR]"));
                            damageLore.add(CC.translate("&7Ingresa &c0 &7para eliminar"));
                        } else {
                            damageLore.add(CC.translate("&7Daño actual: &fVanilla"));
                            damageLore.add("");
                            damageLore.add(CC.translate("&a[CLICK PARA CONFIGURAR]"));
                        }
                        damageMeta.setLore(damageLore);
                        damageButton.setItemMeta(damageMeta);
                        contents.set(2, 3, ClickableItem.of(damageButton, e -> {
                            AttackDamageInputManager.startAttackDamageInput(player, itemId);
                        }));

                        ItemStack advancedButton = new ItemStack(Material.NETHER_STAR);
                        ItemMeta advancedMeta = advancedButton.getItemMeta();
                        advancedMeta.setDisplayName(CC.translate("&5&l⚙ Opciones Avanzadas"));
                        List<String> advancedLore = new ArrayList<>();
                        advancedLore.add(CC.translate("&7Consumibles, Comandos,"));
                        advancedLore.add(CC.translate("&7TPs y más opciones"));
                        advancedLore.add("");
                        advancedLore.add(CC.translate("&a[CLICK PARA ABRIR]"));
                        advancedMeta.setLore(advancedLore);
                        advancedButton.setItemMeta(advancedMeta);
                        contents.set(2, 4, ClickableItem.of(advancedButton, e -> {
                            CustomItemAdvancedOptionsMenu.createAdvancedOptionsMenu(itemId).open(player);
                        }));

                        ItemStack changeIdButton = new ItemStack(Material.NAME_TAG);
                        ItemMeta changeIdMeta = changeIdButton.getItemMeta();
                        changeIdMeta.setDisplayName(CC.translate("&b&lCambiar Material"));
                        List<String> changeIdLore = new ArrayList<>();
                        changeIdLore.add(CC.translate("&7Material actual: &f" + item.getMaterial() +
                                (item.getDurabilityData() > 0 ? "/" + item.getDurabilityData() : "")));
                        changeIdLore.add("");
                        changeIdLore.add(CC.translate("&7Soporta IDs con data: &f567/50"));
                        changeIdLore.add("");
                        changeIdLore.add(CC.translate("&a[CLICK PARA CAMBIAR]"));
                        changeIdMeta.setLore(changeIdLore);
                        changeIdButton.setItemMeta(changeIdMeta);
                        contents.set(2, 5, ClickableItem.of(changeIdButton, e -> {
                            player.closeInventory();
                            ItemIdChangeManager.startMaterialIdChange(player, itemId, "item");
                        }));

                        ItemStack giveButton = new ItemStack(Material.APPLE);
                        ItemMeta giveMeta = giveButton.getItemMeta();
                        giveMeta.setDisplayName(CC.translate("&a&lDar Item"));
                        giveMeta.setLore(Arrays.asList(
                                CC.translate("&7Recibe el item en tu inventario"),
                                "",
                                CC.translate("&a[CLICK PARA RECIBIR]")
                        ));
                        giveButton.setItemMeta(giveMeta);
                        contents.set(1, 7, ClickableItem.of(giveButton, e -> {
                            ItemEditManager.giveCustomItem(player, itemId);
                        }));

                        ItemStack deleteButton = new ItemStack(Material.ANVIL);
                        ItemMeta deleteMeta = deleteButton.getItemMeta();
                        deleteMeta.setDisplayName(CC.translate("&c&lEliminar Item"));
                        deleteMeta.setLore(Arrays.asList(
                                CC.translate("&7Este item será eliminado"),
                                "",
                                CC.translate("&c[CLICK PARA CONFIRMAR]")
                        ));
                        deleteButton.setItemMeta(deleteMeta);
                        contents.set(2, 7, ClickableItem.of(deleteButton, e -> {
                            openDeleteConfirmMenu(itemId).open(player);
                        }));

                        ItemStack backButton = new ItemStack(Material.ARROW);
                        ItemMeta backMeta = backButton.getItemMeta();
                        backMeta.setDisplayName(CC.translate("&b← Atrás"));
                        backButton.setItemMeta(backMeta);
                        contents.set(3, 4, ClickableItem.of(backButton, e -> {
                            if (category != null) {
                                openItemListMenu(1, category).open(player);
                            } else {
                                openCategoryMenu(1).open(player);
                            }
                        }));
                    }

                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(4, 9)
                .title(CC.translate("&c&lEditar Item"))
                .build();
    }

    private static void applyUnbreakableToNbt(CustomItem item, boolean unbreakable) {
        org.bukkit.inventory.ItemStack itemStack = new org.bukkit.inventory.ItemStack(item.getMaterial(), 1, item.getDurabilityData());

        if (item.getNbtData() != null && !item.getNbtData().isEmpty()) {
            NbtHandler nbt = new NbtHandler(itemStack);
            nbt.setCompoundFromString(item.getNbtData());
            itemStack = nbt.getItemStack();
        }

        NbtHandler nbt = new NbtHandler(itemStack);
        nbt.setBoolean("Unbreakable", unbreakable);

        item.setNbtData(nbt.getCompound() != null ? nbt.getCompound().toString() : null);
    }

    public static SmartInventory openDeleteConfirmMenu(String itemId) {
        return SmartInventory.builder()
                .id("ci_delete_confirm_" + itemId)
                .provider(new InventoryProvider() {
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane()));

                        ItemStack confirmItem = new ItemStack(Material.REDSTONE_BLOCK);
                        ItemMeta confirmMeta = confirmItem.getItemMeta();
                        confirmMeta.setDisplayName(CC.translate("&c&l¿Estás seguro?"));
                        confirmMeta.setLore(Arrays.asList(
                                CC.translate("&7Esta acción no puede deshacerse"),
                                CC.translate("&7El item &f" + itemId + "&7 será eliminado"),
                                CC.translate("&7permanentemente")
                        ));
                        confirmItem.setItemMeta(confirmMeta);
                        contents.set(1, 4, ClickableItem.empty(confirmItem));

                        ItemStack yesButton = new ItemStack(Material.EMERALD_BLOCK);
                        ItemMeta yesMeta = yesButton.getItemMeta();
                        yesMeta.setDisplayName(CC.translate("&a&lSÍ, ELIMINAR"));
                        yesButton.setItemMeta(yesMeta);
                        contents.set(2, 3, ClickableItem.of(yesButton, e -> {
                            CustomItemCommand.items.remove(itemId);
                            org.debentialc.customitems.tools.storage.CustomItemStorage.getInstance().deleteItem(itemId);
                            player.sendMessage(CC.translate("&a✓ Item eliminado correctamente"));
                            openCategoryMenu(1).open(player);
                        }));

                        ItemStack noButton = new ItemStack(Material.REDSTONE_BLOCK);
                        ItemMeta noMeta = noButton.getItemMeta();
                        noMeta.setDisplayName(CC.translate("&c&lCANCELAR"));
                        noButton.setItemMeta(noMeta);
                        contents.set(2, 5, ClickableItem.of(noButton, e -> {
                            openEditItemMenu(itemId).open(player);
                        }));
                    }

                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(4, 9)
                .title(CC.translate("&c&lConfirmar Eliminación"))
                .build();
    }

    public static SmartInventory openHelpMenu() {
        return SmartInventory.builder()
                .id("ci_help_menu")
                .provider(new InventoryProvider() {
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane()));

                        List<String> commands = Arrays.asList(
                                "/ci create <id> - Crear un item",
                                "/ci delete <id> - Eliminar un item",
                                "/ci addline <texto> - Agregar lore",
                                "/ci plus <id> <valor> <stat> - Bonus +",
                                "/ci less <id> <valor> <stat> - Bonus -",
                                "/ci percentage <id> <val> <stat> - Bonus *",
                                "/ci effect <tipo> <valor> - Agregar efecto"
                        );

                        int row = 1;
                        for (String cmd : commands) {
                            ItemStack cmdItem = new ItemStack(Material.PAPER);
                            ItemMeta cmdMeta = cmdItem.getItemMeta();
                            cmdMeta.setDisplayName(CC.translate("&c" + cmd.split(" ")[0]));
                            cmdMeta.setLore(Collections.singletonList(CC.translate("&7" + cmd)));
                            cmdItem.setItemMeta(cmdMeta);
                            contents.set(row, 1, ClickableItem.empty(cmdItem));
                            row++;
                        }

                        ItemStack statsItem = new ItemStack(Material.REDSTONE);
                        ItemMeta statsMeta = statsItem.getItemMeta();
                        statsMeta.setDisplayName(CC.translate("&a&lStats Disponibles"));
                        statsMeta.setLore(Arrays.asList(
                                CC.translate("&fstr &7- Fuerza"),
                                CC.translate("&fcon &7- Constitución"),
                                CC.translate("&fdex &7- Destreza"),
                                CC.translate("&fwill &7- Voluntad"),
                                CC.translate("&fmnd &7- Mente")
                        ));
                        statsItem.setItemMeta(statsMeta);
                        contents.set(1, 7, ClickableItem.empty(statsItem));

                        ItemStack pastebinInfo = new ItemStack(Material.BOOK);
                        ItemMeta pastebinMeta = pastebinInfo.getItemMeta();
                        pastebinMeta.setDisplayName(CC.translate("&d&lPastebin"));
                        pastebinMeta.setLore(Arrays.asList(
                                CC.translate("&7Puedes cargar lore desde"),
                                CC.translate("&7pastebin.com usando el botón"),
                                CC.translate("&7en el menú de edición"),
                                "",
                                CC.translate("&fFormato: Una línea por línea")
                        ));
                        pastebinInfo.setItemMeta(pastebinMeta);
                        contents.set(3, 7, ClickableItem.empty(pastebinInfo));

                        ItemStack backButton = new ItemStack(Material.ARROW);
                        ItemMeta backMeta = backButton.getItemMeta();
                        backMeta.setDisplayName(CC.translate("&b← Atrás"));
                        backButton.setItemMeta(backMeta);
                        contents.set(5, 4, ClickableItem.of(backButton, e -> {
                            createMainMenu().open(player);
                        }));
                    }

                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(6, 9)
                .title(CC.translate("&c&lAyuda - Comandos /ci"))
                .build();
    }

    private static ItemStack createGlassPane() {
        ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(CC.translate("&8"));
        glass.setItemMeta(glassMeta);
        return glass;
    }

    private static String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        return Character.toUpperCase(text.charAt(0)) + text.substring(1).toLowerCase();
    }
}
