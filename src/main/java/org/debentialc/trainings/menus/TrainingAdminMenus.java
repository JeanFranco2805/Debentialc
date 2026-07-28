package org.debentialc.trainings.menus;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.debentialc.service.CC;
import org.debentialc.trainings.managers.TrainingInputManager;
import org.debentialc.trainings.managers.TrainingManager;
import org.debentialc.trainings.managers.TrainingNpcInputManager;
import org.debentialc.trainings.model.Training;
import org.debentialc.trainings.model.TrainingLevel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrainingAdminMenus {

    public static SmartInventory createMainMenu(int page) {
        return SmartInventory.builder()
                .id("training_admin_main_" + page)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane((short) 7)));

                        ItemStack info = new ItemStack(Material.NETHER_STAR);
                        ItemMeta infoMeta = info.getItemMeta();
                        infoMeta.setDisplayName(CC.translate("&6&lConfiguración de Trainings"));
                        List<String> infoLore = new ArrayList<>();
                        infoLore.add(CC.translate("&7Gestiona los trainings del servidor"));
                        infoLore.add(CC.translate("&7Total trainings: &f" + TrainingManager.getInstance().getAllTrainings().size()));
                        infoMeta.setLore(infoLore);
                        info.setItemMeta(infoMeta);
                        contents.set(0, 4, ClickableItem.empty(info));

                        List<Training> trainingList = new ArrayList<>(TrainingManager.getInstance().getAllTrainings());
                        int pageSize = 7;
                        int totalPages = Math.max(1, (int) Math.ceil((double) trainingList.size() / pageSize));
                        if (page < 1) return;
                        if (page > totalPages) return;

                        int start = (page - 1) * pageSize;
                        int end = Math.min(start + pageSize, trainingList.size());

                        int slot = 10;
                        for (int i = start; i < end; i++) {
                            Training training = trainingList.get(i);
                            ItemStack item = createTrainingIcon(training);
                            final int currentSlot = slot;
                            contents.set(currentSlot / 9, currentSlot % 9, ClickableItem.of(item, e -> {
                                createEditTrainingMenu(training.getId(), () -> createMainMenu(page).open(player)).open(player);
                            }));
                            slot++;
                        }

                        if (page > 1) {
                            ItemStack prev = new ItemStack(Material.ARROW);
                            ItemMeta prevMeta = prev.getItemMeta();
                            prevMeta.setDisplayName(CC.translate("&b← Anterior"));
                            prev.setItemMeta(prevMeta);
                            contents.set(5, 2, ClickableItem.of(prev, e -> createMainMenu(page - 1).open(player)));
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
                            contents.set(5, 6, ClickableItem.of(next, e -> createMainMenu(page + 1).open(player)));
                        }

                        ItemStack createButton = new ItemStack(Material.EMERALD_BLOCK);
                        ItemMeta createMeta = createButton.getItemMeta();
                        createMeta.setDisplayName(CC.translate("&a&lCrear Training"));
                        createMeta.setLore(Arrays.asList(
                                CC.translate("&7Crea un nuevo training"),
                                CC.translate("&a[CLICK PARA CREAR]")
                        ));
                        createButton.setItemMeta(createMeta);
                        contents.set(2, 1, ClickableItem.of(createButton, e -> {
                            Training training = new Training(TrainingManager.getInstance().getNextTrainingId());
                            TrainingManager.getInstance().saveTraining(training);
                            player.sendMessage(CC.translate("&a✓ Training &e" + training.getId() + " &acreado."));
                            createMainMenu(page).open(player);
                        }));

                        ItemStack vipButton = new ItemStack(Material.DIAMOND);
                        ItemMeta vipMeta = vipButton.getItemMeta();
                        vipMeta.setDisplayName(CC.translate("&b&lVer VIP"));
                        vipMeta.setLore(Arrays.asList(
                                CC.translate("&7Muestra solo trainings VIP"),
                                CC.translate("&b[CLICK PARA VER]")
                        ));
                        vipButton.setItemMeta(vipMeta);
                        contents.set(2, 7, ClickableItem.of(vipButton, e -> createVipListMenu(1).open(player)));

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
                .title(CC.translate("&c&lAdmin Trainings"))
                .build();
    }

    public static SmartInventory createVipListMenu(int page) {
        return SmartInventory.builder()
                .id("training_admin_vip_" + page)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane((short) 3)));

                        ItemStack info = new ItemStack(Material.DIAMOND);
                        ItemMeta infoMeta = info.getItemMeta();
                        infoMeta.setDisplayName(CC.translate("&b&lTrainings VIP"));
                        info.setItemMeta(infoMeta);
                        contents.set(0, 4, ClickableItem.empty(info));

                        List<Training> trainingList = new ArrayList<>(TrainingManager.getInstance().getVipTrainings());
                        int pageSize = 7;
                        int totalPages = Math.max(1, (int) Math.ceil((double) trainingList.size() / pageSize));
                        if (page < 1) return;
                        if (page > totalPages) return;

                        int start = (page - 1) * pageSize;
                        int end = Math.min(start + pageSize, trainingList.size());

                        int slot = 10;
                        for (int i = start; i < end; i++) {
                            Training training = trainingList.get(i);
                            ItemStack item = createTrainingIcon(training);
                            contents.set(slot / 9, slot % 9, ClickableItem.of(item, e -> {
                                createEditTrainingMenu(training.getId(), () -> createVipListMenu(page).open(player)).open(player);
                            }));
                            slot++;
                        }

                        if (page > 1) {
                            ItemStack prev = new ItemStack(Material.ARROW);
                            ItemMeta prevMeta = prev.getItemMeta();
                            prevMeta.setDisplayName(CC.translate("&b← Anterior"));
                            prev.setItemMeta(prevMeta);
                            contents.set(5, 2, ClickableItem.of(prev, e -> createVipListMenu(page - 1).open(player)));
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
                            contents.set(5, 6, ClickableItem.of(next, e -> createVipListMenu(page + 1).open(player)));
                        }

                        ItemStack back = new ItemStack(Material.REDSTONE_BLOCK);
                        ItemMeta backMeta = back.getItemMeta();
                        backMeta.setDisplayName(CC.translate("&c← Atrás"));
                        back.setItemMeta(backMeta);
                        contents.set(5, 0, ClickableItem.of(back, e -> createMainMenu(1).open(player)));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(6, 9)
                .title(CC.translate("&b&lTrainings VIP"))
                .build();
    }

    public static SmartInventory createEditTrainingMenu(String trainingId) {
        return createEditTrainingMenu(trainingId, null);
    }

    public static SmartInventory createEditTrainingMenu(String trainingId, Runnable backAction) {
        Training training = TrainingManager.getInstance().getTraining(trainingId);
        if (training == null) return null;

        return SmartInventory.builder()
                .id("training_admin_edit_" + trainingId)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane((short) 7)));

                        Runnable reopenMenu = () -> createEditTrainingMenu(trainingId, backAction).open(player);

                        ItemStack info = createTrainingIcon(training);
                        contents.set(0, 4, ClickableItem.empty(info));

                        ItemStack nameButton = new ItemStack(Material.NAME_TAG);
                        ItemMeta nameMeta = nameButton.getItemMeta();
                        nameMeta.setDisplayName(CC.translate("&e&lCambiar Nombre"));
                        nameMeta.setLore(Arrays.asList(
                                CC.translate("&7Actual: &r" + training.getName()),
                                CC.translate("&a[CLICK PARA EDITAR]")
                        ));
                        nameButton.setItemMeta(nameMeta);
                        contents.set(1, 1, ClickableItem.of(nameButton, e -> {
                            TrainingInputManager.startInput(player, trainingId, TrainingInputManager.InputType.TRAINING_NAME, reopenMenu);
                        }));

                        ItemStack materialButton = new ItemStack(Material.getMaterial(training.getMaterialId()) != null ? Material.getMaterial(training.getMaterialId()) : Material.PAPER);
                        ItemMeta materialMeta = materialButton.getItemMeta();
                        materialMeta.setDisplayName(CC.translate("&b&lCambiar Material"));
                        materialMeta.setLore(Arrays.asList(
                                CC.translate("&7Actual: &f" + training.getMaterialId()),
                                CC.translate("&a[CLICK PARA EDITAR]")
                        ));
                        materialButton.setItemMeta(materialMeta);
                        contents.set(1, 2, ClickableItem.of(materialButton, e -> {
                            TrainingInputManager.startInput(player, trainingId, TrainingInputManager.InputType.TRAINING_MATERIAL, reopenMenu);
                        }));

                        ItemStack vipButton = new ItemStack(training.isVip() ? Material.DIAMOND_BLOCK : Material.COAL_BLOCK);
                        ItemMeta vipMeta = vipButton.getItemMeta();
                        vipMeta.setDisplayName(CC.translate("&d&lVIP: " + (training.isVip() ? "&aSÍ" : "&cNO")));
                        vipMeta.setLore(Arrays.asList(
                                CC.translate("&7Click para cambiar"),
                                CC.translate("&a[CLICK PARA TOGGLEAR]")
                        ));
                        vipButton.setItemMeta(vipMeta);
                        contents.set(1, 3, ClickableItem.of(vipButton, e -> {
                            training.setVip(!training.isVip());
                            TrainingManager.getInstance().saveTraining(training);
                            reopenMenu.run();
                        }));

                        ItemStack permButton = new ItemStack(Material.BOOK);
                        ItemMeta permMeta = permButton.getItemMeta();
                        permMeta.setDisplayName(CC.translate("&6&lPermiso Requerido"));
                        String perm = training.getRequiredPermission();
                        permMeta.setLore(Arrays.asList(
                                CC.translate("&7Actual: &f" + (perm == null || perm.isEmpty() ? "Ninguno" : perm)),
                                CC.translate("&a[CLICK PARA EDITAR]")
                        ));
                        permButton.setItemMeta(permMeta);
                        contents.set(1, 4, ClickableItem.of(permButton, e -> {
                            TrainingInputManager.startInput(player, trainingId, TrainingInputManager.InputType.TRAINING_PERMISSION, reopenMenu);
                        }));

                        ItemStack levelsButton = new ItemStack(Material.BOOKSHELF);
                        ItemMeta levelsMeta = levelsButton.getItemMeta();
                        levelsMeta.setDisplayName(CC.translate("&a&lGestionar Niveles"));
                        levelsMeta.setLore(Arrays.asList(
                                CC.translate("&7Niveles: &f" + training.getLevels().size()),
                                CC.translate("&a[CLICK PARA GESTIONAR]")
                        ));
                        levelsButton.setItemMeta(levelsMeta);
                        contents.set(1, 5, ClickableItem.of(levelsButton, e -> {
                            createLevelsMenu(trainingId, reopenMenu).open(player);
                        }));

                        ItemStack createLevelButton = new ItemStack(Material.EMERALD_BLOCK);
                        ItemMeta createLevelMeta = createLevelButton.getItemMeta();
                        createLevelMeta.setDisplayName(CC.translate("&a&lCrear Nivel"));
                        int nextLevel = training.getLevels().isEmpty() ? 1 : training.getMaxLevel() + 1;
                        createLevelMeta.setLore(Arrays.asList(
                                CC.translate("&7Siguiente nivel: &f" + nextLevel),
                                CC.translate("&a[CLICK PARA CREAR]")
                        ));
                        createLevelButton.setItemMeta(createLevelMeta);
                        contents.set(1, 6, ClickableItem.of(createLevelButton, e -> {
                            TrainingLevel level = new TrainingLevel(nextLevel);
                            training.getLevels().add(level);
                            TrainingManager.getInstance().saveTraining(training);
                            createEditLevelMenu(trainingId, nextLevel, () -> createLevelsMenu(trainingId, reopenMenu).open(player)).open(player);
                        }));

                        ItemStack deleteButton = new ItemStack(Material.ANVIL);
                        ItemMeta deleteMeta = deleteButton.getItemMeta();
                        deleteMeta.setDisplayName(CC.translate("&c&lEliminar Training"));
                        deleteMeta.setLore(Arrays.asList(
                                CC.translate("&c[CLICK PARA ELIMINAR]")
                        ));
                        deleteButton.setItemMeta(deleteMeta);
                        contents.set(1, 7, ClickableItem.of(deleteButton, e -> {
                            createDeleteConfirmMenu(trainingId, backAction).open(player);
                        }));

                        ItemStack npcButton = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
                        ItemMeta npcMeta = npcButton.getItemMeta();
                        npcMeta.setDisplayName(CC.translate("&2&lGestionar NPC's"));
                        npcMeta.setLore(Arrays.asList(
                                CC.translate("&7NPCs registrados: &f" + (training.getNpcTpBase() != null ? training.getNpcTpBase().size() : 0)),
                                CC.translate("&a[CLICK PARA GESTIONAR]")
                        ));
                        npcButton.setItemMeta(npcMeta);
                        contents.set(2, 6, ClickableItem.of(npcButton, e -> {
                            createNpcManagementMenu(trainingId, reopenMenu).open(player);
                        }));

                        ItemStack back = new ItemStack(Material.ARROW);
                        ItemMeta backMeta = back.getItemMeta();
                        backMeta.setDisplayName(CC.translate("&b← Atrás"));
                        back.setItemMeta(backMeta);
                        contents.set(2, 8, ClickableItem.of(back, e -> {
                            if (backAction != null) {
                                backAction.run();
                            } else {
                                createMainMenu(1).open(player);
                            }
                        }));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(3, 9)
                .title(CC.translate("&c&lEditar Training " + trainingId))
                .build();
    }

    public static SmartInventory createNpcManagementMenu(String trainingId) {
        return createNpcManagementMenu(trainingId, null);
    }

    public static SmartInventory createNpcManagementMenu(String trainingId, Runnable backAction) {
        Training training = TrainingManager.getInstance().getTraining(trainingId);
        if (training == null) return null;

        return SmartInventory.builder()
                .id("training_admin_npcs_" + trainingId)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane((short) 2)));

                        ItemStack info = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
                        ItemMeta infoMeta = info.getItemMeta();
                        infoMeta.setDisplayName(CC.translate("&2&lNPCs de &e" + training.getName()));
                        infoMeta.setLore(Arrays.asList(CC.translate("&7Total: &f" + (training.getNpcTpBase() != null ? training.getNpcTpBase().size() : 0))));
                        info.setItemMeta(infoMeta);
                        contents.set(0, 4, ClickableItem.empty(info));

                        Map<String, Integer> npcs = training.getNpcTpBase() != null ? training.getNpcTpBase() : new HashMap<>();
                        int slot = 10;
                        for (Map.Entry<String, Integer> entry : npcs.entrySet()) {
                            ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
                            ItemMeta meta = item.getItemMeta();
                            meta.setDisplayName(CC.translate("&e" + entry.getKey()));
                            List<String> lore = new ArrayList<>();
                            lore.add(CC.translate("&7TP Base: &f" + entry.getValue()));
                            lore.add("");
                            lore.add(CC.translate("&a[CLICK IZQUIERDO PARA EDITAR TP]"));
                            lore.add(CC.translate("&c[CLICK DERECHO PARA ELIMINAR]"));
                            meta.setLore(lore);
                            item.setItemMeta(meta);

                            String npcId = entry.getKey();
                            contents.set(slot / 9, slot % 9, ClickableItem.of(item, e -> {
                                if (e.isRightClick()) {
                                    training.removeNpc(npcId);
                                    TrainingManager.getInstance().saveTraining(training);
                                    player.sendMessage(CC.translate("&c✓ NPC &f" + npcId + " &celiminado."));
                                    createNpcManagementMenu(trainingId, backAction).open(player);
                                } else {
                                    TrainingNpcInputManager.startEditNpcTpInput(player, trainingId, npcId, () -> createNpcManagementMenu(trainingId, backAction).open(player));
                                }
                            }));
                            slot++;
                            if (slot % 9 == 8) slot += 2;
                            if (slot >= 44) break;
                        }

                        ItemStack addButton = new ItemStack(Material.EMERALD_BLOCK);
                        ItemMeta addMeta = addButton.getItemMeta();
                        addMeta.setDisplayName(CC.translate("&a&l+ Agregar NPC"));
                        addMeta.setLore(Arrays.asList(
                                CC.translate("&7Añade un nuevo NPC virtual"),
                                CC.translate("&7Formato: &f<id> <tp_base>"),
                                CC.translate("&a[CLICK PARA AGREGAR]")
                        ));
                        addButton.setItemMeta(addMeta);
                        contents.set(5, 2, ClickableItem.of(addButton, e -> {
                            TrainingNpcInputManager.startAddNpcInput(player, trainingId, () -> createNpcManagementMenu(trainingId, backAction).open(player));
                        }));

                        ItemStack back = new ItemStack(Material.ARROW);
                        ItemMeta backMeta = back.getItemMeta();
                        backMeta.setDisplayName(CC.translate("&b← Atrás"));
                        back.setItemMeta(backMeta);
                        contents.set(5, 0, ClickableItem.of(back, e -> {
                            if (backAction != null) {
                                backAction.run();
                            } else {
                                createEditTrainingMenu(trainingId).open(player);
                            }
                        }));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(6, 9)
                .title(CC.translate("&2&lNPCs de " + training.getName()))
                .build();
    }

    public static SmartInventory createLevelsMenu(String trainingId) {
        return createLevelsMenu(trainingId, null);
    }

    public static SmartInventory createLevelsMenu(String trainingId, Runnable backAction) {
        Training training = TrainingManager.getInstance().getTraining(trainingId);
        if (training == null) return null;

        return SmartInventory.builder()
                .id("training_admin_levels_" + trainingId)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane((short) 5)));

                        ItemStack info = new ItemStack(Material.BOOKSHELF);
                        ItemMeta infoMeta = info.getItemMeta();
                        infoMeta.setDisplayName(CC.translate("&a&lNiveles de &e" + training.getName()));
                        infoMeta.setLore(Arrays.asList(CC.translate("&7Total: &f" + training.getLevels().size())));
                        info.setItemMeta(infoMeta);
                        contents.set(0, 4, ClickableItem.empty(info));

                        int slot = 10;
                        for (TrainingLevel level : training.getLevels()) {
                            ItemStack item = new ItemStack(Material.EXP_BOTTLE);
                            ItemMeta meta = item.getItemMeta();
                            meta.setDisplayName(CC.translate("&eNivel " + level.getLevel()));
                            List<String> lore = new ArrayList<>();
                            lore.add(CC.translate("&7Bonus: &f" + level.getTpBonusPercent() + "%"));
                            lore.add(CC.translate("&7Costo TP: &f" + level.getTpCost()));
                            lore.add(CC.translate("&7Comandos: &f" + level.getRewardCommands().size()));
                            lore.add(CC.translate("&a[CLICK PARA EDITAR]"));
                            meta.setLore(lore);
                            item.setItemMeta(meta);
                            final int levelNumber = level.getLevel();
                            contents.set(slot / 9, slot % 9, ClickableItem.of(item, e -> {
                                createEditLevelMenu(trainingId, levelNumber, () -> createLevelsMenu(trainingId, backAction).open(player)).open(player);
                            }));
                            slot++;
                            if (slot % 9 == 8) slot += 2;
                            if (slot >= 44) break;
                        }

                        ItemStack back = new ItemStack(Material.ARROW);
                        ItemMeta backMeta = back.getItemMeta();
                        backMeta.setDisplayName(CC.translate("&b← Atrás"));
                        back.setItemMeta(backMeta);
                        contents.set(5, 0, ClickableItem.of(back, e -> {
                            if (backAction != null) {
                                backAction.run();
                            } else {
                                createEditTrainingMenu(trainingId).open(player);
                            }
                        }));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(6, 9)
                .title(CC.translate("&a&lNiveles de " + training.getName()))
                .build();
    }

    public static SmartInventory createEditLevelMenu(String trainingId, int levelNumber) {
        return createEditLevelMenu(trainingId, levelNumber, null);
    }

    public static SmartInventory createEditLevelMenu(String trainingId, int levelNumber, Runnable backAction) {
        Training training = TrainingManager.getInstance().getTraining(trainingId);
        if (training == null) return null;
        TrainingLevel level = training.getLevel(levelNumber);
        if (level == null) return null;

        return SmartInventory.builder()
                .id("training_admin_level_edit_" + trainingId + "_" + levelNumber)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane((short) 6)));

                        Runnable reopenMenu = () -> createEditLevelMenu(trainingId, levelNumber, backAction).open(player);

                        ItemStack info = new ItemStack(Material.EXP_BOTTLE);
                        ItemMeta infoMeta = info.getItemMeta();
                        infoMeta.setDisplayName(CC.translate("&6&lNivel " + levelNumber + " &7de &e" + training.getName()));
                        List<String> infoLore = new ArrayList<>();
                        infoLore.add(CC.translate("&7Bonus: &f" + level.getTpBonusPercent() + "%"));
                        infoLore.add(CC.translate("&7Costo TP: &f" + level.getTpCost()));
                        if (level.getDescription() != null && !level.getDescription().isEmpty()) {
                            infoLore.add(CC.translate("&7Descripción: &f" + level.getDescription()));
                        }
                        infoMeta.setLore(infoLore);
                        info.setItemMeta(infoMeta);
                        contents.set(0, 4, ClickableItem.empty(info));

                        ItemStack descButton = new ItemStack(Material.BOOK_AND_QUILL);
                        ItemMeta descMeta = descButton.getItemMeta();
                        descMeta.setDisplayName(CC.translate("&e&lDescripción"));
                        descMeta.setLore(Arrays.asList(
                                CC.translate("&7Actual: &f" + (level.getDescription() == null ? "Ninguna" : level.getDescription())),
                                CC.translate("&a[CLICK PARA EDITAR]")
                        ));
                        descButton.setItemMeta(descMeta);
                        contents.set(1, 1, ClickableItem.of(descButton, e -> {
                            TrainingInputManager.startInput(player, trainingId, levelNumber, TrainingInputManager.InputType.LEVEL_DESCRIPTION, reopenMenu);
                        }));

                        ItemStack tpBonusButton = new ItemStack(Material.GOLD_NUGGET);
                        ItemMeta tpBonusMeta = tpBonusButton.getItemMeta();
                        tpBonusMeta.setDisplayName(CC.translate("&6&lBonus TPs"));
                        tpBonusMeta.setLore(Arrays.asList(
                                CC.translate("&7Actual: &f" + level.getTpBonusPercent() + "%"),
                                CC.translate("&a[CLICK PARA EDITAR]")
                        ));
                        tpBonusButton.setItemMeta(tpBonusMeta);
                        contents.set(1, 2, ClickableItem.of(tpBonusButton, e -> {
                            TrainingInputManager.startInput(player, trainingId, levelNumber, TrainingInputManager.InputType.LEVEL_TP_BONUS_PERCENT, reopenMenu);
                        }));

                        ItemStack tpCostButton = new ItemStack(Material.GOLD_INGOT);
                        ItemMeta tpCostMeta = tpCostButton.getItemMeta();
                        tpCostMeta.setDisplayName(CC.translate("&b&lCosto TPs"));
                        tpCostMeta.setLore(Arrays.asList(
                                CC.translate("&7Actual: &f" + level.getTpCost()),
                                CC.translate("&a[CLICK PARA EDITAR]")
                        ));
                        tpCostButton.setItemMeta(tpCostMeta);
                        contents.set(1, 3, ClickableItem.of(tpCostButton, e -> {
                            TrainingInputManager.startInput(player, trainingId, levelNumber, TrainingInputManager.InputType.LEVEL_TP_COST, reopenMenu);
                        }));

                        ItemStack statsButton = new ItemStack(Material.REDSTONE);
                        ItemMeta statsMeta = statsButton.getItemMeta();
                        statsMeta.setDisplayName(CC.translate("&c&lCostos de Stats"));
                        List<String> statsLore = new ArrayList<>();
                        if (level.getStatCosts().isEmpty()) {
                            statsLore.add(CC.translate("&7Sin costos configurados"));
                        } else {
                            for (Map.Entry<String, Integer> entry : level.getStatCosts().entrySet()) {
                                statsLore.add(CC.translate("&f" + entry.getKey() + ": &e" + entry.getValue()));
                            }
                        }
                        statsLore.add("");
                        statsLore.add(CC.translate("&a[CLICK PARA EDITAR]"));
                        statsMeta.setLore(statsLore);
                        statsButton.setItemMeta(statsMeta);
                        contents.set(1, 4, ClickableItem.of(statsButton, e -> {
                            TrainingInputManager.startInput(player, trainingId, levelNumber, TrainingInputManager.InputType.LEVEL_STAT_COST, reopenMenu);
                        }));

                        ItemStack commandsButton = new ItemStack(Material.COMMAND);
                        ItemMeta commandsMeta = commandsButton.getItemMeta();
                        commandsMeta.setDisplayName(CC.translate("&5&lComandos"));
                        commandsMeta.setLore(Arrays.asList(
                                CC.translate("&7Total: &f" + level.getRewardCommands().size()),
                                CC.translate("&7%player% = nombre del jugador"),
                                CC.translate("&a[CLICK PARA GESTIONAR]")
                        ));
                        commandsButton.setItemMeta(commandsMeta);
                        contents.set(1, 5, ClickableItem.of(commandsButton, e -> {
                            createLevelCommandsMenu(trainingId, levelNumber, reopenMenu).open(player);
                        }));

                        ItemStack deleteButton = new ItemStack(Material.ANVIL);
                        ItemMeta deleteMeta = deleteButton.getItemMeta();
                        deleteMeta.setDisplayName(CC.translate("&c&lEliminar Nivel"));
                        deleteMeta.setLore(Arrays.asList(CC.translate("&c[CLICK PARA ELIMINAR]")));
                        deleteButton.setItemMeta(deleteMeta);
                        contents.set(1, 7, ClickableItem.of(deleteButton, e -> {
                            training.getLevels().remove(level);
                            TrainingManager.getInstance().saveTraining(training);
                            player.sendMessage(CC.translate("&c✗ Nivel " + levelNumber + " eliminado."));
                            createLevelsMenu(trainingId, backAction).open(player);
                        }));

                        ItemStack back = new ItemStack(Material.ARROW);
                        ItemMeta backMeta = back.getItemMeta();
                        backMeta.setDisplayName(CC.translate("&b← Atrás"));
                        back.setItemMeta(backMeta);
                        contents.set(2, 8, ClickableItem.of(back, e -> {
                            if (backAction != null) {
                                backAction.run();
                            } else {
                                createLevelsMenu(trainingId).open(player);
                            }
                        }));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(3, 9)
                .title(CC.translate("&c&lEditar Nivel " + levelNumber))
                .build();
    }

    public static SmartInventory createLevelCommandsMenu(String trainingId, int levelNumber) {
        return createLevelCommandsMenu(trainingId, levelNumber, null);
    }

    public static SmartInventory createLevelCommandsMenu(String trainingId, int levelNumber, Runnable backAction) {
        Training training = TrainingManager.getInstance().getTraining(trainingId);
        if (training == null) return null;
        TrainingLevel level = training.getLevel(levelNumber);
        if (level == null) return null;

        return SmartInventory.builder()
                .id("training_admin_level_commands_" + trainingId + "_" + levelNumber)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane((short) 5)));

                        Runnable reopenMenu = () -> createLevelCommandsMenu(trainingId, levelNumber, backAction).open(player);

                        ItemStack info = new ItemStack(Material.COMMAND);
                        ItemMeta infoMeta = info.getItemMeta();
                        infoMeta.setDisplayName(CC.translate("&5&lComandos Nivel " + levelNumber));
                        infoMeta.setLore(Arrays.asList(CC.translate("&7Total: &f" + level.getRewardCommands().size())));
                        info.setItemMeta(infoMeta);
                        contents.set(0, 4, ClickableItem.empty(info));

                        List<String> commands = level.getRewardCommands();
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
                                TrainingManager.getInstance().saveTraining(training);
                                reopenMenu.run();
                            }));
                            slot++;
                            if (slot % 9 == 8) slot += 2;
                        }

                        ItemStack addButton = new ItemStack(Material.EMERALD_BLOCK);
                        ItemMeta addMeta = addButton.getItemMeta();
                        addMeta.setDisplayName(CC.translate("&a&lAgregar Comando"));
                        addMeta.setLore(Arrays.asList(
                                CC.translate("&7Usa &f%player% &7para el nombre"),
                                CC.translate("&a[CLICK PARA AGREGAR]")
                        ));
                        addButton.setItemMeta(addMeta);
                        contents.set(4, 2, ClickableItem.of(addButton, e -> {
                            TrainingInputManager.startInput(player, trainingId, levelNumber, TrainingInputManager.InputType.LEVEL_ADD_COMMAND, reopenMenu);
                        }));

                        ItemStack back = new ItemStack(Material.ARROW);
                        ItemMeta backMeta = back.getItemMeta();
                        backMeta.setDisplayName(CC.translate("&b← Atrás"));
                        back.setItemMeta(backMeta);
                        contents.set(4, 6, ClickableItem.of(back, e -> {
                            if (backAction != null) {
                                backAction.run();
                            } else {
                                createEditLevelMenu(trainingId, levelNumber).open(player);
                            }
                        }));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(6, 9)
                .title(CC.translate("&5&lComandos Nivel " + levelNumber))
                .build();
    }

    public static SmartInventory createDeleteConfirmMenu(String trainingId) {
        return createDeleteConfirmMenu(trainingId, null);
    }

    public static SmartInventory createDeleteConfirmMenu(String trainingId, Runnable backAction) {
        Training training = TrainingManager.getInstance().getTraining(trainingId);
        if (training == null) return null;

        return SmartInventory.builder()
                .id("training_admin_delete_" + trainingId)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane((short) 14)));

                        ItemStack confirm = new ItemStack(Material.PAPER);
                        ItemMeta confirmMeta = confirm.getItemMeta();
                        confirmMeta.setDisplayName(CC.translate("&c&l¿Eliminar " + training.getName() + "?"));
                        confirmMeta.setLore(Arrays.asList(
                                CC.translate("&7Esta acción no se puede deshacer"),
                                CC.translate("&7ID: &f" + trainingId)
                        ));
                        confirm.setItemMeta(confirmMeta);
                        contents.set(0, 4, ClickableItem.empty(confirm));

                        ItemStack yes = new ItemStack(Material.EMERALD_BLOCK);
                        ItemMeta yesMeta = yes.getItemMeta();
                        yesMeta.setDisplayName(CC.translate("&a&lSÍ, ELIMINAR"));
                        yes.setItemMeta(yesMeta);
                        contents.set(1, 3, ClickableItem.of(yes, e -> {
                            TrainingManager.getInstance().deleteTraining(trainingId);
                            player.sendMessage(CC.translate("&c✗ Training " + trainingId + " eliminado."));
                            if (backAction != null) {
                                backAction.run();
                            } else {
                                createMainMenu(1).open(player);
                            }
                        }));

                        ItemStack no = new ItemStack(Material.REDSTONE_BLOCK);
                        ItemMeta noMeta = no.getItemMeta();
                        noMeta.setDisplayName(CC.translate("&c&lCANCELAR"));
                        no.setItemMeta(noMeta);
                        contents.set(1, 5, ClickableItem.of(no, e -> {
                            if (backAction != null) {
                                createEditTrainingMenu(trainingId, backAction).open(player);
                            } else {
                                createEditTrainingMenu(trainingId).open(player);
                            }
                        }));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(2, 9)
                .title(CC.translate("&c&lConfirmar Eliminación"))
                .build();
    }

    private static ItemStack createTrainingIcon(Training training) {
        Material material = Material.getMaterial(training.getMaterialId());
        if (material == null) material = Material.PAPER;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CC.translate((training.isVip() ? "&b" : "&e") + training.getName()));
        List<String> lore = new ArrayList<>();
        lore.add(CC.translate("&7ID: &f" + training.getId()));
        lore.add(CC.translate("&7Material: &f" + training.getMaterialId()));
        lore.add(CC.translate("&7Niveles: &f" + training.getLevels().size()));
        lore.add(CC.translate("&7VIP: &f" + (training.isVip() ? "Sí" : "No")));
        if (training.getRequiredPermission() != null && !training.getRequiredPermission().isEmpty()) {
            lore.add(CC.translate("&7Permiso: &f" + training.getRequiredPermission()));
        }
        lore.add("");
        lore.add(CC.translate("&a[CLICK PARA EDITAR]"));
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
