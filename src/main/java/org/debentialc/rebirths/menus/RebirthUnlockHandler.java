package org.debentialc.rebirths.menus;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.debentialc.Main;
import org.debentialc.rebirths.managers.RebirthBlockManager;
import org.debentialc.rebirths.managers.RebirthManager;
import org.debentialc.rebirths.model.PlayerBlockStats;
import org.debentialc.rebirths.model.Rebirth;
import org.debentialc.rebirths.model.RebirthBlock;
import org.debentialc.service.CC;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RebirthUnlockHandler {

    public static void unlockRebirth(Player player, Rebirth rebirth) {
        if (!RebirthManager.getInstance().canUnlockRebirth(player, rebirth)) {
            player.sendMessage(CC.translate("&c✗ No cumples los requisitos."));
            return;
        }

        RebirthManager.getInstance().setPlayerRebirthLevel(player, rebirth.getId());

        applyLevelChange(player, rebirth);

        giveRewardItems(player, rebirth.getRewardItems());
        executeRewardCommands(player, rebirth.getRewardCommands());

        playUnlockEffects(player);

        player.sendMessage("");
        player.sendMessage(CC.translate("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(CC.translate("&6&l✨ NUEVO REBIRTH DESBLOQUEADO ✨"));
        player.sendMessage("");
        player.sendMessage(CC.translate("&eHas desbloqueado: &r" + rebirth.getDisplayName()));
        player.sendMessage(CC.translate("&7Bonus de TPs: &f" + rebirth.getTpBonusPercent() + "%"));

        RebirthBlock block = RebirthBlockManager.getInstance().getBlockForRebirth(rebirth.getId());
        if (block != null && block.isSaveLevel()) {
            player.sendMessage(CC.translate("&7Tu nivel ha sido preservado en el bloque &e" + block.getName() + "&7."));
        } else {
            player.sendMessage(CC.translate("&7Tu nivel ha sido reiniciado."));
        }
        player.sendMessage("");
        player.sendMessage(CC.translate("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage("");

        Bukkit.broadcastMessage(CC.translate("&6&l[Rebirths] &e" + player.getName() + " &7ha desbloqueado &r" + rebirth.getDisplayName() + "&7!"));
    }

    private static void applyLevelChange(Player player, Rebirth rebirth) {
        RebirthBlock block = RebirthBlockManager.getInstance().getBlockForRebirth(rebirth.getId());

        if (block != null && block.isSaveLevel()) {
            PlayerBlockStats savedStats = RebirthBlockManager.getInstance().getPlayerBlockStats(player, block.getId());

            if (savedStats.getStats().isEmpty()) {
                Map<String, Integer> captured = RebirthBlockManager.getInstance().capturePlayerStats(player);
                savedStats.setStartRebirthId(block.getId());
                savedStats.setStats(captured);
                RebirthBlockManager.getInstance().savePlayerBlockStats(player, block.getId(), savedStats);
                player.sendMessage(CC.translate("&a✓ Nivel del bloque &e" + block.getName() + " &acapturado."));
            }

            RebirthBlockManager.getInstance().restorePlayerStats(player, savedStats.getStats());
            player.sendMessage(CC.translate("&a✓ Stats restauradas al nivel del bloque &e" + block.getName()));
        } else {
            if (block != null) {
                RebirthBlockManager.getInstance().clearPlayerBlockStats(player, block.getId());
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "jrmcrei " + player.getName() + " 0 true true true");
        }
    }

    private static void giveRewardItems(Player player, List<ItemStack> items) {
        if (items == null || items.isEmpty()) return;

        for (ItemStack item : items) {
            if (item == null || item.getType() == Material.AIR) continue;

            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
            if (!leftover.isEmpty()) {
                Location loc = player.getLocation();
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(loc, drop);
                }
                player.sendMessage(CC.translate("&e⚠ Tu inventario estaba lleno. Algunos items cayeron al suelo."));
            }
        }
    }

    private static void executeRewardCommands(Player player, List<String> commands) {
        if (commands == null || commands.isEmpty()) return;

        for (String command : commands) {
            if (command == null || command.isEmpty()) continue;
            String formatted = command.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formatted);
        }
    }

    private static void playUnlockEffects(Player player) {
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 2.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.EXPLODE, 1.5f, 1.2f);

        for (int i = 0; i < 20; i++) {
            player.getWorld().playEffect(player.getLocation().clone().add(0, 1, 0), Effect.MOBSPAWNER_FLAMES, 0);
        }
        for (int i = 0; i < 15; i++) {
            player.getWorld().playEffect(player.getLocation().clone().add(0, 1, 0), Effect.POTION_BREAK, 0);
        }
    }
}
