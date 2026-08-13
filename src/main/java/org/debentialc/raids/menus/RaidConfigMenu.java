package org.debentialc.raids.menus;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.debentialc.customitems.commands.CustomItemCommand;
import org.debentialc.raids.managers.RaidCategoryManager;
import org.debentialc.raids.managers.RaidManager;
import org.debentialc.raids.managers.RaidStorageManager;
import org.debentialc.raids.models.Raid;
import org.debentialc.service.CC;

import java.util.*;

/**
 * Menú visual de configuración de una raid individual
 * ACTUALIZADO: Sin botón "Arena Spawn" (los spawn points se configuran por oleada)
 */
public class RaidConfigMenu {

    public static SmartInventory createRaidConfigMenu(String raidId) {
        Raid raid = RaidManager.getRaidById(raidId);
        if (raid == null) return null;

        return SmartInventory.builder()
                .id("raid_config_" + raidId)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        buildMenu(player, contents, raidId);
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(4, 9)
                .title(CC.translate("&6&lConfig Raid"))
                .build();
    }

    private static void reopenMenu(Player player, String raidId) {
        org.bukkit.Bukkit.getScheduler().scheduleSyncDelayedTask(
                org.debentialc.Main.instance,
                () -> {
                    SmartInventory menu = createRaidConfigMenu(raidId);
                    if (menu != null) {
                        menu.open(player);
                    }
                },
                1L
        );
    }

