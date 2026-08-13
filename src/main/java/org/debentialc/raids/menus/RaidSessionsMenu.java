package org.debentialc.raids.menus;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.debentialc.raids.managers.RaidSessionManager;
import org.debentialc.raids.models.RaidSession;
import org.debentialc.service.CC;

import java.util.*;

/**
 * Menú visual para ver sesiones activas de raids
 */
public class RaidSessionsMenu {

    public static SmartInventory createSessionsMenu() {
        return SmartInventory.builder()
                .id("raid_sessions")
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(RaidMainMenu.createGlassPane((short) 14)));

                        // TÍTULO
                        ItemStack titleItem = new ItemStack(Material.REDSTONE_TORCH_ON);
                        ItemMeta titleMeta = titleItem.getItemMeta();
                        titleMeta.setDisplayName(CC.translate("&c&lSesiones Activas"));
                        titleMeta.setLore(Arrays.asList(
                                CC.translate("&7Total: &f" + RaidSessionManager.getTotalActiveSessions()),
                                CC.translate("&7Jugadores en raids: &f" + RaidSessionManager.getTotalPlayersInRaids())
                        ));
                        titleItem.setItemMeta(titleMeta);
                        contents.set(0, 4, ClickableItem.empty(titleItem));

                        Collection<RaidSession> sessions = RaidSessionManager.getAllActiveSessions();

                        if (sessions.isEmpty()) {
                            ItemStack emptyItem = new ItemStack(Material.ANVIL);
                            ItemMeta emptyMeta = emptyItem.getItemMeta();
                            emptyMeta.setDisplayName(CC.translate("&cNo hay sesiones activas"));
                            emptyMeta.setLore(Arrays.asList(
                                    CC.translate("&7Las sesiones aparecerán aquí"),
                                    CC.translate("&7cuando una party inicie una raid")
                            ));
                            emptyItem.setItemMeta(emptyMeta);
                            contents.set(2, 4, ClickableItem.empty(emptyItem));
                        } else {
                            int row = 1;
                            int col = 1;

                            for (RaidSession session : sessions) {
                                ItemStack sessionItem = new ItemStack(Material.EYE_OF_ENDER);
                                ItemMeta sessionMeta = sessionItem.getItemMeta();
                                sessionMeta.setDisplayName(CC.translate("&6&l" + session.getRaid().getDisplayName()));

                                List<String> lore = new ArrayList<>();
                                lore.add(CC.translate("&7Sesión: &f" + session.getSessionId()));
                                lore.add(CC.translate("&7Oleada: &f" + (session.getCurrentWaveIndex() + 1) + "/" + session.getRaid().getTotalWaves()));
                                lore.add(CC.translate("&7Progreso: &f" + session.getProgress() + "%"));
                                lore.add(CC.translate("&7Jugadores vivos: &f" + session.getActivePlayers().size()));
                                lore.add(CC.translate("&7Jugadores muertos: &f" + session.getDeadPlayers().size()));
                                lore.add(CC.translate("&7Duración: &f" + session.getDurationSeconds() + "s"));
                                lore.add(CC.translate("&7Estado: &f" + session.getStatus().getDisplayName()));
                                lore.add("");
                                lore.add(CC.translate("&c[CLICK] &7Forzar finalización"));

                                sessionMeta.setLore(lore);
                                sessionItem.setItemMeta(sessionMeta);

                                final String sessionId = session.getSessionId();
                                contents.set(row, col, ClickableItem.of(sessionItem, e -> {
                                    RaidSessionManager.removeSession(sessionId);
                                    player.sendMessage(CC.translate("&c✗ Sesión forzada a terminar"));
                                    createSessionsMenu().open(player);
                                }));

                                col++;
                                if (col >= 8) {
                                    col = 1;
                                    row++;
                                    if (row >= 4) break;
                                }
                            }
                        }

                        // ATRÁS
                        ItemStack backButton = new ItemStack(Material.ARROW);
                        ItemMeta backMeta = backButton.getItemMeta();
                        backMeta.setDisplayName(CC.translate("&b← Atrás"));
                        backButton.setItemMeta(backMeta);
                        contents.set(4, 4, ClickableItem.of(backButton, e -> {
                            RaidMainMenu.createMainMenu().open(player);
                        }));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(5, 9)
                .title(CC.translate("&c&lSesiones Activas"))
                .build();
    }
}