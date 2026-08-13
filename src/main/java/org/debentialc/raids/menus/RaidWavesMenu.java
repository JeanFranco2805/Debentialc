package org.debentialc.raids.menus;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.debentialc.raids.managers.RaidManager;
import org.debentialc.raids.managers.RaidStorageManager;
import org.debentialc.raids.models.Raid;
import org.debentialc.raids.models.Wave;
import org.debentialc.service.CC;

import java.util.*;

/**
 * Menú visual para gestionar oleadas de una raid
 */
public class RaidWavesMenu {

    public static SmartInventory createWavesMenu(String raidId) {
        Raid raid = RaidManager.getRaidById(raidId);
        if (raid == null) return null;

        return SmartInventory.builder()
                .id("raid_waves_" + raidId)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(RaidMainMenu.createGlassPane((short) 4)));

                        // TÍTULO
                        ItemStack titleItem = new ItemStack(Material.BLAZE_POWDER);
                        ItemMeta titleMeta = titleItem.getItemMeta();
                        titleMeta.setDisplayName(CC.translate("&6&lOleadas - " + raid.getDisplayName()));
                        titleMeta.setLore(Arrays.asList(
                                CC.translate("&7Total: &f" + raid.getTotalWaves() + " oleadas")
                        ));
                        titleItem.setItemMeta(titleMeta);
                        contents.set(0, 4, ClickableItem.empty(titleItem));

                        // OLEADAS EXISTENTES
                        if (raid.getTotalWaves() == 0) {
                            ItemStack emptyItem = new ItemStack(Material.ANVIL);
                            ItemMeta emptyMeta = emptyItem.getItemMeta();
                            emptyMeta.setDisplayName(CC.translate("&cNo hay oleadas"));
                            emptyMeta.setLore(Arrays.asList(
                                    CC.translate("&7Agrega oleadas con el botón verde")
                            ));
                            emptyItem.setItemMeta(emptyMeta);
                            contents.set(2, 4, ClickableItem.empty(emptyItem));
                        } else {
                            int row = 1;
                            int col = 1;

                            for (int i = 0; i < raid.getTotalWaves(); i++) {
                                Wave wave = raid.getWaveByIndex(i);
                                final int waveIndex = i;

                                ItemStack waveItem = new ItemStack(Material.MAGMA_CREAM);
                                ItemMeta waveMeta = waveItem.getItemMeta();
                                waveMeta.setDisplayName(CC.translate("&6&lOleada " + wave.getWaveNumber()));

                                List<String> waveLore = new ArrayList<>();
                                waveLore.add(CC.translate("&7Enemigos: &f" + wave.getTotalEnemies()));
                                waveLore.add(CC.translate("&7Puntos de spawn: &f" + wave.getSpawnPoints().size()));
                                waveLore.add(CC.translate("&7Recompensas: &f" + wave.getRewards().size()));
                                waveLore.add("");
                                waveLore.add(CC.translate("&a[CLICK IZQUIERDO] &7Editar"));
                                waveLore.add(CC.translate("&c[CLICK DERECHO] &7Eliminar"));

                                waveMeta.setLore(waveLore);
                                waveItem.setItemMeta(waveMeta);

                                contents.set(row, col, ClickableItem.of(waveItem, e -> {
                                    if (e.isRightClick()) {
                                        // Eliminar oleada
                                        raid.getWaves().remove(waveIndex);
                                        RaidManager.updateRaid(raid);
                                        RaidStorageManager.saveRaid(raid);
                                        player.sendMessage(CC.translate("&c✗ Oleada " + wave.getWaveNumber() + " eliminada"));
                                        createWavesMenu(raidId).open(player);
                                    } else {
                                        // Editar oleada
                                        RaidWaveConfigMenu.createWaveConfigMenu(raidId, waveIndex).open(player);
                                    }
                                }));

                                col++;
                                if (col >= 8) {
                                    col = 1;
                                    row++;
                                    if (row >= 4) break;
                                }
                            }
                        }

                        // AGREGAR OLEADA
                        ItemStack addButton = new ItemStack(Material.EMERALD_BLOCK);
                        ItemMeta addMeta = addButton.getItemMeta();
                        addMeta.setDisplayName(CC.translate("&a&lAgregar Oleada"));
                        addMeta.setLore(Arrays.asList(
                                CC.translate("&7Crea una nueva oleada"),
                                CC.translate("&7Número: &f" + (raid.getTotalWaves() + 1)),
                                "",
                                CC.translate("&a[CLICK PARA AGREGAR]")
                        ));
                        addButton.setItemMeta(addMeta);
                        contents.set(4, 4, ClickableItem.of(addButton, e -> {
                            Wave newWave = new Wave(raid.getTotalWaves() + 1);
                            raid.addWave(newWave);
                            RaidManager.updateRaid(raid);
                            RaidStorageManager.saveRaid(raid);

                            player.sendMessage("");
                            player.sendMessage(CC.translate("&a✓ Oleada " + newWave.getWaveNumber() + " creada exitosamente"));
                            player.sendMessage("");
                            org.bukkit.Bukkit.getScheduler().scheduleSyncDelayedTask(
                                    org.debentialc.Main.instance,
                                    () -> createWavesMenu(raidId).open(player),
                                    1L
                            );
                        }));

                        // ATRÁS
                        ItemStack backButton = new ItemStack(Material.ARROW);
                        ItemMeta backMeta = backButton.getItemMeta();
                        backMeta.setDisplayName(CC.translate("&b← Atrás"));
                        backButton.setItemMeta(backMeta);
                        contents.set(4, 0, ClickableItem.of(backButton, e -> {
                            RaidConfigMenu.createRaidConfigMenu(raidId).open(player);
                        }));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(5, 9)
                .title(CC.translate("&6&lOleadas"))
                .build();
    }
}