    private static void buildMenu(Player player, InventoryContents contents, String raidId) {
        Raid raid = RaidManager.getRaidById(raidId);
        if (raid == null) return;

        contents.fillBorders(ClickableItem.empty(RaidMainMenu.createGlassPane((short) 1)));

        // INFO DE LA RAID
        ItemStack infoItem = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName(CC.translate("&6&l" + raid.getDisplayName()));
        List<String> infoLore = new ArrayList<>();
        infoLore.add(CC.translate("&7ID: &f" + raid.getRaidId()));
        infoLore.add(CC.translate("&7Nombre visible: &f" + (raid.getRaidName() != null && !raid.getRaidName().isEmpty() ? raid.getRaidName() : "&7(mismo ID)")));
        infoLore.add(CC.translate("&7Categoría: &f" + RaidCategoryManager.getDisplayName(raid.getCategory())));
        infoLore.add(CC.translate("&7Ícono: &f" + (raid.getMenuItem() != null && !raid.getMenuItem().isEmpty() ? raid.getMenuItem() : "&7(por defecto)")));
        infoLore.add(CC.translate("&7Descripción: &f" + (raid.getDescription() != null ? raid.getDescription() : "N/A")));
        infoLore.add(CC.translate("&7Oleadas: &f" + raid.getTotalWaves()));
        infoLore.add(CC.translate("&7Jugadores: &f" + raid.getMinPlayers() + "-" + raid.getMaxPlayers()));
        infoLore.add(CC.translate("&7Cooldown: &f" + (raid.getCooldownSeconds() / 60) + " min"));
        infoLore.add(CC.translate("&7Spawn: " + (raid.getPlayerSpawnPoint() != null ? "&a✓ Configurado" : "&c✗ Sin configurar")));
        infoLore.add(CC.translate("&7Estado: " + (raid.isEnabled() ? "&a✓ Habilitada" : "&c✗ Deshabilitada")));
        infoMeta.setLore(infoLore);
        infoItem.setItemMeta(infoMeta);
        contents.set(1, 1, ClickableItem.empty(infoItem));

        // NOMBRE VISIBLE
        ItemStack renameButton = new ItemStack(Material.NAME_TAG);
        ItemMeta renameMeta = renameButton.getItemMeta();
        renameMeta.setDisplayName(CC.translate("&e&lCambiar Nombre Visible"));
        renameMeta.setLore(Arrays.asList(
                CC.translate("&7Nombre visible:"),
                CC.translate("&f" + raid.getDisplayName()),
                "",
                CC.translate("&a[CLICK PARA EDITAR]")
        ));
        renameButton.setItemMeta(renameMeta);
        contents.set(1, 2, ClickableItem.of(renameButton, e -> {
            player.closeInventory();
            RaidChatInputManager.startRaidRenameInput(player, raidId);
        }));

        // CAMBIAR ID
        ItemStack idButton = new ItemStack(Material.PAPER);
        ItemMeta idMeta = idButton.getItemMeta();
        idMeta.setDisplayName(CC.translate("&e&lCambiar ID"));
        idMeta.setLore(Arrays.asList(
                CC.translate("&7ID actual:"),
                CC.translate("&f" + raid.getRaidId()),
                "",
                CC.translate("&a[CLICK PARA EDITAR]")
        ));
        idButton.setItemMeta(idMeta);
        contents.set(2, 1, ClickableItem.of(idButton, e -> {
            player.closeInventory();
            RaidChatInputManager.startRaidIdInput(player, raidId);
        }));

        // EDITAR DESCRIPCIÓN
        ItemStack descButton = new ItemStack(Material.BOOK_AND_QUILL);
        ItemMeta descMeta = descButton.getItemMeta();
        descMeta.setDisplayName(CC.translate("&b&lDescripción"));
        descMeta.setLore(Arrays.asList(
                CC.translate("&7Actual: &f" + (raid.getDescription() != null ? raid.getDescription() : "Sin descripción")),
                "",
                CC.translate("&a[CLICK PARA EDITAR]")
        ));
        descButton.setItemMeta(descMeta);
        contents.set(1, 3, ClickableItem.of(descButton, e -> {
            player.closeInventory();
            RaidChatInputManager.startRaidDescriptionInput(player, raidId);
        }));

        // GESTIONAR OLEADAS
        ItemStack wavesButton = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta wavesMeta = wavesButton.getItemMeta();
        wavesMeta.setDisplayName(CC.translate("&6&lOleadas"));
        List<String> wavesLore = new ArrayList<>();
        wavesLore.add(CC.translate("&7Oleadas configuradas: &f" + raid.getTotalWaves()));
        if (raid.getTotalWaves() > 0) {
            int totalEnemies = 0;
            for (int i = 0; i < raid.getTotalWaves(); i++) {
                totalEnemies += raid.getWaveByIndex(i).getTotalEnemies();
            }
            wavesLore.add(CC.translate("&7Enemigos totales: &f" + totalEnemies));
        }
        wavesLore.add("");
        wavesLore.add(CC.translate("&6[CLICK PARA GESTIONAR]"));
        wavesMeta.setLore(wavesLore);
        wavesButton.setItemMeta(wavesMeta);
        contents.set(1, 4, ClickableItem.of(wavesButton, e -> {
            RaidWavesMenu.createWavesMenu(raidId).open(player);
        }));

        // CONFIGURAR PLAYER SPAWN
        boolean playerSpawnSet = raid.getPlayerSpawnPoint() != null;
        ItemStack playerSpawnButton;
        if (playerSpawnSet) {
            playerSpawnButton = new ItemStack(Material.BED);
        } else {
            playerSpawnButton = new ItemStack(Material.REDSTONE_BLOCK, 1, (short) 14);
        }
        ItemMeta playerSpawnMeta = playerSpawnButton.getItemMeta();
        playerSpawnMeta.setDisplayName(CC.translate("&a&lPlayer Spawn"));
        List<String> pSpawnLore = new ArrayList<>();
        pSpawnLore.add(CC.translate("&7Punto donde aparecen los jugadores"));
        if (playerSpawnSet) {
            Location pLoc = raid.getPlayerSpawnPoint();
            pSpawnLore.add(CC.translate("&7Estado: &a✓ Configurado"));
            pSpawnLore.add(CC.translate("&7Pos: &f" + pLoc.getBlockX() + ", " + pLoc.getBlockY() + ", " + pLoc.getBlockZ()));
        } else {
            pSpawnLore.add(CC.translate("&7Estado: &c✗ Sin configurar"));
        }
        pSpawnLore.add("");
        pSpawnLore.add(CC.translate("&7Se usará tu posición actual"));
        pSpawnLore.add("");
        pSpawnLore.add(CC.translate("&a[CLICK PARA ESTABLECER]"));
        playerSpawnMeta.setLore(pSpawnLore);
        playerSpawnButton.setItemMeta(playerSpawnMeta);
        contents.set(1, 5, ClickableItem.of(playerSpawnButton, e -> {
            Location loc = player.getLocation().clone();
            raid.setPlayerSpawnPoint(loc);
            RaidManager.updateRaid(raid);
            RaidStorageManager.saveRaid(raid);
            player.sendMessage(CC.translate("&a✓ Player spawn: &f" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()));
            reopenMenu(player, raidId);
        }));

        // CAMBIAR CATEGORÍA
        ItemStack categoryButton = new ItemStack(Material.BOOKSHELF);
        ItemMeta categoryMeta = categoryButton.getItemMeta();
        categoryMeta.setDisplayName(CC.translate("&e&lCambiar Categoría"));
        categoryMeta.setLore(Arrays.asList(
                CC.translate("&7Actual: &f" + RaidCategoryManager.getDisplayName(raid.getCategory())),
                "",
                CC.translate("&a[CLICK PARA CAMBIAR]")
        ));
        categoryButton.setItemMeta(categoryMeta);
        contents.set(1, 6, ClickableItem.of(categoryButton, e -> {
            RaidCategorySelectMenu.open(player, raidId);
        }));

        // CAMBIAR ÍCONO
        ItemStack iconButton;
        if (raid.getMenuItem() != null && !raid.getMenuItem().isEmpty() && CustomItemCommand.items.containsKey(raid.getMenuItem())) {
            iconButton = CustomItemCommand.buildItemStack(CustomItemCommand.items.get(raid.getMenuItem())).clone();
        } else {
            iconButton = new ItemStack(Material.ITEM_FRAME);
        }
        ItemMeta iconMeta = iconButton.getItemMeta();
        iconMeta.setDisplayName(CC.translate("&e&lCambiar Ícono"));
        iconMeta.setLore(Arrays.asList(
                CC.translate("&7Actual: &f" + (raid.getMenuItem() != null && !raid.getMenuItem().isEmpty() ? raid.getMenuItem() : "&7(por defecto)")),
                "",
                CC.translate("&a[CLICK PARA EDITAR]")
        ));
        iconButton.setItemMeta(iconMeta);
        contents.set(1, 7, ClickableItem.of(iconButton, e -> {
            player.closeInventory();
            RaidChatInputManager.startRaidMenuItemInput(player, raidId);
        }));

        // COOLDOWN
        ItemStack cooldownButton = new ItemStack(Material.WATCH);
        ItemMeta cooldownMeta = cooldownButton.getItemMeta();
        cooldownMeta.setDisplayName(CC.translate("&3&lCooldown"));
        cooldownMeta.setLore(Arrays.asList(
                CC.translate("&7Tiempo de espera entre raids"),
                CC.translate("&7Actual: &f" + (raid.getCooldownSeconds() / 60) + " minutos"),
                "",
                CC.translate("&3[CLICK PARA CAMBIAR]")
        ));
        cooldownButton.setItemMeta(cooldownMeta);
        contents.set(2, 2, ClickableItem.of(cooldownButton, e -> {
            player.closeInventory();
            RaidChatInputManager.startCooldownInput(player, raidId);
        }));

        // JUGADORES MIN/MAX
        ItemStack playersButton = new ItemStack(Material.SKULL_ITEM);
        ItemMeta playersMeta = playersButton.getItemMeta();
        playersMeta.setDisplayName(CC.translate("&b&lJugadores"));
        playersMeta.setLore(Arrays.asList(
                CC.translate("&7Mínimo: &f" + raid.getMinPlayers()),
                CC.translate("&7Máximo: &f" + raid.getMaxPlayers()),
                "",
                CC.translate("&b[CLICK PARA CAMBIAR]")
        ));
        playersButton.setItemMeta(playersMeta);
        contents.set(2, 3, ClickableItem.of(playersButton, e -> {
            player.closeInventory();
            RaidChatInputManager.startPlayersInput(player, raidId);
        }));

        // HABILITAR / DESHABILITAR
        ItemStack toggleButton;
        if (raid.isEnabled()) {
            toggleButton = new ItemStack(Material.EMERALD);
            ItemMeta toggleMeta = toggleButton.getItemMeta();
            toggleMeta.setDisplayName(CC.translate("&a&lHabilitada"));
            toggleMeta.setLore(Arrays.asList(
                    CC.translate("&a✓ La raid está activa"),
                    "",
                    CC.translate("&c[CLICK PARA DESHABILITAR]")
            ));
            toggleButton.setItemMeta(toggleMeta);
        } else {
            toggleButton = new ItemStack(Material.REDSTONE);
            ItemMeta toggleMeta = toggleButton.getItemMeta();
            toggleMeta.setDisplayName(CC.translate("&c&lDeshabilitada"));
            toggleMeta.setLore(Arrays.asList(
                    CC.translate("&c✗ La raid está inactiva"),
                    "",
                    CC.translate("&a[CLICK PARA HABILITAR]")
            ));
            toggleButton.setItemMeta(toggleMeta);
        }
        contents.set(2, 4, ClickableItem.of(toggleButton, e -> {
            if (raid.isEnabled()) {
                RaidManager.disableRaid(raidId);
                player.sendMessage(CC.translate("&c✗ Raid deshabilitada"));
            } else {
                RaidManager.enableRaid(raidId);
                player.sendMessage(CC.translate("&a✓ Raid habilitada"));
            }
            RaidStorageManager.saveRaid(raid);
            reopenMenu(player, raidId);
        }));

        // GUARDAR
        ItemStack saveButton = new ItemStack(Material.CHEST);
        ItemMeta saveMeta = saveButton.getItemMeta();
        saveMeta.setDisplayName(CC.translate("&d&lGuardar"));
        saveMeta.setLore(Arrays.asList(
                CC.translate("&7Guarda esta raid"),
                "",
                CC.translate("&d[CLICK PARA GUARDAR]")
        ));
        saveButton.setItemMeta(saveMeta);
        contents.set(2, 5, ClickableItem.of(saveButton, e -> {
            RaidStorageManager.saveRaid(raid);
            RaidStorageManager.saveAllRaids();
            player.sendMessage(CC.translate("&a✓ Raid guardada"));
        }));

        // ELIMINAR
        ItemStack deleteButton = new ItemStack(Material.ANVIL);
        ItemMeta deleteMeta = deleteButton.getItemMeta();
        deleteMeta.setDisplayName(CC.translate("&c&lEliminar Raid"));
        deleteMeta.setLore(Arrays.asList(
                CC.translate("&7Elimina esta raid permanentemente"),
                "",
                CC.translate("&c[CLICK PARA ELIMINAR]")
        ));
        deleteButton.setItemMeta(deleteMeta);
        contents.set(2, 7, ClickableItem.of(deleteButton, e -> {
            RaidDeleteConfirmMenu.createDeleteConfirmMenu(raidId).open(player);
        }));

        // ATRÁS
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName(CC.translate("&b← Volver a la categoría"));
        backButton.setItemMeta(backMeta);
        contents.set(3, 4, ClickableItem.of(backButton, e -> {
            String category = raid.getCategory();
            RaidAdminCategoryRaidsMenu.open(player, category);
        }));
    }
}