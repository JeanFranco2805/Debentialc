package org.debentialc.crates.events;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.debentialc.crates.managers.CrateManager;
import org.debentialc.crates.models.Crate;
import org.debentialc.crates.models.CrateItem;
import org.debentialc.crates.models.CrateRarity;
import org.debentialc.crates.utils.CrateItemSerializer;
import org.debentialc.service.CC;

import java.util.HashMap;

public class CrateListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.CHEST) {
            return;
        }
        Crate crate = CrateManager.getInstance().getCrateAtLocation(block.getLocation());
        if (crate == null) {
            return;
        }

        Player player = event.getPlayer();
        event.setCancelled(true);

        if (action == Action.LEFT_CLICK_BLOCK) {
            if (player.isSneaking() && player.hasPermission("dbcplugin.crates.admin")) {
                CrateEditInventory.open(player, crate);
            } else {
                CrateEditInventory.openView(player, crate);
            }
            return;
        }

        if (player.isSneaking() && player.hasPermission("dbcplugin.crates.admin")) {
            CrateEditInventory.open(player, crate);
            return;
        }

        ItemStack item = player.getItemInHand();
        String crateId = CrateManager.getInstance().getKeyCrateId(item);
        if (crateId == null || !crateId.equalsIgnoreCase(crate.getId())) {
            player.sendMessage(CC.translate("&c✗ Necesitas una llave de " + crate.getDisplayName() + " &cpara abrirla."));
            return;
        }
        openCrate(player, crate, item);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.CHEST) {
            return;
        }
        Crate crate = CrateManager.getInstance().getCrateAtLocation(block.getLocation());
        if (crate != null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(CC.translate("&c✗ No puedes romper una crate. Usa &e/crate delete " + crate.getId() + " &csi eres admin."));
        }
    }

    private void openCrate(Player player, Crate crate, ItemStack key) {
        if (crate.getItems().isEmpty()) {
            player.sendMessage(CC.translate("&c✗ La crate &f" + crate.getDisplayName() + " &cno tiene items."));
            return;
        }
        CrateItem won = CrateManager.getInstance().openCrate(crate.getId());
        if (won == null) {
            player.sendMessage(CC.translate("&c✗ Error al abrir la crate."));
            return;
        }
        ItemStack reward = CrateItemSerializer.itemFromBase64(won.getItemBase64());
        if (reward == null) {
            player.sendMessage(CC.translate("&c✗ Error al crear el item ganado."));
            return;
        }
        if (key.getAmount() > 1) {
            key.setAmount(key.getAmount() - 1);
        } else {
            player.setItemInHand(null);
        }
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(reward);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
        player.sendMessage(CC.translate("&a✓ ¡Has abierto la crate &f" + crate.getDisplayName() + "&a!"));
        CrateRarity rarity = CrateManager.getInstance().getRarity(won.getRarityId());
        if (rarity != null && rarity.isAnnounce()) {
            String itemName = reward.hasItemMeta() && reward.getItemMeta().hasDisplayName()
                    ? reward.getItemMeta().getDisplayName()
                    : reward.getType().name();
            Bukkit.broadcastMessage(CC.translate(rarity.getColor() + "¡" + player.getName() + " ha obtenido " + itemName + " de " + crate.getDisplayName() + "!"));
        }
    }
}
