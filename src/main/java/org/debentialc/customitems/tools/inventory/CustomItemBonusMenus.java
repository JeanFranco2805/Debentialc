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

import java.util.Arrays;

/**
 * Menús para agregar bonificaciones a items custom
 * VERSIÓN CORREGIDA PARA MINECRAFT 1.7.10 (títulos máximo 32 caracteres)
 */
public class CustomItemBonusMenus {

    /**
     * Menú para seleccionar el atributo (stat)
     */
    public static SmartInventory createStatSelectionMenu(String itemId) {
        CustomItem item = CustomItemCommand.items.get(itemId);
        if (item == null) return null;

        return SmartInventory.builder()
                .id("ci_bonus_stat_select_" + itemId)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane()));

                        // Título
                        ItemStack titleItem = new ItemStack(Material.PAPER);
                        ItemMeta titleMeta = titleItem.getItemMeta();
                        titleMeta.setDisplayName(CC.translate("&c&lSelecciona un Atributo"));
                        titleMeta.setLore(Arrays.asList(
                                CC.translate("&7Item: &f" + itemId),
                                CC.translate("&7Elige sobre cuál stat aplicar el bonus")
                        ));
                        titleItem.setItemMeta(titleMeta);
                        contents.set(0, 4, ClickableItem.empty(titleItem));

                        // Stats disponibles
                        String[] stats = {"str", "con", "dex", "will", "mnd", "spi"};
                        String[] statNames = {"Fuerza", "Constitución", "Destreza", "Voluntad", "Mente", "Espíritu"};
                        Material[] statMaterials = {
                                Material.DIAMOND_SWORD,
                                Material.IRON_CHESTPLATE,
                                Material.LEATHER_BOOTS,
                                Material.WOOD_PICKAXE,
                                Material.BOOK,
                                Material.GLOWSTONE_DUST
                        };

                        int row = 1;
                        int col = 2;

                        for (int i = 0; i < stats.length; i++) {
                            final String stat = stats[i];
                            ItemStack statButton = new ItemStack(statMaterials[i]);
                            ItemMeta statMeta = statButton.getItemMeta();
                            statMeta.setDisplayName(CC.translate("&a&l" + statNames[i]));
                            statMeta.setLore(Arrays.asList(
                                    CC.translate("&7Stat: &f" + stat.toUpperCase()),
                                    CC.translate("&a[CLICK PARA SELECCIONAR]")
                            ));
                            statButton.setItemMeta(statMeta);

                            contents.set(row, col, ClickableItem.of(statButton, e -> {
                                BonusFlowManager.setSelectedStat(player, stat);
                                BonusFlowManager.nextStep(player);
                                createOperationSelectionMenu(itemId).open(player);
                            }));

                            col += 2;
                            if (col > 7) {
                                col = 2;
                                row++;
                            }
                        }

                        // Botón atrás
                        ItemStack backButton = new ItemStack(Material.ARROW);
                        ItemMeta backMeta = backButton.getItemMeta();
                        backMeta.setDisplayName(CC.translate("&b← Atrás"));
                        backButton.setItemMeta(backMeta);
                        contents.set(4, 0, ClickableItem.of(backButton, e -> {
                            BonusFlowManager.clearFlowState(player);
                            CustomItemMenus.openEditItemMenu(itemId).open(player);
                        }));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(5, 9)
                .title(CC.translate("&c&lStats "))
                .build();
    }

    /**
     * Menú para seleccionar la operación (+ - *)
     */
    public static SmartInventory createOperationSelectionMenu(String itemId) {
        CustomItem item = CustomItemCommand.items.get(itemId);
        if (item == null) return null;

        return SmartInventory.builder()
                .id("ci_bonus_op_select_" + itemId)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane()));

                        BonusFlowManager.BonusFlowState playerState = BonusFlowManager.getFlowState(player);

                        // Título
                        ItemStack titleItem = new ItemStack(Material.PAPER);
                        ItemMeta titleMeta = titleItem.getItemMeta();
                        titleMeta.setDisplayName(CC.translate("&c&lSelecciona Operación"));
                        titleMeta.setLore(Arrays.asList(
                                CC.translate("&7Item: &f" + itemId),
                                CC.translate("&7Stat: &f" + playerState.selectedStat.toUpperCase()),
                                CC.translate("&7Elige cómo aplicar el bonus")
                        ));
                        titleItem.setItemMeta(titleMeta);
                        contents.set(0, 4, ClickableItem.empty(titleItem));

                        // Operaciones disponibles
                        String[] operations = {"+", "-", "*"};
                        String[] operationNames = {"Aditivo", "Sustractivo", "Multiplicativo"};
                        String[] descriptions = {
                                "Suma directa al stat",
                                "Resta directa al stat",
                                "Porcentaje multiplicador"
                        };
                        Material[] operationMaterials = {
                                Material.EMERALD_BLOCK,
                                Material.REDSTONE_BLOCK,
                                Material.GOLD_BLOCK
                        };

                        int row = 2;
                        int col = 1;

                        for (int i = 0; i < operations.length; i++) {
                            final String operation = operations[i];
                            ItemStack opButton = new ItemStack(operationMaterials[i]);
                            ItemMeta opMeta = opButton.getItemMeta();
                            opMeta.setDisplayName(CC.translate("&a&l" + operationNames[i] + " (" + operation + ")"));
                            opMeta.setLore(Arrays.asList(
                                    CC.translate("&7" + descriptions[i]),
                                    CC.translate("&a[CLICK PARA SELECCIONAR]")
                            ));
                            opButton.setItemMeta(opMeta);

                            contents.set(row, col, ClickableItem.of(opButton, e -> {
                                BonusFlowManager.setSelectedOperation(player, operation);
                                BonusFlowManager.nextStep(player);

                                player.closeInventory();
                                BonusInputManager.startBonusInput(player, itemId,
                                        playerState.selectedStat, operation, "item");
                            }));

                            col += 3;
                        }

                        // Botón atrás
                        ItemStack backButton = new ItemStack(Material.ARROW);
                        ItemMeta backMeta = backButton.getItemMeta();
                        backMeta.setDisplayName(CC.translate("&b← Atrás"));
                        backButton.setItemMeta(backMeta);
                        contents.set(4, 0, ClickableItem.of(backButton, e -> {
                            BonusFlowManager.setSelectedStat(player, null);
                            BonusFlowManager.getFlowState(player).step = "selecting_stat";
                            createStatSelectionMenu(itemId).open(player);
                        }));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(5, 9)
                .title(CC.translate("&c&lOp"))
                .build();
    }

    /**
     * Crea un panel de cristal decorativo
     */
    private static ItemStack createGlassPane() {
        ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(CC.translate("&8"));
        glass.setItemMeta(glassMeta);
        return glass;
    }
}