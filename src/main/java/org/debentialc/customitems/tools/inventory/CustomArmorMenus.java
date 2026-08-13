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
import org.debentialc.service.CC;
import org.debentialc.customitems.tools.ci.CustomArmor;

import java.util.*;

/**
 * Menús de gestión de armaduras custom
 * VERSIÓN MEJORADA: Integración completa con Pastebin para edición de lore
 */
public class CustomArmorMenus {

    public static SmartInventory createMainMenu() {
        return SmartInventory.builder()
                .id("ca_menu_main")
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane()));

                        ItemStack createButton = new ItemStack(Material.IRON_CHESTPLATE);
                        ItemMeta createMeta = createButton.getItemMeta();
                        createMeta.setDisplayName(CC.translate("&a&lCrear Armadura"));
                        createMeta.setLore(Arrays.asList(
                                CC.translate("&7Crea una nueva armadura custom"),
                                CC.translate("&7Debes sostener la armadura en mano")
                        ));
                        createButton.setItemMeta(createMeta);

                        contents.set(1, 1, ClickableItem.of(createButton, e -> {
                            player.closeInventory();
                            ArmorCreationManager.startArmorCreation(player);
                        }));

                        ItemStack listButton = new ItemStack(Material.BOOK);
                        ItemMeta listMeta = listButton.getItemMeta();
                        listMeta.setDisplayName(CC.translate("&b&lListar Armaduras"));
                        listMeta.setLore(Arrays.asList(
                                CC.translate("&7Ve todas las armaduras registradas"),
                                CC.translate("&7Y edítalas desde el menú")
                        ));
                        listButton.setItemMeta(listMeta);

                        contents.set(1, 4, ClickableItem.of(listButton, e -> {
                            openArmorListMenu(1).open(player);
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

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(3, 9)
                .title(CC.translate("&b&lGestión Armaduras Custom"))
                .build();
    }

    public static SmartInventory openArmorListMenu(int page) {
        return SmartInventory.builder()
                .id("ca_list_menu_" + page)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        Set<String> armorIds = RegisterItem.items.keySet();
                        int pageSize = 21;
                        int totalPages = (int) Math.ceil((double) armorIds.size() / pageSize);

                        if (page < 1) return;
                        if (page > totalPages && totalPages > 0) return;

                        contents.fillBorders(ClickableItem.empty(createGlassPane()));

                        List<String> ids = new ArrayList<>(armorIds);
                        Collections.sort(ids);
                        int start = (page - 1) * pageSize;
                        int end = Math.min(start + pageSize, ids.size());

                        int row = 1;
                        int col = 1;

                        for (int i = start; i < end; i++) {
                            String id = ids.get(i);
                            CustomArmor armor = RegisterItem.items.get(id);
                            if (armor == null) continue;

                            Material mat = Material.getMaterial(armor.getMaterial());
                            if (mat == null || mat == Material.AIR) {
                                mat = Material.IRON_CHESTPLATE;
                            }
                            ItemStack displayItem = new ItemStack(mat);
                            ItemMeta meta = displayItem.getItemMeta();
                            if (meta == null) continue;
                            meta.setDisplayName(CC.translate("&b" + id));

                            List<String> lore = new ArrayList<>();
                            lore.add(CC.translate("&7Nombre: &f" + armor.getDisplayName()));
                            lore.add(CC.translate("&7Stats: &f" + armor.getValueByStat().size()));
                            lore.add(CC.translate("&7Efectos: &f" + armor.getEffects().size()));
                            lore.add(CC.translate("&7Lore: &f" + (armor.getLore() != null ? armor.getLore().size() + " líneas" : "Sin lore")));
                            lore.add("");
                            lore.add(CC.translate("&a[CLICK PARA EDITAR]"));

                            meta.setLore(lore);
                            displayItem.setItemMeta(meta);

                            String finalId = id;
                            contents.set(row, col, ClickableItem.of(displayItem, e -> {
                                openEditArmorMenu(finalId).open(player);
                            }));

                            col++;
                            if (col >= 8) {
                                col = 1;
                                row++;

                                if (row >= 4) {
                                    break;
                                }
                            }
                        }

                        if (page > 1) {
                            ItemStack prevButton = new ItemStack(Material.ARROW);
                            ItemMeta prevMeta = prevButton.getItemMeta();
                            prevMeta.setDisplayName(CC.translate("&b← Anterior"));
                            prevButton.setItemMeta(prevMeta);

                            contents.set(4, 2, ClickableItem.of(prevButton, e -> {
                                openArmorListMenu(page - 1).open(player);
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
                                openArmorListMenu(page + 1).open(player);
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

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(6, 9)
                .title(CC.translate("&b&lArmaduras #" + page))
                .build();
    }

    public static SmartInventory openEditArmorMenu(String armorId) {
        CustomArmor armor = RegisterItem.items.get(armorId);
        if (armor == null) return null;

        return SmartInventory.builder()
                .id("ca_edit_" + armorId)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane()));

                        // INFO DE LA ARMADURA
                        ItemStack infoItem = new ItemStack(Material.PAPER);
                        ItemMeta infoMeta = infoItem.getItemMeta();
                        infoMeta.setDisplayName(CC.translate("&b&l" + armorId));
                        List<String> infoLore = new ArrayList<>();
                        infoLore.add(CC.translate("&7ID: &f" + armor.getId()));
                        infoLore.add(CC.translate("&7Nombre: &f" + armor.getDisplayName()));
                        infoLore.add(CC.translate("&7Material: &f" + armor.getMaterial()));
                        infoLore.add(CC.translate("&7Estado: &f" + (armor.isArmor() ? "Activa" : "Inactiva")));
                        infoLore.add("");
                        infoLore.add(CC.translate("&a[CLICK PARA CAMBIAR ID CUSTOM]"));
                        infoItem.setItemMeta(infoMeta);
                        contents.set(1, 1, ClickableItem.of(infoItem, e -> {
                            player.closeInventory();
                            CustomIdChangeManager.startIdChange(player, armorId, "armor");
                        }));

                        // RENOMBRAR
                        ItemStack renameButton = new ItemStack(Material.NAME_TAG);
                        ItemMeta renameMeta = renameButton.getItemMeta();
                        renameMeta.setDisplayName(CC.translate("&e&lRenombrar"));
                        renameMeta.setLore(Arrays.asList(
                                CC.translate("&7Nombre actual:"),
                                CC.translate("&f" + armor.getDisplayName()),
                                "",
                                CC.translate("&a[CLICK PARA RENOMBRAR]")
                        ));
                        renameButton.setItemMeta(renameMeta);
                        contents.set(1, 2, ClickableItem.of(renameButton, e -> {
                            ArmorEditManager.startArmorEdit(player, armorId, "rename");
                        }));

                        ItemStack pastebinButton = new ItemStack(Material.ENCHANTED_BOOK);
                        ItemMeta pastebinMeta = pastebinButton.getItemMeta();
                        pastebinMeta.setDisplayName(CC.translate("&d&l⚡ Lore desde Pastebin"));
                        List<String> pastebinLore = new ArrayList<>();
                        pastebinLore.add(CC.translate("&7Carga el lore completo desde"));
                        pastebinLore.add(CC.translate("&7una URL de Pastebin"));
                        pastebinLore.add("");
                        pastebinLore.add(CC.translate("&e&oLíneas actuales: &f" + (armor.getLore() != null ? armor.getLore().size() : 0)));
                        pastebinLore.add("");
                        pastebinLore.add(CC.translate("&a[CLICK PARA CARGAR]"));
                        pastebinLore.add(CC.translate("&8Compatible con /ca addline"));
                        pastebinMeta.setLore(pastebinLore);
                        pastebinButton.setItemMeta(pastebinMeta);
                        contents.set(1, 3, ClickableItem.of(pastebinButton, e -> {
                            org.debentialc.customitems.tools.pastebin.PastebinLoreManager.startPastebinInput(player, armorId, "armor");
                        }));

                        // EDITAR LORE (PASTEBIN)
                        ItemStack loreButton = new ItemStack(Material.BOOK_AND_QUILL);
                        ItemMeta loreMeta = loreButton.getItemMeta();
                        loreMeta.setDisplayName(CC.translate("&bEditar Lore"));
                        loreMeta.setLore(Arrays.asList(
                                CC.translate("&7Líneas: &f" + (armor.getLore() != null ? armor.getLore().size() : 0)),
                                "",
                                CC.translate("&a[CLICK PARA EDITAR]"),
                                CC.translate("&7Usa Pastebin para lore largo")
                        ));
                        loreButton.setItemMeta(loreMeta);
                        contents.set(1, 3, ClickableItem.of(loreButton, e -> {
                            org.debentialc.customitems.tools.pastebin.PastebinLoreManager.startPastebinInput(player, armorId, "armor");
                        }));

                        // AÑADIR STATS
                        ItemStack statsButton = new ItemStack(Material.REDSTONE);
                        ItemMeta statsMeta = statsButton.getItemMeta();
                        statsMeta.setDisplayName(CC.translate("&aAñadir Stats"));
                        List<String> statsLore = new ArrayList<>();
                        statsLore.add(CC.translate("&7Stats actuales: &f" + armor.getValueByStat().size()));
                        armor.getValueByStat().forEach((stat, value) -> {
                            String op = armor.getOperation().getOrDefault(stat, "+");
                            statsLore.add(CC.translate("&f  " + stat + " " + op + value));
                        });
                        statsLore.add("");
                        statsLore.add(CC.translate("&a[CLICK PARA AGREGAR]"));
                        statsMeta.setLore(statsLore);
                        statsButton.setItemMeta(statsMeta);
                        contents.set(1, 4, ClickableItem.of(statsButton, e -> {
                            BonusFlowManager.startBonusFlow(player, armorId);
                            CustomArmorBonusMenus.createStatSelectionMenu(armorId).open(player);
                        }));

                        // AGREGAR EFECTOS
                        ItemStack effectsButton = new ItemStack(Material.REDSTONE_TORCH_ON);
                        ItemMeta effectsMeta = effectsButton.getItemMeta();
                        effectsMeta.setDisplayName(CC.translate("&6Agregar Efectos"));
                        List<String> effectsLore = new ArrayList<>();
                        effectsLore.add(CC.translate("&7Efectos actuales: &f" + armor.getEffects().size()));
                        armor.getEffects().forEach((effect, value) -> {
                            effectsLore.add(CC.translate("&f  " + effect + ": " + (value * 100) + "%"));
                        });
                        effectsLore.add("");
                        effectsLore.add(CC.translate("&a[CLICK PARA AGREGAR]"));
                        effectsMeta.setLore(effectsLore);
                        effectsButton.setItemMeta(effectsMeta);
                        contents.set(1, 5, ClickableItem.of(effectsButton, e -> {
                            CustomArmorEffectsMenus.createEffectSelectionMenu(armorId).open(player);
                        }));

                        // MODIFICAR DURABILIDAD
                        ItemStack durabilityButton = new ItemStack(Material.DIAMOND_CHESTPLATE);
                        ItemMeta durabilityMeta = durabilityButton.getItemMeta();
                        durabilityMeta.setDisplayName(CC.translate("&d&lDurabilidad"));
                        List<String> durabilityLore = new ArrayList<>();
                        durabilityLore.add(CC.translate("&7Modifica la durabilidad de la armadura"));
                        durabilityLore.add("");
                        durabilityLore.add(CC.translate("&7Actual: &f" + (armor.getMaxDurability() > 0 ? armor.getMaxDurability() : "Sin configurar")));
                        durabilityLore.add("");
                        durabilityLore.add(CC.translate("&a[CLICK PARA CONFIGURAR]"));
                        durabilityMeta.setLore(durabilityLore);
                        durabilityButton.setItemMeta(durabilityMeta);
                        contents.set(2, 1, ClickableItem.of(durabilityButton, e -> {
                            DurabilityInputManager.startDurabilityInput(player, armorId, "armor");
                        }));

                        // CAMBIAR ID
                        ItemStack changeIdButton = new ItemStack(Material.NAME_TAG);
                        ItemMeta changeIdMeta = changeIdButton.getItemMeta();
                        changeIdMeta.setDisplayName(CC.translate("&b&lCambiar Material"));
                        List<String> changeIdLore = new ArrayList<>();
                        changeIdLore.add(CC.translate("&7Material actual: &f" + armor.getMaterial()));
                        changeIdLore.add("");
                        changeIdLore.add(CC.translate("&7Permite cambiar el material"));
                        changeIdLore.add(CC.translate("&7Soporta IDs con data: id/data"));
                        changeIdLore.add("");
                        changeIdLore.add(CC.translate("&a[CLICK PARA CAMBIAR]"));
                        changeIdMeta.setLore(changeIdLore);
                        changeIdButton.setItemMeta(changeIdMeta);
                        contents.set(2, 3, ClickableItem.of(changeIdButton, e -> {
                            player.closeInventory();
                            ItemIdChangeManager.startMaterialIdChange(player, armorId, "armor");
                        }));

                        // HACER IRROMPIBLE

                        // HACER IRROMPIBLE
                        ItemStack unbreakableButton = new ItemStack(Material.BEDROCK);
                        ItemMeta unbreakableMeta = unbreakableButton.getItemMeta();
                        unbreakableMeta.setDisplayName(CC.translate("&b&lIrrompible"));
                        List<String> unbreakableLore = new ArrayList<>();
                        unbreakableLore.add(CC.translate("&7La armadura no recibirá daño"));
                        unbreakableLore.add("");
                        if (armor.isUnbreakable()) {
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
                            // Alternar estado
                            armor.setUnbreakable(!armor.isUnbreakable());

                            // Guardar
                            org.debentialc.customitems.tools.storage.CustomArmorStorage storage =
                                    org.debentialc.customitems.tools.storage.CustomArmorStorage.getInstance();
                            storage.saveArmor(armor);

                            // Actualizar en memoria
                            RegisterItem.items.put(armorId, armor);

                            // Mensaje
                            player.sendMessage("");
                            if (armor.isUnbreakable()) {
                                player.sendMessage(CC.translate("&a✓ Armadura ahora es irrompible"));
                            } else {
                                player.sendMessage(CC.translate("&c✗ Armadura ya no es irrompible"));
                            }
                            player.sendMessage("");

                            // Reabrir menú
                            openEditArmorMenu(armorId).open(player);
                        }));
                        // OPCIONES AVANZADAS
                        ItemStack advancedButton = new ItemStack(Material.NETHER_STAR);
                        ItemMeta advancedMeta = advancedButton.getItemMeta();
                        advancedMeta.setDisplayName(CC.translate("&5&l⚙ Opciones Avanzadas"));
                        List<String> advancedLore = new ArrayList<>();
                        advancedLore.add(CC.translate("&7Rebirth y permisos"));
                        advancedLore.add("");
                        if (armor.getRequiredRebirthBlock() != null && !armor.getRequiredRebirthBlock().isEmpty() && armor.getRequiredRebirthLevel() > 0) {
                            advancedLore.add(CC.translate("&7Bloque: &f" + armor.getRequiredRebirthBlock()));
                            advancedLore.add(CC.translate("&7Nivel: &f" + armor.getRequiredRebirthLevel()));
                        }
                        if (armor.getRequiredPermission() != null && !armor.getRequiredPermission().isEmpty()) {
                            advancedLore.add(CC.translate("&7Permiso: &f" + armor.getRequiredPermission()));
                        }
                        if ((armor.getRequiredRebirthBlock() == null || armor.getRequiredRebirthBlock().isEmpty() || armor.getRequiredRebirthLevel() <= 0) &&
                                (armor.getRequiredPermission() == null || armor.getRequiredPermission().isEmpty())) {
                            advancedLore.add(CC.translate("&cSin requisitos extra"));
                        }
                        advancedLore.add("");
                        advancedLore.add(CC.translate("&a[CLICK PARA ABRIR]"));
                        advancedMeta.setLore(advancedLore);
                        advancedButton.setItemMeta(advancedMeta);
                        contents.set(2, 4, ClickableItem.of(advancedButton, e -> {
                            CustomArmorAdvancedOptionsMenu.createAdvancedOptionsMenu(armorId).open(player);
                        }));

                        // DAR ARMADURA
                        ItemStack giveButton = new ItemStack(Material.APPLE);
                        ItemMeta giveMeta = giveButton.getItemMeta();
                        giveMeta.setDisplayName(CC.translate("&a&lDar Armadura"));
                        giveMeta.setLore(Arrays.asList(
                                CC.translate("&7Recibe la armadura en tu inventario"),
                                "",
                                CC.translate("&a[CLICK PARA RECIBIR]")
                        ));
                        giveButton.setItemMeta(giveMeta);
                        contents.set(1, 7, ClickableItem.of(giveButton, e -> {
                            ArmorEditManager.giveCustomArmor(player, armorId);
                        }));

                        // ELIMINAR ARMADURA
                        ItemStack deleteButton = new ItemStack(Material.ANVIL);
                        ItemMeta deleteMeta = deleteButton.getItemMeta();
                        deleteMeta.setDisplayName(CC.translate("&c&lEliminar Armadura"));
                        deleteMeta.setLore(Arrays.asList(
                                CC.translate("&7Esta armadura será eliminada"),
                                "",
                                CC.translate("&c[CLICK PARA CONFIRMAR]")
                        ));
                        deleteButton.setItemMeta(deleteMeta);
                        contents.set(2, 7, ClickableItem.of(deleteButton, e -> {
                            openDeleteConfirmMenu(armorId).open(player);
                        }));

                        // BOTÓN ATRÁS
                        ItemStack backButton = new ItemStack(Material.ARROW);
                        ItemMeta backMeta = backButton.getItemMeta();
                        backMeta.setDisplayName(CC.translate("&b← Atrás"));
                        backButton.setItemMeta(backMeta);
                        contents.set(3, 4, ClickableItem.of(backButton, e -> {
                            openArmorListMenu(1).open(player);
                        }));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(4, 9)
                .title(CC.translate("&b&lEditar Armadura"))
                .build();
    }

    public static SmartInventory openDeleteConfirmMenu(String armorId) {
        return SmartInventory.builder()
                .id("ca_delete_confirm_" + armorId)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane()));

                        ItemStack confirmItem = new ItemStack(Material.REDSTONE_BLOCK);
                        ItemMeta confirmMeta = confirmItem.getItemMeta();
                        confirmMeta.setDisplayName(CC.translate("&c&l¿Estás seguro?"));
                        confirmMeta.setLore(Arrays.asList(
                                CC.translate("&7Esta acción no puede deshacerse"),
                                CC.translate("&7La armadura &f" + armorId + "&7 será eliminada"),
                                CC.translate("&7permanentemente")
                        ));
                        confirmItem.setItemMeta(confirmMeta);
                        contents.set(1, 4, ClickableItem.empty(confirmItem));

                        ItemStack yesButton = new ItemStack(Material.EMERALD_BLOCK);
                        ItemMeta yesMeta = yesButton.getItemMeta();
                        yesMeta.setDisplayName(CC.translate("&a&lSÍ, ELIMINAR"));
                        yesButton.setItemMeta(yesMeta);
                        contents.set(2, 3, ClickableItem.of(yesButton, e -> {
                            RegisterItem.items.remove(armorId);
                            // Eliminar de la base de datos (si existe)
                            try {
                                org.debentialc.customitems.tools.storage.CustomArmorStorage storage = org.debentialc.customitems.tools.storage.CustomArmorStorage.getInstance();
                                storage.deleteArmor(armorId);
                            } catch (Exception ex) {
                            }

                            player.sendMessage(CC.translate("&a✓ Armadura eliminada correctamente"));
                            openArmorListMenu(1).open(player);
                        }));

                        ItemStack noButton = new ItemStack(Material.REDSTONE_BLOCK);
                        ItemMeta noMeta = noButton.getItemMeta();
                        noMeta.setDisplayName(CC.translate("&c&lCANCELAR"));
                        noButton.setItemMeta(noMeta);
                        contents.set(2, 5, ClickableItem.of(noButton, e -> {
                            openEditArmorMenu(armorId).open(player);
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

    public static SmartInventory openHelpMenu() {
        return SmartInventory.builder()
                .id("ca_help_menu")
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane()));

                        List<String> commands = Arrays.asList(
                                "/ca create <id> - Crear armadura",
                                "/ca delete <id> - Eliminar armadura",
                                "/ca addline <texto> - Agregar lore",
                                "/ca plus <id> <valor> <stat> - Bonus +",
                                "/ca less <id> <valor> <stat> - Bonus -",
                                "/ca percentage <id> <val> <stat> - Bonus *",
                                "/ca effect <tipo> <valor> - Agregar efecto"
                        );

                        int row = 1;
                        for (String cmd : commands) {
                            ItemStack cmdItem = new ItemStack(Material.PAPER);
                            ItemMeta cmdMeta = cmdItem.getItemMeta();
                            cmdMeta.setDisplayName(CC.translate("&b" + cmd.split(" ")[0]));
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

                        // Información de Pastebin
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

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(6, 9)
                .title(CC.translate("&b&lAyuda - Comandos /ca"))
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