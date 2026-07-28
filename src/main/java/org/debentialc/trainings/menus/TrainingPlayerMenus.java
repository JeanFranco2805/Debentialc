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
import org.debentialc.trainings.TrainingClassCostFilter;
import org.debentialc.trainings.managers.TrainingManager;
import org.debentialc.trainings.model.Training;
import org.debentialc.trainings.model.TrainingLevel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TrainingPlayerMenus {

    public static SmartInventory createMainMenu(Player player) {
        return createMainMenu(player, 1);
    }

    public static SmartInventory createMainMenu(Player player, int page) {
        return SmartInventory.builder()
                .id("training_player_main_" + page)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane((short) 1)));

                        ItemStack info = new ItemStack(Material.NETHER_STAR);
                        ItemMeta infoMeta = info.getItemMeta();
                        infoMeta.setDisplayName(CC.translate("&6&lSistema de Trainings"));
                        List<String> infoLore = new ArrayList<>();
                        infoLore.add(CC.translate("&7Selecciona un training para ver sus niveles"));
                        infoMeta.setLore(infoLore);
                        info.setItemMeta(infoMeta);
                        contents.set(0, 4, ClickableItem.empty(info));

                        List<Training> normalTrainings = TrainingManager.getInstance().getNormalTrainings();
                        int pageSize = 7;
                        int totalPages = Math.max(1, (int) Math.ceil((double) normalTrainings.size() / pageSize));
                        if (page < 1) return;
                        if (page > totalPages) return;

                        int start = (page - 1) * pageSize;
                        int end = Math.min(start + pageSize, normalTrainings.size());

                        int slot = 10;
                        for (int i = start; i < end; i++) {
                            Training training = normalTrainings.get(i);
                            ItemStack item = createTrainingItem(player, training);
                            String trainingId = training.getId();
                            contents.set(slot / 9, slot % 9, ClickableItem.of(item, e -> {
                                createTrainingLevelsMenu(player, trainingId, 1).open(player);
                            }));
                            slot++;
                        }

                        if (page > 1) {
                            ItemStack prev = new ItemStack(Material.ARROW);
                            ItemMeta prevMeta = prev.getItemMeta();
                            prevMeta.setDisplayName(CC.translate("&b← Anterior"));
                            prev.setItemMeta(prevMeta);
                            contents.set(5, 2, ClickableItem.of(prev, e -> createMainMenu(player, page - 1).open(player)));
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
                            contents.set(5, 6, ClickableItem.of(next, e -> createMainMenu(player, page + 1).open(player)));
                        }

                        ItemStack vipButton = new ItemStack(Material.DIAMOND);
                        ItemMeta vipMeta = vipButton.getItemMeta();
                        vipMeta.setDisplayName(CC.translate("&b&lVIP"));
                        List<String> vipLore = new ArrayList<>();
                        vipLore.add(CC.translate("&7Trainings exclusivos VIP"));
                        vipLore.add("");
                        if (player.hasPermission("debentialc.command.training.vip")) {
                            vipLore.add(CC.translate("&a[CLICK PARA ENTRAR]"));
                        } else {
                            vipLore.add(CC.translate("&c✗ No tienes acceso"));
                        }
                        vipMeta.setLore(vipLore);
                        vipButton.setItemMeta(vipMeta);
                        contents.set(5, 8, ClickableItem.of(vipButton, e -> {
                            if (!player.hasPermission("debentialc.command.training.vip")) {
                                player.sendMessage(CC.translate("&c✗ No tienes acceso a los trainings VIP."));
                                return;
                            }
                            createVipMenu(player, 1).open(player);
                        }));

                        ItemStack closeButton = new ItemStack(Material.REDSTONE_BLOCK);
                        ItemMeta closeMeta = closeButton.getItemMeta();
                        closeMeta.setDisplayName(CC.translate("&c&lCerrar"));
                        closeButton.setItemMeta(closeMeta);
                        contents.set(5, 0, ClickableItem.of(closeButton, e -> player.closeInventory()));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(6, 9)
                .title(CC.translate("&6&lMenú de Trainings"))
                .build();
    }

    public static SmartInventory createVipMenu(Player player) {
        return createVipMenu(player, 1);
    }

    public static SmartInventory createVipMenu(Player player, int page) {
        return SmartInventory.builder()
                .id("training_player_vip_" + page)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane((short) 3)));

                        ItemStack info = new ItemStack(Material.DIAMOND);
                        ItemMeta infoMeta = info.getItemMeta();
                        infoMeta.setDisplayName(CC.translate("&b&lTrainings VIP"));
                        info.setItemMeta(infoMeta);
                        contents.set(0, 4, ClickableItem.empty(info));

                        List<Training> vipTrainings = new ArrayList<>(TrainingManager.getInstance().getVipTrainings());
                        int pageSize = 7;
                        int totalPages = Math.max(1, (int) Math.ceil((double) vipTrainings.size() / pageSize));
                        if (page < 1) return;
                        if (page > totalPages) return;

                        int start = (page - 1) * pageSize;
                        int end = Math.min(start + pageSize, vipTrainings.size());

                        int slot = 10;
                        for (int i = start; i < end; i++) {
                            Training training = vipTrainings.get(i);
                            String permission = training.getRequiredPermission();
                            boolean hasPermission = permission == null || permission.isEmpty() || player.hasPermission(permission);

                            Material material = hasPermission ? Material.GOLD_BLOCK : Material.COAL_BLOCK;
                            ItemStack item = createTrainingItem(player, training);
                            item.setType(material);
                            String trainingId = training.getId();
                            contents.set(slot / 9, slot % 9, ClickableItem.of(item, e -> {
                                if (!hasPermission) {
                                    player.sendMessage(CC.translate("&c✗ No tienes el permiso requerido."));
                                    return;
                                }
                                createTrainingLevelsMenu(player, trainingId, 1).open(player);
                            }));
                            slot++;
                        }

                        if (page > 1) {
                            ItemStack prev = new ItemStack(Material.ARROW);
                            ItemMeta prevMeta = prev.getItemMeta();
                            prevMeta.setDisplayName(CC.translate("&b← Anterior"));
                            prev.setItemMeta(prevMeta);
                            contents.set(5, 2, ClickableItem.of(prev, e -> createVipMenu(player, page - 1).open(player)));
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
                            contents.set(5, 6, ClickableItem.of(next, e -> createVipMenu(player, page + 1).open(player)));
                        }

                        ItemStack back = new ItemStack(Material.REDSTONE_BLOCK);
                        ItemMeta backMeta = back.getItemMeta();
                        backMeta.setDisplayName(CC.translate("&c← Atrás"));
                        back.setItemMeta(backMeta);
                        contents.set(5, 0, ClickableItem.of(back, e -> createMainMenu(player, 1).open(player)));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(6, 9)
                .title(CC.translate("&b&lTrainings VIP"))
                .build();
    }

    public static SmartInventory createTrainingLevelsMenu(Player player, String trainingId) {
        return createTrainingLevelsMenu(player, trainingId, 1);
    }

    public static SmartInventory createTrainingLevelsMenu(Player player, String trainingId, int page) {
        Training training = TrainingManager.getInstance().getTraining(trainingId);
        if (training == null) return createMainMenu(player, 1);

        return SmartInventory.builder()
                .id("training_player_levels_" + trainingId + "_" + page)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane((short) (training.isVip() ? 3 : 1))));

                        int currentLevel = TrainingManager.getInstance().getPlayerTrainingLevel(player, trainingId);
                        double multiplier = TrainingManager.getInstance().getTrainingMultiplier(player, trainingId);

                        ItemStack info = new ItemStack(Material.getMaterial(training.getMaterialId()) != null ? Material.getMaterial(training.getMaterialId()) : Material.PAPER);
                        ItemMeta infoMeta = info.getItemMeta();
                        infoMeta.setDisplayName(CC.translate((training.isVip() ? "&b" : "&6") + training.getName()));
                        List<String> infoLore = new ArrayList<>();
                        infoLore.add(CC.translate("&7Nivel actual: &e" + currentLevel));
                        infoLore.add(CC.translate("&7Potenciador TP: &ex" + String.format("%.2f", multiplier)));
                        infoMeta.setLore(infoLore);
                        info.setItemMeta(infoMeta);
                        contents.set(0, 4, ClickableItem.empty(info));

                        List<TrainingLevel> levels = new ArrayList<>(training.getLevels());
                        int pageSize = 21;
                        int totalPages = Math.max(1, (int) Math.ceil((double) levels.size() / pageSize));
                        if (page < 1) return;
                        if (page > totalPages) return;

                        int start = (page - 1) * pageSize;
                        int end = Math.min(start + pageSize, levels.size());

                        int row = 1;
                        int col = 1;
                        for (int i = start; i < end; i++) {
                            TrainingLevel level = levels.get(i);
                            boolean unlocked = level.getLevel() <= currentLevel;
                            boolean isNext = level.getLevel() == currentLevel + 1;

                            Material material = unlocked ? Material.EMERALD_BLOCK : (isNext ? Material.GOLD_BLOCK : Material.COAL_BLOCK);
                            ItemStack item = new ItemStack(material);
                            ItemMeta meta = item.getItemMeta();
                            meta.setDisplayName(CC.translate((unlocked ? "&a" : isNext ? "&6" : "&7") + "Nivel " + level.getLevel()));

                            List<String> lore = new ArrayList<>();
                            if (level.getDescription() != null && !level.getDescription().isEmpty()) {
                                lore.add(CC.translate("&7" + level.getDescription()));
                            }
                            lore.add(CC.translate("&7Bonus TP: &f" + level.getTpBonusPercent() + "%"));
                            if (level.getTpCost() > 0) {
                                lore.add(CC.translate("&7Costo TP: &f" + level.getTpCost()));
                            }

                            Map<String, Integer> filteredCosts = TrainingClassCostFilter.filterCosts(level.getStatCosts(), player);
                            if (!filteredCosts.isEmpty()) {
                                String playerClass = TrainingClassCostFilter.getPlayerClass(player);
                                String className = playerClass != null && !playerClass.isEmpty() ? playerClass : "Todos";
                                lore.add(CC.translate("&7Costos (" + className + "):"));
                                for (Map.Entry<String, Integer> entry : filteredCosts.entrySet()) {
                                    lore.add(CC.translate("&f  " + entry.getKey() + ": &e" + entry.getValue()));
                                }
                            }

                            if (!level.getRewardCommands().isEmpty()) {
                                lore.add(CC.translate("&7Comandos:"));
                                for (String cmd : level.getRewardCommands()) {
                                    lore.add(CC.translate("&f  " + cmd));
                                }
                            }

                            lore.add("");
                            if (unlocked) {
                                lore.add(CC.translate("&a✓ DESBLOQUEADO"));
                            } else if (isNext) {
                                lore.add(CC.translate("&e⚡ Click para subir de nivel"));
                            } else {
                                lore.add(CC.translate("&c✗ BLOQUEADO"));
                            }

                            meta.setLore(lore);
                            item.setItemMeta(meta);

                            contents.set(row, col, ClickableItem.of(item, e -> {
                                if (!isNext) {
                                    if (unlocked) {
                                        player.sendMessage(CC.translate("&a✓ Ya tienes este nivel desbloqueado."));
                                    } else {
                                        player.sendMessage(CC.translate("&c✗ Debes desbloquear los niveles anteriores."));
                                    }
                                    return;
                                }
                                TrainingManager.getInstance().upgrade(player, trainingId);
                                createTrainingLevelsMenu(player, trainingId, page).open(player);
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
                            contents.set(4, 2, ClickableItem.of(prev, e -> createTrainingLevelsMenu(player, trainingId, page - 1).open(player)));
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
                            contents.set(4, 6, ClickableItem.of(next, e -> createTrainingLevelsMenu(player, trainingId, page + 1).open(player)));
                        }

                        ItemStack back = new ItemStack(Material.REDSTONE_BLOCK);
                        ItemMeta backMeta = back.getItemMeta();
                        backMeta.setDisplayName(CC.translate("&c← Atrás"));
                        back.setItemMeta(backMeta);
                        contents.set(4, 8, ClickableItem.of(back, e -> {
                            if (training.isVip()) {
                                createVipMenu(player, 1).open(player);
                            } else {
                                createMainMenu(player, 1).open(player);
                            }
                        }));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(6, 9)
                .title(CC.translate("&6&l" + training.getName()))
                .build();
    }

    private static ItemStack createTrainingItem(Player player, Training training) {
        Material material = Material.getMaterial(training.getMaterialId());
        if (material == null) material = Material.PAPER;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CC.translate((training.isVip() ? "&b" : "&e") + training.getName()));

        int currentLevel = TrainingManager.getInstance().getPlayerTrainingLevel(player, training.getId());
        double multiplier = TrainingManager.getInstance().getTrainingMultiplier(player, training.getId());

        List<String> lore = new ArrayList<>();
        lore.add(CC.translate("&7Nivel actual: &e" + currentLevel));
        lore.add(CC.translate("&7Potenciador TP: &ex" + String.format("%.2f", multiplier)));

        TrainingLevel nextLevel = training.getLevel(currentLevel + 1);
        if (nextLevel != null) {
            lore.add(CC.translate("&7Costo siguiente nivel:"));
            if (nextLevel.getTpCost() > 0) {
                lore.add(CC.translate("&f  TPs: &e" + nextLevel.getTpCost()));
            }
            Map<String, Integer> filteredCosts = TrainingClassCostFilter.filterCosts(nextLevel.getStatCosts(), player);
            for (Map.Entry<String, Integer> entry : filteredCosts.entrySet()) {
                lore.add(CC.translate("&f  " + entry.getKey() + ": &e" + entry.getValue()));
            }
        } else {
            lore.add(CC.translate("&a✓ Nivel máximo alcanzado"));
        }

        lore.add("");
        lore.add(CC.translate("&a[CLICK PARA VER NIVELES]"));
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
