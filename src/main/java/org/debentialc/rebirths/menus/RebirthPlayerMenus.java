package org.debentialc.rebirths.menus;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.debentialc.rebirths.managers.RebirthBlockManager;
import org.debentialc.rebirths.managers.RebirthManager;
import org.debentialc.rebirths.model.Rebirth;
import org.debentialc.rebirths.model.RebirthBlock;
import org.debentialc.service.CC;

import java.util.ArrayList;
import java.util.List;

public class RebirthPlayerMenus {

    public static SmartInventory createMainMenu(Player player) {
        return createMainMenu(player, 1);
    }

    public static SmartInventory createMainMenu(Player player, int page) {
        return SmartInventory.builder()
                .id("rebirth_player_main_" + page)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane((short) 1)));

                        int currentLevel = RebirthManager.getInstance().getPlayerRebirthLevel(player);

                        ItemStack info = new ItemStack(Material.NETHER_STAR);
                        ItemMeta infoMeta = info.getItemMeta();
                        infoMeta.setDisplayName(CC.translate("&6&lSistema de Rebirths"));
                        List<String> infoLore = new ArrayList<>();
                        infoLore.add(CC.translate("&7Tu rebirth actual: &e" + currentLevel));
                        infoLore.add(CC.translate("&7Potenciador global: &e" + String.format("%.2f", (RebirthManager.getInstance().getRebirthMultiplier(player) - 1.0) * 100.0) + "%"));
                        infoLore.add(CC.translate("&7Selecciona un bloque para ver sus rebirths."));
                        infoMeta.setLore(infoLore);
                        info.setItemMeta(infoMeta);
                        contents.set(0, 4, ClickableItem.empty(info));

                        List<RebirthBlock> normalBlocks = new ArrayList<>(RebirthBlockManager.getInstance().getNormalBlocks());
                        int pageSize = 7;
                        int totalPages = Math.max(1, (int) Math.ceil((double) normalBlocks.size() / pageSize));
                        if (page < 1) return;
                        if (page > totalPages) return;

                        int start = (page - 1) * pageSize;
                        int end = Math.min(start + pageSize, normalBlocks.size());

                        int slot = 10;
                        for (int i = start; i < end; i++) {
                            RebirthBlock block = normalBlocks.get(i);
                            boolean available = RebirthBlockManager.getInstance().isBlockAvailable(player, block);
                            boolean completed = RebirthBlockManager.getInstance().isBlockCompleted(player, block);

                            Material material;
                            if (completed) {
                                material = Material.EMERALD_BLOCK;
                            } else if (available) {
                                material = Material.GOLD_BLOCK;
                            } else {
                                material = Material.COAL_BLOCK;
                            }

                            ItemStack item = new ItemStack(material);
                            ItemMeta meta = item.getItemMeta();
                            meta.setDisplayName(CC.translate((completed ? "&a" : available ? "&6" : "&7") + block.getName()));

                            List<String> lore = new ArrayList<>();
                            lore.add(CC.translate("&7Rebirths: &f" + block.getRebirthIds().size()));

                            appendBlockStatsLore(block, lore);
                            lore.add(CC.translate("&7Progreso: &e" + countUnlocked(player, block) + "&7/&f" + block.getRebirthIds().size()));
                            lore.add("");

                            if (completed) {
                                lore.add(CC.translate("&a✓ BLOQUE COMPLETADO"));
                            } else if (available) {
                                lore.add(CC.translate("&e⚡ Click para entrar"));
                            } else {
                                lore.add(CC.translate("&c✗ Completa el bloque anterior"));
                            }

                            meta.setLore(lore);
                            item.setItemMeta(meta);

                            String blockId = block.getId();
                            contents.set(slot / 9, slot % 9, ClickableItem.of(item, e -> {
                                if (!available && !completed) {
                                    player.sendMessage(CC.translate("&c✗ Debes completar el bloque anterior primero."));
                                    return;
                                }
                                createBlockRebirthsMenu(player, blockId, 1).open(player);
                            }));

                            slot++;
                            if (slot % 9 == 8) slot += 2;
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
                        vipLore.add(CC.translate("&7Rebirths exclusivos VIP"));
                        vipLore.add("");
                        if (player.hasPermission(org.debentialc.customitems.tools.permissions.Permissions.COMMAND + "rebirth.vip")) {
                            vipLore.add(CC.translate("&a[CLICK PARA ENTRAR]"));
                        } else {
                            vipLore.add(CC.translate("&c✗ No tienes acceso"));
                        }
                        vipMeta.setLore(vipLore);
                        vipButton.setItemMeta(vipMeta);
                        contents.set(5, 8, ClickableItem.of(vipButton, e -> {
                            if (!player.hasPermission(org.debentialc.customitems.tools.permissions.Permissions.COMMAND + "rebirth.vip")) {
                                player.sendMessage(CC.translate("&c✗ No tienes acceso a los rebirths VIP."));
                                return;
                            }
                            createVipBlocksMenu(player).open(player);
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
                .title(CC.translate("&6&lMenú de Rebirths"))
                .build();
    }

    public static SmartInventory createVipBlocksMenu(Player player) {
        return SmartInventory.builder()
                .id("rebirth_player_vip_main")
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane((short) 3)));

                        int currentLevel = RebirthManager.getInstance().getPlayerRebirthLevel(player);

                        ItemStack info = new ItemStack(Material.DIAMOND);
                        ItemMeta infoMeta = info.getItemMeta();
                        infoMeta.setDisplayName(CC.translate("&b&lRebirths VIP"));
                        List<String> infoLore = new ArrayList<>();
                        infoLore.add(CC.translate("&7Tu rebirth actual: &e" + currentLevel));
                        infoLore.add(CC.translate("&7Selecciona un bloque VIP para ver sus rebirths."));
                        infoMeta.setLore(infoLore);
                        info.setItemMeta(infoMeta);
                        contents.set(0, 4, ClickableItem.empty(info));

                        int slot = 10;
                        for (RebirthBlock block : RebirthBlockManager.getInstance().getVipBlocks()) {
                            boolean available = RebirthBlockManager.getInstance().isBlockAvailable(player, block);
                            boolean completed = RebirthBlockManager.getInstance().isBlockCompleted(player, block);
                            boolean hasPermission = block.getRequiredPermission() == null || block.getRequiredPermission().isEmpty() || player.hasPermission(block.getRequiredPermission());

                            Material material;
                            if (completed) {
                                material = Material.EMERALD_BLOCK;
                            } else if (available && hasPermission) {
                                material = Material.GOLD_BLOCK;
                            } else {
                                material = Material.COAL_BLOCK;
                            }

                            ItemStack item = new ItemStack(material);
                            ItemMeta meta = item.getItemMeta();
                            meta.setDisplayName(CC.translate((completed ? "&a" : available && hasPermission ? "&6" : "&7") + block.getName()));

                            List<String> lore = new ArrayList<>();
                            lore.add(CC.translate("&7Rebirths: &f" + block.getRebirthIds().size()));
                            if (block.getRequiredPermission() != null && !block.getRequiredPermission().isEmpty()) {
                                lore.add(CC.translate("&7Permiso: &f" + block.getRequiredPermission()));
                                lore.add(CC.translate(hasPermission ? "&a✓ Tienes acceso" : "&c✗ No tienes acceso"));
                            }

                            appendBlockStatsLore(block, lore);
                            lore.add(CC.translate("&7Progreso: &e" + countUnlocked(player, block) + "&7/&f" + block.getRebirthIds().size()));
                            lore.add("");

                            if (completed) {
                                lore.add(CC.translate("&a✓ BLOQUE COMPLETADO"));
                            } else if (!hasPermission) {
                                lore.add(CC.translate("&c✗ No tienes el permiso requerido"));
                            } else if (available) {
                                lore.add(CC.translate("&e⚡ Click para entrar"));
                            } else {
                                lore.add(CC.translate("&c✗ Completa el bloque anterior"));
                            }

                            meta.setLore(lore);
                            item.setItemMeta(meta);

                            String blockId = block.getId();
                            contents.set(slot / 9, slot % 9, ClickableItem.of(item, e -> {
                                if (!hasPermission) {
                                    player.sendMessage(CC.translate("&c✗ No tienes el permiso requerido para este bloque VIP."));
                                    return;
                                }
                                if (!available && !completed) {
                                    player.sendMessage(CC.translate("&c✗ Debes completar el bloque anterior primero."));
                                    return;
                                }
                                createVipBlockRebirthsMenu(player, blockId, 1).open(player);
                            }));

                            slot++;
                            if (slot % 9 == 8) slot += 2;
                            if (slot >= 44) break;
                        }

                        ItemStack backButton = new ItemStack(Material.REDSTONE_BLOCK);
                        ItemMeta backMeta = backButton.getItemMeta();
                        backMeta.setDisplayName(CC.translate("&c← Atrás"));
                        backButton.setItemMeta(backMeta);
                        contents.set(5, 4, ClickableItem.of(backButton, e -> createMainMenu(player).open(player)));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(6, 9)
                .title(CC.translate("&b&lRebirths VIP"))
                .build();
    }

    public static SmartInventory createBlockRebirthsMenu(Player player, String blockId, int page) {
        RebirthBlock block = RebirthBlockManager.getInstance().getBlock(blockId);
        if (block == null) return createMainMenu(player);

        return SmartInventory.builder()
                .id("rebirth_player_block_" + blockId + "_" + page)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane((short) 1)));

                        int currentLevel = RebirthManager.getInstance().getPlayerRebirthLevel(player);

                        ItemStack info = new ItemStack(Material.NETHER_STAR);
                        ItemMeta infoMeta = info.getItemMeta();
                        infoMeta.setDisplayName(CC.translate("&6&l" + block.getName()));
                        List<String> infoLore = new ArrayList<>();
                        infoLore.add(CC.translate("&7Rebirths: &f" + block.getRebirthIds().size()));
                        infoLore.add(CC.translate("&7Progreso: &e" + countUnlocked(player, block) + "&7/&f" + block.getRebirthIds().size()));
                        infoMeta.setLore(infoLore);
                        info.setItemMeta(infoMeta);
                        contents.set(0, 4, ClickableItem.empty(info));

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
                            boolean unlocked = RebirthManager.getInstance().hasRebirth(player, rebirth.getId());
                            boolean canUnlock = !unlocked && RebirthManager.getInstance().canUnlockRebirth(player, rebirth);

                            Material material = unlocked ? Material.EMERALD_BLOCK : (canUnlock ? Material.GOLD_BLOCK : Material.COAL_BLOCK);
                            ItemStack item = new ItemStack(material);
                            ItemMeta meta = item.getItemMeta();
                            meta.setDisplayName(CC.translate((unlocked ? "&a" : canUnlock ? "&6" : "&7") + rebirth.getDisplayName()));

                            List<String> lore = new ArrayList<>();
                            lore.add(CC.translate("&7Nivel requerido: &f" + rebirth.getRequiredLevel()));
                            lore.add(CC.translate("&7Bonus TPs: &f" + rebirth.getTpBonusPercent() + "%"));
                            lore.add(CC.translate("&7Zonas: &f" + (rebirth.getAllowedRegions().isEmpty() ? "Ninguna" : String.join(", ", rebirth.getAllowedRegions()))));
                            lore.add("");

                            if (unlocked) {
                                lore.add(CC.translate("&a✓ DESBLOQUEADO"));
                            } else if (canUnlock) {
                                lore.add(CC.translate("&e⚡ Click para desbloquear"));
                            } else {
                                lore.add(CC.translate("&c✗ BLOQUEADO"));
                                lore.add(CC.translate("&7Necesitas nivel &f" + rebirth.getRequiredLevel()));
                            }

                            meta.setLore(lore);
                            item.setItemMeta(meta);

                            contents.set(row, col, ClickableItem.of(item, e -> {
                                if (unlocked) {
                                    player.sendMessage(CC.translate("&a✓ Ya tienes este rebirth desbloqueado."));
                                    return;
                                }
                                if (!canUnlock) {
                                    int playerLevel = org.debentialc.service.General.getLVL(player);
                                    player.sendMessage("");
                                    player.sendMessage(CC.translate("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
                                    player.sendMessage(CC.translate("&c✗ No cumples los requisitos para &e" + rebirth.getDisplayName()));
                                    player.sendMessage(CC.translate("&7Tu nivel actual: &f" + playerLevel));
                                    player.sendMessage(CC.translate("&7Nivel requerido: &f" + rebirth.getRequiredLevel()));
                                    player.sendMessage(CC.translate("&7Rebirth actual: &f" + currentLevel));
                                    player.sendMessage(CC.translate("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
                                    player.sendMessage("");
                                    return;
                                }
                                player.closeInventory();
                                RebirthUnlockHandler.unlockRebirth(player, rebirth);
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
                            contents.set(4, 2, ClickableItem.of(prev, e -> createBlockRebirthsMenu(player, blockId, page - 1).open(player)));
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
                            contents.set(4, 6, ClickableItem.of(next, e -> createBlockRebirthsMenu(player, blockId, page + 1).open(player)));
                        }

                        ItemStack back = new ItemStack(Material.REDSTONE_BLOCK);
                        ItemMeta backMeta = back.getItemMeta();
                        backMeta.setDisplayName(CC.translate("&c← Atrás"));
                        back.setItemMeta(backMeta);
                        contents.set(4, 8, ClickableItem.of(back, e -> createMainMenu(player).open(player)));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(6, 9)
                .title(CC.translate("&6&l" + block.getName()))
                .build();
    }

    public static SmartInventory createVipBlockRebirthsMenu(Player player, String blockId, int page) {
        RebirthBlock block = RebirthBlockManager.getInstance().getBlock(blockId);
        if (block == null || !block.isVip()) return createVipBlocksMenu(player);

        return SmartInventory.builder()
                .id("rebirth_player_vip_block_" + blockId + "_" + page)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane((short) 3)));

                        int currentLevel = RebirthManager.getInstance().getPlayerRebirthLevel(player);

                        ItemStack info = new ItemStack(Material.DIAMOND);
                        ItemMeta infoMeta = info.getItemMeta();
                        infoMeta.setDisplayName(CC.translate("&b&l" + block.getName()));
                        List<String> infoLore = new ArrayList<>();
                        infoLore.add(CC.translate("&7Rebirths: &f" + block.getRebirthIds().size()));
                        infoLore.add(CC.translate("&7Progreso: &e" + countUnlocked(player, block) + "&7/&f" + block.getRebirthIds().size()));
                        infoMeta.setLore(infoLore);
                        info.setItemMeta(infoMeta);
                        contents.set(0, 4, ClickableItem.empty(info));

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
                            boolean unlocked = RebirthManager.getInstance().hasRebirth(player, rebirth.getId());
                            boolean canUnlock = !unlocked && RebirthManager.getInstance().canUnlockRebirth(player, rebirth);

                            Material material = unlocked ? Material.EMERALD_BLOCK : (canUnlock ? Material.GOLD_BLOCK : Material.COAL_BLOCK);
                            ItemStack item = new ItemStack(material);
                            ItemMeta meta = item.getItemMeta();
                            meta.setDisplayName(CC.translate((unlocked ? "&a" : canUnlock ? "&6" : "&7") + rebirth.getDisplayName()));

                            List<String> lore = new ArrayList<>();
                            lore.add(CC.translate("&7Nivel requerido: &f" + rebirth.getRequiredLevel()));
                            lore.add(CC.translate("&7Bonus TPs: &f" + rebirth.getTpBonusPercent() + "%"));
                            lore.add(CC.translate("&7Zonas: &f" + (rebirth.getAllowedRegions().isEmpty() ? "Ninguna" : String.join(", ", rebirth.getAllowedRegions()))));
                            lore.add("");

                            if (unlocked) {
                                lore.add(CC.translate("&a✓ DESBLOQUEADO"));
                            } else if (canUnlock) {
                                lore.add(CC.translate("&e⚡ Click para desbloquear"));
                            } else {
                                lore.add(CC.translate("&c✗ BLOQUEADO"));
                                lore.add(CC.translate("&7Necesitas nivel &f" + rebirth.getRequiredLevel()));
                            }

                            meta.setLore(lore);
                            item.setItemMeta(meta);

                            contents.set(row, col, ClickableItem.of(item, e -> {
                                if (unlocked) {
                                    player.sendMessage(CC.translate("&a✓ Ya tienes este rebirth desbloqueado."));
                                    return;
                                }
                                if (!canUnlock) {
                                    int playerLevel = org.debentialc.service.General.getLVL(player);
                                    player.sendMessage("");
                                    player.sendMessage(CC.translate("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
                                    player.sendMessage(CC.translate("&c✗ No cumples los requisitos para &e" + rebirth.getDisplayName()));
                                    player.sendMessage(CC.translate("&7Tu nivel actual: &f" + playerLevel));
                                    player.sendMessage(CC.translate("&7Nivel requerido: &f" + rebirth.getRequiredLevel()));
                                    player.sendMessage(CC.translate("&7Rebirth actual: &f" + currentLevel));
                                    player.sendMessage(CC.translate("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
                                    player.sendMessage("");
                                    return;
                                }
                                player.closeInventory();
                                RebirthUnlockHandler.unlockRebirth(player, rebirth);
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
                            contents.set(4, 2, ClickableItem.of(prev, e -> createVipBlockRebirthsMenu(player, blockId, page - 1).open(player)));
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
                            contents.set(4, 6, ClickableItem.of(next, e -> createVipBlockRebirthsMenu(player, blockId, page + 1).open(player)));
                        }

                        ItemStack back = new ItemStack(Material.REDSTONE_BLOCK);
                        ItemMeta backMeta = back.getItemMeta();
                        backMeta.setDisplayName(CC.translate("&c← Atrás"));
                        back.setItemMeta(backMeta);
                        contents.set(4, 8, ClickableItem.of(back, e -> createVipBlocksMenu(player).open(player)));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(6, 9)
                .title(CC.translate("&b&l" + block.getName()))
                .build();
    }

    private static int countUnlocked(Player player, RebirthBlock block) {
        int count = 0;
        for (int rebirthId : block.getRebirthIds()) {
            if (RebirthManager.getInstance().hasRebirth(player, rebirthId)) count++;
        }
        return count;
    }

    private static void appendBlockStatsLore(RebirthBlock block, List<String> lore) {
        double tpBonus = 0.0;
        double addBonus = 0.0;
        double mulBonus = 1.0;

        for (int rebirthId : block.getRebirthIds()) {
            Rebirth rebirth = RebirthManager.getInstance().getRebirth(rebirthId);
            if (rebirth == null) continue;

            tpBonus += rebirth.getTpBonusPercent();

            double value = rebirth.getStatBonusMultiplier();
            if (value <= 0) continue;

            String operation = rebirth.getStatBonusOperation();
            if ("+".equals(operation)) {
                addBonus += value;
            } else if ("*".equals(operation)) {
                mulBonus *= value;
            }
        }

        lore.add(CC.translate("&7Potenciador TP: &e" + String.format("%.2f", tpBonus) + "%"));
        if (addBonus > 0) {
            String valueText = addBonus == Math.floor(addBonus) ? String.valueOf((int) addBonus) : String.format("%.2f", addBonus);
            lore.add(CC.translate("&7Stats: &a+" + valueText));
        }
        if (mulBonus > 1.0) {
            lore.add(CC.translate("&7Stats: &a×" + String.format("%.2f", mulBonus)));
        }
    }

    private static ItemStack createGlassPane(short color) {
        ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, color);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(CC.translate("&8"));
        glass.setItemMeta(meta);
        return glass;
    }
}
