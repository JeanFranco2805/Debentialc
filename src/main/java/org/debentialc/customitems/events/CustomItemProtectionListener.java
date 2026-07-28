package org.debentialc.customitems.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.debentialc.Main;
import org.debentialc.customitems.commands.CustomItemCommand;
import org.debentialc.customitems.commands.RegisterItem;
import org.debentialc.customitems.tools.ci.CustomArmor;
import org.debentialc.customitems.tools.ci.CustomItem;
import org.debentialc.customitems.tools.nbt.NbtHandler;

/**
 * Silently blocks players from picking up, equipping or interacting with
 * custom items/armors whose rebirth or permission requirements they don't meet.
 */
public class CustomItemProtectionListener implements Listener {

    @EventHandler
    public void onPickup(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem().getItemStack();
        if (isRestricted(item, player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        ItemStack clicked = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        boolean restrictedClicked = isRestricted(clicked, player);
        boolean restrictedCursor = isRestricted(cursor, player);
        if (!restrictedClicked && !restrictedCursor) return;

        boolean ownInventory = event.getInventory().equals(player.getInventory());
        boolean armorSlot = event.getSlotType() == InventoryType.SlotType.ARMOR;

        if (restrictedClicked) {
            // Picking up from a chest/mob loot/other external inventory
            if (!ownInventory) {
                event.setCancelled(true);
                return;
            }
            // Trying to equip into armor slot
            if (armorSlot) {
                event.setCancelled(true);
                return;
            }
            // Shift-click armor in own inventory tries to auto-equip
            if (event.isShiftClick() && isArmorPiece(clicked)) {
                event.setCancelled(true);
                return;
            }
        }

        // Placing a restricted item from cursor into own inventory
        if (restrictedCursor && ownInventory) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (isRestricted(event.getOldCursor(), player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item != null && isRestricted(item, player)) {
            event.setCancelled(true);
        }
    }

    private boolean isRestricted(ItemStack item, Player player) {
        if (item == null || item.getTypeId() == 0) return false;

        NbtHandler nbt = new NbtHandler(item);
        if (nbt.hasKey("debentialc_id")) {
            return !meetsRequirements(nbt, player);
        }

        // Fallback for old items without NBT tags
        CustomItem customItem = findCustomItem(item);
        if (customItem != null) {
            return !meetsRequirements(customItem.getRequiredRebirthBlock(), customItem.getRequiredRebirthLevel(), customItem.getRequiredPermission(), player);
        }
        CustomArmor customArmor = findCustomArmor(item);
        if (customArmor != null) {
            return !meetsRequirements(customArmor.getRequiredRebirthBlock(), customArmor.getRequiredRebirthLevel(), customArmor.getRequiredPermission(), player);
        }

        return false;
    }

    private boolean meetsRequirements(NbtHandler nbt, Player player) {
        String rebirthBlock = null;
        if (nbt.hasKey("debentialc_rebirth_block")) {
            rebirthBlock = nbt.getString("debentialc_rebirth_block");
            // Fallback for old integer tags
            if (rebirthBlock == null || rebirthBlock.isEmpty()) {
                rebirthBlock = String.valueOf(nbt.getInteger("debentialc_rebirth_block"));
                if ("0".equals(rebirthBlock)) {
                    rebirthBlock = null;
                }
            }
        }
        int rebirthLevel = nbt.hasKey("debentialc_rebirth_level") ? nbt.getInteger("debentialc_rebirth_level") : 0;
        String permission = nbt.hasKey("debentialc_permission") ? nbt.getString("debentialc_permission") : null;
        return meetsRequirements(rebirthBlock, rebirthLevel, permission, player);
    }

    private boolean meetsRequirements(String rebirthBlock, int rebirthLevel, String permission, Player player) {
        if (permission != null && !permission.isEmpty()) {
            if (!player.hasPermission(permission)) {
                return false;
            }
        }
        if (rebirthBlock != null && !rebirthBlock.isEmpty() && rebirthLevel > 0) {
            int playerLevel = Main.getPlayerRebirthLevelInBlock(player.getName(), rebirthBlock);
            if (playerLevel < rebirthLevel) {
                return false;
            }
        }
        return true;
    }

    private CustomItem findCustomItem(ItemStack item) {
        if (!item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        CustomItem probe = new CustomItem()
                .setMaterial(item.getTypeId())
                .setLore(meta.getLore())
                .setDisplayName(meta.getDisplayName());

        for (CustomItem ci : CustomItemCommand.items.values()) {
            if (ci.equals(probe)) return ci;
        }
        return null;
    }

    private CustomArmor findCustomArmor(ItemStack item) {
        for (CustomArmor armor : RegisterItem.items.values()) {
            if (armor.matchesItem(item)) return armor;
        }
        return null;
    }

    private boolean isArmorPiece(ItemStack item) {
        if (item == null) return false;

        NbtHandler nbt = new NbtHandler(item);
        if ("armor".equalsIgnoreCase(nbt.getString("debentialc_type"))) {
            return true;
        }
        if (findCustomArmor(item) != null) {
            return true;
        }

        int id = item.getTypeId();
        // Vanilla armor material IDs in 1.7.10
        return id == 298 || id == 299 || id == 300 || id == 301 || // leather
               id == 302 || id == 303 || id == 304 || id == 305 || // chain
               id == 306 || id == 307 || id == 308 || id == 309 || // iron
               id == 310 || id == 311 || id == 312 || id == 313 || // diamond
               id == 314 || id == 315 || id == 316 || id == 317;   // gold
    }
}
