package org.debentialc.factions.menus;

import com.massivecraft.factions.entity.Faction;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.debentialc.factions.FactionWarp;
import org.debentialc.factions.FactionWarpStorage;
import org.debentialc.factions.cmd.CmdFactionWarpCommand;
import org.debentialc.service.CC;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FactionWarpMenu {

    public static void open(Player player, final Faction faction) {
        Map<String, FactionWarp> warps = FactionWarpStorage.getInstance().getWarps(faction.getId());

        if (warps.isEmpty()) {
            player.sendMessage(CC.translate("&cTu faction no tiene warps. Usa &e/f setwarp <nombre>&c."));
            return;
        }

        List<FactionWarp> warpList = new ArrayList<>(warps.values());
        int rows = Math.min(6, Math.max(3, (int) Math.ceil(warpList.size() / 7.0) + 2));

        SmartInventory inventory = SmartInventory.builder()
                .id("faction_warps_" + faction.getId())
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.fillBorders(ClickableItem.empty(createGlassPane()));

                        ItemStack title = new ItemStack(Material.ENDER_PEARL);
                        ItemMeta titleMeta = title.getItemMeta();
                        titleMeta.setDisplayName(CC.translate("&6&lWarps de " + faction.getName()));
                        title.setItemMeta(titleMeta);
                        contents.set(0, 4, ClickableItem.empty(title));

                        int slot = 10;
                        for (FactionWarp warp : warpList) {
                            ItemStack item = new ItemStack(Material.PAPER);
                            ItemMeta meta = item.getItemMeta();
                            meta.setDisplayName(CC.translate("&e" + warp.getName()));
                            List<String> lore = new ArrayList<>();
                            lore.add(CC.translate("&7Mundo: &f" + warp.getWorld()));
                            lore.add(CC.translate("&7X: &f" + (int) warp.getX() + " &7Y: &f" + (int) warp.getY() + " &7Z: &f" + (int) warp.getZ()));
                            lore.add("");
                            lore.add(CC.translate("&a[CLICK PARA TELETRANSPORTARTE]"));
                            meta.setLore(lore);
                            item.setItemMeta(meta);

                            final String warpName = warp.getName();
                            final String factionId = faction.getId();
                            contents.set(slot / 9, slot % 9, ClickableItem.of(item, e -> {
                                player.closeInventory();
                                CmdFactionWarpCommand.teleportToWarp(player, factionId, warpName);
                            }));

                            slot++;
                            if (slot % 9 == 8) slot += 2;
                            if (slot / 9 >= rows - 1) break;
                        }

                        ItemStack close = new ItemStack(Material.REDSTONE_BLOCK);
                        ItemMeta closeMeta = close.getItemMeta();
                        closeMeta.setDisplayName(CC.translate("&c&lCerrar"));
                        close.setItemMeta(closeMeta);
                        contents.set(rows - 1, 4, ClickableItem.of(close, e -> player.closeInventory()));
                    }

                    @Override
                    public void update(Player player, InventoryContents contents) {
                    }
                })
                .size(rows, 9)
                .title(CC.translate("&6Warps"))
                .build();

        inventory.open(player);
    }

    private static ItemStack createGlassPane() {
        ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(CC.translate("&8"));
        glass.setItemMeta(meta);
        return glass;
    }
}
