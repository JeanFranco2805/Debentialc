package org.debentialc.rebirths.menus;

import noppes.npcs.api.entity.IDBCPlayer;
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
import org.debentialc.rebirths.model.Rebirth;
import org.debentialc.rebirths.model.RebirthBlock;
import org.debentialc.service.CC;
import org.debentialc.service.General;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RebirthUnlockHandler {

    public static void unlockRebirth(Player player, Rebirth rebirth) {
        if (!RebirthManager.getInstance().canUnlockRebirth(player, rebirth)) {
            player.sendMessage(CC.translate("&c✗ No cumples los requisitos."));
            return;
        }

        RebirthManager.getInstance().unlockRebirth(player, rebirth.getId());

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "jrmca set all 10 " + player.getName());

        // Apply stat bonus one tick later to ensure stat changes have taken effect
        org.bukkit.Bukkit.getScheduler().runTask(org.debentialc.Main.instance, new Runnable() {
            public void run() {
                applyStatBonus(player, rebirth);
            }
        });

        giveRewardItems(player, rebirth.getRewardItems());
        executeRewardCommands(player, rebirth.getRewardCommands());

        playUnlockEffects(player);

        player.sendMessage("");
        player.sendMessage(CC.translate("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(CC.translate("&6&l✨ NUEVO REBIRTH DESBLOQUEADO ✨"));
        player.sendMessage("");
        player.sendMessage(CC.translate("&eHas desbloqueado: &r" + rebirth.getDisplayName()));
        player.sendMessage(CC.translate("&7Bonus de TPs: &f" + rebirth.getTpBonusPercent() + "%"));
        player.sendMessage("");
        player.sendMessage(CC.translate("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage("");

        Bukkit.broadcastMessage(CC.translate("&6&l[Rebirths] &e" + player.getName() + " &7ha desbloqueado &r" + rebirth.getDisplayName() + "&7!"));
    }

    public static void applyStatBonus(Player player, Rebirth rebirth) {
        double value = rebirth.getStatBonusMultiplier();
        if (value <= 0) return;

        String operation = rebirth.getStatBonusOperation();
        if (!"*".equals(operation) && !"+".equals(operation)) return;

        String bonusId = "rebirth_u";
        RebirthBlock block = RebirthBlockManager.getInstance().getBlockForRebirth(rebirth.getId());
        if (block != null) {
            bonusId = "rebirth_" + block.getId();
        }

        try {
            IDBCPlayer idbcPlayer = General.getDBCPlayer(player.getName());
            String[] stats = {"STR", "DEX", "CON", "SPI", "WIL"};
            for (String stat : stats) {
                String bonusStat = General.BONUS_STATS.get(stat);
                if (bonusStat == null) continue;
                try {
                    idbcPlayer.addBonusAttribute(bonusStat, bonusId, operation, value);
                } catch (Exception e) {
                    idbcPlayer.setBonusAttribute(bonusStat, bonusId, operation, value);
                }
            }
            player.sendMessage(CC.translate("&7Bonus de stats aplicado (&f" + bonusId + "&7): &f" + operation + " " + value));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void removeStatBonus(Player player, String bonusId) {
        try {
            IDBCPlayer idbcPlayer = General.getDBCPlayer(player.getName());
            for (String stat : General.BONUS_STATS.values()) {
                try {
                    idbcPlayer.removeBonusAttribute(stat, bonusId);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void giveRewardItems(Player player, List<ItemStack> items) {
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

    public static void executeRewardCommands(Player player, List<String> commands) {
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
