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
import org.debentialc.customitems.tools.storage.CustomItemStorage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Menú de opciones avanzadas para items custom
 * Incluye: Consumibles, Comandos, TPs, Scripts
 * VERSIÓN ACTUALIZADA: Soporte completo para scripts JavaScript
 */
public class CustomItemAdvancedOptionsMenu {

    public static SmartInventory createAdvancedOptionsMenu(String itemId) {
        CustomItem item = CustomItemCommand.items.get(itemId);
        if (item == null) return null;

        return SmartInventory.builder()
                .id("ci_advanced_" + itemId)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane()));

                        // TÍTULO
                        ItemStack titleItem = new ItemStack(Material.PAPER);
                        ItemMeta titleMeta = titleItem.getItemMeta();
                        titleMeta.setDisplayName(CC.translate("&d&lOpciones Avanzadas"));
                        titleMeta.setLore(Arrays.asList(
                                CC.translate("&7Item: &f" + itemId),
                                CC.translate("&7Configura opciones especiales")
                        ));
                        titleItem.setItemMeta(titleMeta);
                        contents.set(0, 4, ClickableItem.empty(titleItem));

                        // CONSUMIBLE
                        ItemStack consumableButton = new ItemStack(Material.APPLE);
                        ItemMeta consumableMeta = consumableButton.getItemMeta();
                        consumableMeta.setDisplayName(CC.translate("&a&lConsumible"));
                        List<String> consumableLore = new ArrayList<>();
                        consumableLore.add(CC.translate("&7El item se consume al usarse"));
                        consumableLore.add("");
                        if (item.isConsumable()) {
                            consumableLore.add(CC.translate("&a✓ ACTIVADO"));
                            consumableLore.add("");
                            consumableLore.add(CC.translate("&c[CLICK PARA DESACTIVAR]"));
                        } else {
                            consumableLore.add(CC.translate("&c✗ DESACTIVADO"));
                            consumableLore.add("");
                            consumableLore.add(CC.translate("&a[CLICK PARA ACTIVAR]"));
                        }
                        consumableMeta.setLore(consumableLore);
                        consumableButton.setItemMeta(consumableMeta);
                        contents.set(2, 1, ClickableItem.of(consumableButton, e -> {
                            // Alternar estado
                            item.setConsumable(!item.isConsumable());

                            // Guardar
                            CustomItemStorage storage = CustomItemStorage.getInstance();
                            storage.saveItem(item);

                            // Mensaje
                            player.sendMessage("");
                            if (item.isConsumable()) {
                                player.sendMessage(CC.translate("&a✓ Item ahora es consumible"));
                            } else {
                                player.sendMessage(CC.translate("&c✗ Item ya no es consumible"));
                            }
                            player.sendMessage("");

                            // Reabrir menú
                            createAdvancedOptionsMenu(itemId).open(player);
                        }));

                        // DELAY (antes Cooldown)
                        ItemStack delayButton = new ItemStack(Material.WATCH);
                        ItemMeta delayMeta = delayButton.getItemMeta();
                        delayMeta.setDisplayName(CC.translate("&6&lDelay"));
                        List<String> delayLore = new ArrayList<>();
                        delayLore.add(CC.translate("&7Tiempo de espera entre usos"));
                        delayLore.add(CC.translate("&7Solo aplica si el item es consumible"));
                        delayLore.add("");
                        if (item.getDelaySeconds() > 0) {
                            delayLore.add(CC.translate("&7Actual: &f" + org.debentialc.boosters.core.BoosterParser.formatSecondsToTime(item.getDelaySeconds())));
                        } else {
                            delayLore.add(CC.translate("&cSin delay"));
                        }
                        delayLore.add("");
                        delayLore.add(CC.translate("&a[CLICK PARA CONFIGURAR]"));
                        delayLore.add(CC.translate("&7Formato: 50m, 1h, 2d, 30s"));
                        delayMeta.setLore(delayLore);
                        delayButton.setItemMeta(delayMeta);
                        contents.set(2, 2, ClickableItem.of(delayButton, e -> {
                            player.closeInventory();
                            ItemCooldownInputManager.startCooldownInput(player, itemId);
                        }));

                        // COOLDOWN (expiración real)
                        ItemStack cooldownButton = new ItemStack(Material.BEDROCK);
                        ItemMeta cooldownMeta = cooldownButton.getItemMeta();
                        cooldownMeta.setDisplayName(CC.translate("&4&lCooldown"));
                        List<String> cooldownLore = new ArrayList<>();
                        cooldownLore.add(CC.translate("&7Tiempo de vida real del item"));
                        cooldownLore.add(CC.translate("&7Al expirar desaparece de cualquier lugar"));
                        cooldownLore.add("");
                        if (item.getExpirationSeconds() > 0) {
                            cooldownLore.add(CC.translate("&7Actual: &f" + org.debentialc.boosters.core.BoosterParser.formatSecondsToTime(item.getExpirationSeconds())));
                        } else {
                            cooldownLore.add(CC.translate("&cSin cooldown de expiración"));
                        }
                        cooldownLore.add("");
                        cooldownLore.add(CC.translate("&a[CLICK PARA CONFIGURAR]"));
                        cooldownLore.add(CC.translate("&7Formato: 30m, 1h, 2d, 7d, 30s"));
                        cooldownMeta.setLore(cooldownLore);
                        cooldownButton.setItemMeta(cooldownMeta);
                        contents.set(2, 3, ClickableItem.of(cooldownButton, e -> {
                            player.closeInventory();
                            ItemExpirationInputManager.startExpirationInput(player, itemId);
                        }));

                        // COMANDOS
                        ItemStack commandsButton = new ItemStack(Material.COMMAND);
                        ItemMeta commandsMeta = commandsButton.getItemMeta();
                        commandsMeta.setDisplayName(CC.translate("&e&lComandos"));
                        List<String> commandsLore = new ArrayList<>();
                        commandsLore.add(CC.translate("&7Ejecuta comandos al consumir"));
                        commandsLore.add("");
                        commandsLore.add(CC.translate("&7Comandos actuales: &f" +
                                (item.getCommands() != null ? item.getCommands().size() : 0)));
                        if (item.getCommands() != null && !item.getCommands().isEmpty()) {
                            commandsLore.add("");
                            for (String cmd : item.getCommands()) {
                                commandsLore.add(CC.translate("&f  • &7/" + cmd));
                            }
                        }
                        commandsLore.add("");
                        commandsLore.add(CC.translate("&a[CLICK PARA GESTIONAR]"));
                        commandsLore.add(CC.translate("&7@dp = Quien usa el item"));
                        commandsLore.add(CC.translate("&7@p = A quien apunta"));
                        commandsMeta.setLore(commandsLore);
                        commandsButton.setItemMeta(commandsMeta);
                        contents.set(2, 4, ClickableItem.of(commandsButton, e -> {
                            createCommandsMenu(itemId).open(player);
                        }));

                        // TPS
                        ItemStack tpButton = new ItemStack(Material.EXP_BOTTLE);
                        ItemMeta tpMeta = tpButton.getItemMeta();
                        tpMeta.setDisplayName(CC.translate("&b&lTP (Training Points)"));
                        List<String> tpLore = new ArrayList<>();
                        tpLore.add(CC.translate("&7Otorga TPs al consumir"));
                        tpLore.add("");
                        tpLore.add(CC.translate("&7Valor TP: &f" + item.getTpValue()));
                        tpLore.add(CC.translate("&7Consume stack: " +
                                (item.isTpConsumeStack() ? "&a✓ SÍ" : "&c✗ NO")));
                        tpLore.add("");
                        tpLore.add(CC.translate("&a[CLICK PARA CONFIGURAR]"));
                        tpMeta.setLore(tpLore);
                        tpButton.setItemMeta(tpMeta);
                        contents.set(2, 5, ClickableItem.of(tpButton, e -> {
                            createTPConfigMenu(itemId).open(player);
                        }));

                        // SCRIPTS - ACTUALIZADO
                        ItemStack scriptsButton = new ItemStack(Material.WRITTEN_BOOK);
                        ItemMeta scriptsMeta = scriptsButton.getItemMeta();
                        scriptsMeta.setDisplayName(CC.translate("&5&lScripts"));
                        List<String> scriptsLore = new ArrayList<>();
                        scriptsLore.add(CC.translate("&7Ejecuta scripts personalizados"));
                        scriptsLore.add(CC.translate("&7mediante JavaScript (Rhino)"));
                        scriptsLore.add("");

                        boolean hasScript = org.debentialc.customitems.tools.scripts.ScriptManager.getInstance().hasScript(itemId);
                        if (hasScript) {
                            scriptsLore.add(CC.translate("&a✓ Script asociado"));
                        } else {
                            scriptsLore.add(CC.translate("&7Sin script"));
                        }

                        scriptsLore.add("");
                        scriptsLore.add(CC.translate("&a[CLICK PARA GESTIONAR]"));
                        scriptsMeta.setLore(scriptsLore);
                        scriptsButton.setItemMeta(scriptsMeta);
                        contents.set(2, 7, ClickableItem.of(scriptsButton, e -> {
                            org.debentialc.customitems.tools.inventory.ScriptManagementMenu.createScriptMenu(itemId).open(player);
                        }));

                        // PROPIETARIO
                        ItemStack ownerButton = new ItemStack(Material.NAME_TAG);
                        ItemMeta ownerMeta = ownerButton.getItemMeta();
                        ownerMeta.setDisplayName(CC.translate("&a&lPropietario"));
                        List<String> ownerLore = new ArrayList<>();
                        ownerLore.add(CC.translate("&7Solo el jugador que recibe el item"));
                        ownerLore.add(CC.translate("&7podrá usarlo/equiparlo."));
                        ownerLore.add("");
                        ownerLore.add(CC.translate(item.isOwnerOnly() ? "&a✓ ACTIVADO" : "&c✗ DESACTIVADO"));
                        ownerLore.add("");
                        ownerLore.add(CC.translate("&a[CLICK PARA ALTERNAR]"));
                        ownerMeta.setLore(ownerLore);
                        ownerButton.setItemMeta(ownerMeta);
                        contents.set(3, 4, ClickableItem.of(ownerButton, e -> {
                            item.setOwnerOnly(!item.isOwnerOnly());
                            CustomItemStorage.getInstance().saveItem(item);
                            player.sendMessage("");
                            player.sendMessage(CC.translate(item.isOwnerOnly() ? "&a✓ El item ahora tiene propietario" : "&c✓ El item ya no tiene propietario"));
                            player.sendMessage("");
                            createAdvancedOptionsMenu(itemId).open(player);
                        }));

                        // BOTÓN ATRÁS
                        ItemStack backButton = new ItemStack(Material.ARROW);
                        ItemMeta backMeta = backButton.getItemMeta();
                        backMeta.setDisplayName(CC.translate("&b← Atrás"));
                        backButton.setItemMeta(backMeta);
                        contents.set(4, 4, ClickableItem.of(backButton, e -> {
                            CustomItemMenus.openEditItemMenu(itemId).open(player);
                        }));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(5, 9)
                .title(CC.translate("&d&lOpciones" ))
                .build();
    }

    /**
     * Menú de gestión de comandos
     */
    public static SmartInventory createCommandsMenu(String itemId) {
        CustomItem item = CustomItemCommand.items.get(itemId);
        if (item == null) return null;

        return SmartInventory.builder()
                .id("ci_commands_" + itemId)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane()));

                        // TÍTULO
                        ItemStack titleItem = new ItemStack(Material.PAPER);
                        ItemMeta titleMeta = titleItem.getItemMeta();
                        titleMeta.setDisplayName(CC.translate("&e&lGestión de Comandos"));
                        titleMeta.setLore(Arrays.asList(
                                CC.translate("&7Item: &f" + itemId),
                                CC.translate("&7Total: &f" +
                                        (item.getCommands() != null ? item.getCommands().size() : 0))
                        ));
                        titleItem.setItemMeta(titleMeta);
                        contents.set(0, 4, ClickableItem.empty(titleItem));

                        // AGREGAR COMANDO
                        ItemStack addButton = new ItemStack(Material.EMERALD_BLOCK);
                        ItemMeta addMeta = addButton.getItemMeta();
                        addMeta.setDisplayName(CC.translate("&a&lAgregar Comando"));
                        addMeta.setLore(Arrays.asList(
                                CC.translate("&7Escribe el comando en el chat"),
                                CC.translate("&7Sin incluir /"),
                                "",
                                CC.translate("&7Ejemplo: &fgamemode 1 @dp"),
                                CC.translate("&7@dp = Quien usa el item"),
                                CC.translate("&7@p = A quien apunta"),
                                "",
                                CC.translate("&a[CLICK PARA AGREGAR]")
                        ));
                        addButton.setItemMeta(addMeta);
                        contents.set(1, 4, ClickableItem.of(addButton, e -> {
                            player.closeInventory();
                            ItemCommandInputManager.startCommandInput(player, itemId);
                        }));

                        // LISTA DE COMANDOS
                        if (item.getCommands() != null && !item.getCommands().isEmpty()) {
                            int row = 2;
                            int col = 1;

                            for (int i = 0; i < item.getCommands().size(); i++) {
                                final String cmd = item.getCommands().get(i);
                                final int index = i;

                                ItemStack cmdItem = new ItemStack(Material.PAPER);
                                ItemMeta cmdMeta = cmdItem.getItemMeta();
                                cmdMeta.setDisplayName(CC.translate("&f/" + cmd));
                                cmdMeta.setLore(Arrays.asList(
                                        CC.translate("&7Comando #" + (i + 1)),
                                        "",
                                        CC.translate("&c[CLICK PARA ELIMINAR]")
                                ));
                                cmdItem.setItemMeta(cmdMeta);

                                contents.set(row, col, ClickableItem.of(cmdItem, e -> {
                                    // Eliminar comando
                                    item.getCommands().remove(index);

                                    // Guardar
                                    CustomItemStorage storage = CustomItemStorage.getInstance();
                                    storage.saveItem(item);

                                    player.sendMessage("");
                                    player.sendMessage(CC.translate("&c✗ Comando eliminado"));
                                    player.sendMessage(CC.translate("&7/" + cmd));
                                    player.sendMessage("");

                                    // Reabrir menú
                                    createCommandsMenu(itemId).open(player);
                                }));

                                col++;
                                if (col > 7) {
                                    col = 1;
                                    row++;
                                }
                            }
                        }

                        // BOTÓN ATRÁS
                        ItemStack backButton = new ItemStack(Material.ARROW);
                        ItemMeta backMeta = backButton.getItemMeta();
                        backMeta.setDisplayName(CC.translate("&b← Atrás"));
                        backButton.setItemMeta(backMeta);
                        contents.set(4, 4, ClickableItem.of(backButton, e -> {
                            createAdvancedOptionsMenu(itemId).open(player);
                        }));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(5, 9)
                .title(CC.translate("&e&lComandos - " + itemId))
                .build();
    }

    /**
     * Menú de configuración de TP
     */
    public static SmartInventory createTPConfigMenu(String itemId) {
        CustomItem item = CustomItemCommand.items.get(itemId);
        if (item == null) return null;

        return SmartInventory.builder()
                .id("ci_tp_config_" + itemId)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane()));

                        // TÍTULO
                        ItemStack titleItem = new ItemStack(Material.PAPER);
                        ItemMeta titleMeta = titleItem.getItemMeta();
                        titleMeta.setDisplayName(CC.translate("&b&lConfiguración TP"));
                        titleMeta.setLore(Arrays.asList(
                                CC.translate("&7Item: &f" + itemId),
                                CC.translate("&7Configura Training Points")
                        ));
                        titleItem.setItemMeta(titleMeta);
                        contents.set(0, 4, ClickableItem.empty(titleItem));

                        // VALOR TP
                        ItemStack valueButton = new ItemStack(Material.EXP_BOTTLE);
                        ItemMeta valueMeta = valueButton.getItemMeta();
                        valueMeta.setDisplayName(CC.translate("&a&lValor TP"));
                        valueMeta.setLore(Arrays.asList(
                                CC.translate("&7TP actual: &f" + item.getTpValue()),
                                "",
                                CC.translate("&7Cuántos TPs otorga el item"),
                                CC.translate("&7al ser consumido"),
                                "",
                                CC.translate("&a[CLICK PARA MODIFICAR]")
                        ));
                        valueButton.setItemMeta(valueMeta);
                        contents.set(2, 2, ClickableItem.of(valueButton, e -> {
                            player.closeInventory();
                            ItemTPInputManager.startTPValueInput(player, itemId);
                        }));

                        // CONSUME STACK
                        ItemStack consumeButton = new ItemStack(Material.REDSTONE_BLOCK);
                        ItemMeta consumeMeta = consumeButton.getItemMeta();
                        consumeMeta.setDisplayName(CC.translate("&c&lConsume Stack"));
                        List<String> consumeLore = new ArrayList<>();
                        consumeLore.add(CC.translate("&7Consume TODO el stack al usar"));
                        consumeLore.add(CC.translate("&7y multiplica los TPs otorgados"));
                        consumeLore.add("");
                        consumeLore.add(CC.translate("&7Ejemplo: 32 items x 100 TP"));
                        consumeLore.add(CC.translate("&7= &f3,200 TP &7total"));
                        consumeLore.add("");
                        if (item.isTpConsumeStack()) {
                            consumeLore.add(CC.translate("&a✓ ACTIVADO"));
                            consumeLore.add("");
                            consumeLore.add(CC.translate("&c[CLICK PARA DESACTIVAR]"));
                        } else {
                            consumeLore.add(CC.translate("&c✗ DESACTIVADO"));
                            consumeLore.add("");
                            consumeLore.add(CC.translate("&a[CLICK PARA ACTIVAR]"));
                        }
                        consumeMeta.setLore(consumeLore);
                        consumeButton.setItemMeta(consumeMeta);
                        contents.set(2, 6, ClickableItem.of(consumeButton, e -> {
                            // Alternar estado
                            item.setTpConsumeStack(!item.isTpConsumeStack());

                            // Guardar
                            CustomItemStorage storage = CustomItemStorage.getInstance();
                            storage.saveItem(item);

                            // Mensaje
                            player.sendMessage("");
                            if (item.isTpConsumeStack()) {
                                player.sendMessage(CC.translate("&a✓ Ahora consume el stack completo"));
                            } else {
                                player.sendMessage(CC.translate("&c✗ Solo consume 1 item"));
                            }
                            player.sendMessage("");

                            // Reabrir menú
                            createTPConfigMenu(itemId).open(player);
                        }));

                        // BOTÓN ATRÁS
                        ItemStack backButton = new ItemStack(Material.ARROW);
                        ItemMeta backMeta = backButton.getItemMeta();
                        backMeta.setDisplayName(CC.translate("&b← Atrás"));
                        backButton.setItemMeta(backMeta);
                        contents.set(4, 4, ClickableItem.of(backButton, e -> {
                            createAdvancedOptionsMenu(itemId).open(player);
                        }));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(5, 9)
                .title(CC.translate("&b&lTP Config - " + itemId))
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