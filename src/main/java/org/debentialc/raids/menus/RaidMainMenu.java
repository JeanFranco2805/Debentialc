package org.debentialc.raids.menus;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.debentialc.raids.managers.RaidCategoryManager;
import org.debentialc.raids.managers.RaidManager;
import org.debentialc.raids.managers.RaidSessionManager;
import org.debentialc.raids.managers.PartyManager;
import org.debentialc.service.CC;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

/**
 * Menú principal visual del sistema de raids
 * Usa SmartInventory igual que el sistema de custom items
 */
public class RaidMainMenu {

    public static SmartInventory createMainMenu() {
        return SmartInventory.builder()
                .id("raid_main_menu")
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane((short) 11)));

                        // TÍTULO / INFO
                        ItemStack titleItem = new ItemStack(Material.NETHER_STAR);
                        ItemMeta titleMeta = titleItem.getItemMeta();
                        titleMeta.setDisplayName(CC.translate("&6&lSistema de Raids"));
                        List<String> titleLore = new ArrayList<>();
                        titleLore.add(CC.translate("&7Raids totales: &f" + RaidManager.getTotalRaids()));
                        titleLore.add(CC.translate("&7Raids activas: &f" + RaidManager.getTotalEnabledRaids()));
                        titleLore.add(CC.translate("&7Sesiones activas: &f" + RaidSessionManager.getTotalActiveSessions()));
                        titleLore.add(CC.translate("&7Parties: &f" + PartyManager.getTotalParties()));
                        titleMeta.setLore(titleLore);
                        titleItem.setItemMeta(titleMeta);
                        contents.set(0, 4, ClickableItem.empty(titleItem));

                        // CREAR RAID
                        ItemStack createButton = new ItemStack(Material.EMERALD_BLOCK);
                        ItemMeta createMeta = createButton.getItemMeta();
                        createMeta.setDisplayName(CC.translate("&a&lCrear Raid"));
                        createMeta.setLore(Arrays.asList(
                                CC.translate("&7Crea una nueva raid desde cero"),
                                CC.translate("&7Configura nombre, descripción"),
                                CC.translate("&7y cooldown"),
                                "",
                                CC.translate("&a[CLICK PARA CREAR]")
                        ));
                        createButton.setItemMeta(createMeta);
                        contents.set(1, 2, ClickableItem.of(createButton, e -> {
                            player.closeInventory();
                            RaidChatInputManager.startCreateRaidInput(player);
                        }));

                        // LISTAR / EDITAR RAIDS
                        ItemStack listButton = new ItemStack(Material.BOOK);
                        ItemMeta listMeta = listButton.getItemMeta();
                        listMeta.setDisplayName(CC.translate("&e&lVer Raids"));
                        listMeta.setLore(Arrays.asList(
                                CC.translate("&7Ve todas las raids creadas"),
                                CC.translate("&7y edítalas desde el menú"),
                                "",
                                CC.translate("&7Total: &f" + RaidManager.getTotalRaids() + " raids"),
                                "",
                                CC.translate("&e[CLICK PARA VER]")
                        ));
                        listButton.setItemMeta(listMeta);
                        contents.set(1, 4, ClickableItem.of(listButton, e -> {
                            RaidListMenu.createRaidListMenu(1).open(player);
                        }));

                        // SESIONES ACTIVAS
                        ItemStack sessionsButton = new ItemStack(Material.REDSTONE_TORCH_ON);
                        ItemMeta sessionsMeta = sessionsButton.getItemMeta();
                        sessionsMeta.setDisplayName(CC.translate("&c&lSesiones Activas"));
                        sessionsMeta.setLore(Arrays.asList(
                                CC.translate("&7Ve las raids en progreso"),
                                "",
                                CC.translate("&7Activas: &f" + RaidSessionManager.getTotalActiveSessions()),
                                CC.translate("&7Jugadores en raids: &f" + RaidSessionManager.getTotalPlayersInRaids()),
                                "",
                                CC.translate("&c[CLICK PARA VER]")
                        ));
                        sessionsButton.setItemMeta(sessionsMeta);
                        contents.set(1, 6, ClickableItem.of(sessionsButton, e -> {
                            RaidSessionsMenu.createSessionsMenu().open(player);
                        }));

                        // CATEGORÍAS
                        ItemStack categoryButton = new ItemStack(Material.BOOKSHELF);
                        ItemMeta categoryMeta = categoryButton.getItemMeta();
                        categoryMeta.setDisplayName(CC.translate("&6&lAdministrar Categorías"));
                        List<String> categoryLore = new ArrayList<>();
                        categoryLore.add(CC.translate("&7Crear, editar o eliminar categorías"));
                        categoryLore.add("");
                        List<org.debentialc.raids.models.RaidCategory> cats = RaidCategoryManager.getAllCategories();
                        if (cats.isEmpty()) {
                            categoryLore.add(CC.translate("&cNo hay categorías creadas"));
                        } else {
                            categoryLore.add(CC.translate("&7Categorías existentes:"));
                            for (org.debentialc.raids.models.RaidCategory cat : cats) {
                                categoryLore.add(CC.translate("&e- &f" + cat.getDisplayName()));
                            }
                        }
                        categoryLore.add("");
                        categoryLore.add(CC.translate("&6[CLICK PARA ADMINISTRAR]"));
                        categoryMeta.setLore(categoryLore);
                        categoryButton.setItemMeta(categoryMeta);
                        contents.set(1, 6, ClickableItem.of(categoryButton, e -> {
                            RaidCategoryAdminMenu.open(player);
                        }));

                        // AYUDA
                        ItemStack helpButton = new ItemStack(Material.PAPER);
                        ItemMeta helpMeta = helpButton.getItemMeta();
                        helpMeta.setDisplayName(CC.translate("&b&lAyuda"));
                        helpMeta.setLore(Arrays.asList(
                                CC.translate("&7Información sobre comandos"),
                                CC.translate("&7y cómo usar el sistema"),
                                "",
                                CC.translate("&b[CLICK PARA VER]")
                        ));
                        helpButton.setItemMeta(helpMeta);
                        contents.set(2, 2, ClickableItem.of(helpButton, e -> {
                            RaidHelpMenu.createHelpMenu().open(player);
                        }));

                        // GUARDAR TODO
                        ItemStack saveButton = new ItemStack(Material.CHEST);
                        ItemMeta saveMeta = saveButton.getItemMeta();
                        saveMeta.setDisplayName(CC.translate("&d&lGuardar Todo"));
                        saveMeta.setLore(Arrays.asList(
                                CC.translate("&7Guarda todas las raids"),
                                CC.translate("&7en el almacenamiento"),
                                "",
                                CC.translate("&d[CLICK PARA GUARDAR]")
                        ));
                        saveButton.setItemMeta(saveMeta);
                        contents.set(2, 4, ClickableItem.of(saveButton, e -> {
                            org.debentialc.raids.managers.RaidStorageManager.saveAllRaids();
                            player.sendMessage(CC.translate("&a✓ Raids guardadas correctamente"));
                            createMainMenu().open(player);
                        }));

                        // CERRAR
                        ItemStack closeButton = new ItemStack(Material.REDSTONE_BLOCK);
                        ItemMeta closeMeta = closeButton.getItemMeta();
                        closeMeta.setDisplayName(CC.translate("&c&lCerrar"));
                        closeButton.setItemMeta(closeMeta);
                        contents.set(2, 6, ClickableItem.of(closeButton, e -> {
                            player.closeInventory();
                        }));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(3, 9)
                .title(CC.translate("&6&lSistema de Raids"))
                .build();
    }

    static ItemStack createGlassPane(short color) {
        ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, color);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(CC.translate("&8"));
        glass.setItemMeta(glassMeta);
        return glass;
    }
}