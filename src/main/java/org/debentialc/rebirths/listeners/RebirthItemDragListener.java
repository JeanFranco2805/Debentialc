package org.debentialc.rebirths.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.debentialc.rebirths.managers.RebirthManager;
import org.debentialc.rebirths.menus.RebirthAdminMenus;
import org.debentialc.rebirths.model.Rebirth;
import org.debentialc.service.CC;

public class RebirthItemDragListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        Integer rebirthId = RebirthAdminMenus.getEditingRebirthId(player);
        if (rebirthId == null) return;

        if (event.getInventory() == null) return;
        if (!event.getInventory().equals(event.getView().getTopInventory())) return;

        ItemStack cursor = event.getCursor();
        if (cursor == null || cursor.getType() == Material.AIR) return;

        InventoryAction action = event.getAction();
        if (action != InventoryAction.PLACE_ALL
                && action != InventoryAction.PLACE_ONE
                && action != InventoryAction.PLACE_SOME
                && action != InventoryAction.SWAP_WITH_CURSOR) {
            return;
        }

        ItemStack current = event.getCurrentItem();
        if (current != null && current.getType() != Material.AIR && action != InventoryAction.SWAP_WITH_CURSOR) {
            return;
        }

        event.setCancelled(true);

        Rebirth rebirth = RebirthManager.getInstance().getRebirth(rebirthId);
        if (rebirth == null) {
            RebirthAdminMenus.removeEditingItems(player);
            return;
        }

        ItemStack toAdd = cursor.clone();
        if (action == InventoryAction.PLACE_ONE && toAdd.getAmount() > 1) {
            toAdd.setAmount(1);
        }

        rebirth.getRewardItems().add(toAdd);
        RebirthManager.getInstance().saveRebirth(rebirth);

        player.sendMessage(CC.translate("&a✓ Item agregado a la recompensa."));
        RebirthAdminMenus.createItemsMenu(rebirthId, RebirthAdminMenus.getBackAction(player)).open(player);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        Integer rebirthId = RebirthAdminMenus.getEditingRebirthId(player);
        if (rebirthId == null) return;

        boolean touchesTop = false;
        int topSize = event.getView().getTopInventory().getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize) {
                touchesTop = true;
                break;
            }
        }
        if (!touchesTop) return;

        event.setCancelled(true);

        Rebirth rebirth = RebirthManager.getInstance().getRebirth(rebirthId);
        if (rebirth == null) {
            RebirthAdminMenus.removeEditingItems(player);
            return;
        }

        ItemStack toAdd = event.getOldCursor().clone();
        if (toAdd.getType() == Material.AIR) return;

        rebirth.getRewardItems().add(toAdd);
        RebirthManager.getInstance().saveRebirth(rebirth);

        player.sendMessage(CC.translate("&a✓ Item agregado a la recompensa."));
        RebirthAdminMenus.createItemsMenu(rebirthId, RebirthAdminMenus.getBackAction(player)).open(player);
    }
}
