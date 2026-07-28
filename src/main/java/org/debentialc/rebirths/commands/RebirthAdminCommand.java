package org.debentialc.rebirths.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.debentialc.Main;
import org.debentialc.customitems.tools.permissions.Permissions;
import org.debentialc.rebirths.managers.RebirthBlockManager;
import org.debentialc.rebirths.managers.RebirthManager;
import org.debentialc.rebirths.menus.RebirthAdminMenus;
import org.debentialc.rebirths.menus.RebirthUnlockHandler;
import org.debentialc.rebirths.model.Rebirth;
import org.debentialc.rebirths.model.RebirthBlock;
import org.debentialc.service.CC;
import org.debentialc.service.commands.BaseCommand;
import org.debentialc.service.commands.Command;
import org.debentialc.service.commands.CommandArgs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RebirthAdminCommand extends BaseCommand {

    @Command(name = "rebirthadmin", aliases = {"radm", "rebirthadm"}, permission = Permissions.COMMAND + "rebirth.admin")
    @Override
    public void onCommand(CommandArgs command) throws IOException {
        if (command.length() == 0) {
            if (!command.isPlayer()) {
                command.getSender().sendMessage(CC.translate("&cUso: /rebirthadmin reset <jugador>"));
                return;
            }
            Player player = command.getPlayer();
            RebirthAdminMenus.createMainMenu().open(player);
            return;
        }

        String subCommand = command.getArgs(0).toLowerCase();

        if (subCommand.equals("reset")) {
            if (command.length() < 2) {
                command.getSender().sendMessage(CC.translate("&cUso: /rebirthadmin reset <jugador>"));
                return;
            }

            String targetName = command.getArgs(1);
            Player onlineTarget = Bukkit.getPlayerExact(targetName);

            if (onlineTarget != null) {
                resetRebirths(command.getSender(), onlineTarget);
                return;
            }

            @SuppressWarnings("deprecation")
            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
            if (offlineTarget == null || !offlineTarget.hasPlayedBefore()) {
                command.getSender().sendMessage(CC.translate("&c✗ Jugador no encontrado: &f" + targetName));
                return;
            }

            resetRebirthsOffline(command.getSender(), offlineTarget);
            return;
        }

        if (subCommand.equals("skip")) {
            if (command.length() < 3) {
                command.getSender().sendMessage(CC.translate("&cUso: /rebirthadmin skip <jugador> <bloqueId> [rebirthId]"));
                return;
            }

            String targetName = command.getArgs(1);
            Player onlineTarget = Bukkit.getPlayerExact(targetName);
            if (onlineTarget == null) {
                command.getSender().sendMessage(CC.translate("&c✗ El jugador debe estar online para saltar rebirths."));
                return;
            }

            String blockId = command.getArgs(2);
            RebirthBlock block = RebirthBlockManager.getInstance().getBlock(blockId);
            if (block == null) {
                command.getSender().sendMessage(CC.translate("&c✗ Bloque no encontrado: &f" + blockId));
                return;
            }

            int destRebirthId;
            if (command.length() >= 4) {
                try {
                    destRebirthId = Integer.parseInt(command.getArgs(3));
                } catch (NumberFormatException e) {
                    command.getSender().sendMessage(CC.translate("&c✗ ID de rebirth inválido."));
                    return;
                }
                if (!block.containsRebirth(destRebirthId)) {
                    command.getSender().sendMessage(CC.translate("&c✗ El rebirth &f" + destRebirthId + " &cno pertenece al bloque &f" + blockId));
                    return;
                }
            } else {
                destRebirthId = block.getEndRebirthId();
                if (destRebirthId == 0) {
                    command.getSender().sendMessage(CC.translate("&c✗ El bloque no tiene rebirths."));
                    return;
                }
            }

            skipRebirths(command.getSender(), onlineTarget, block, destRebirthId);
            return;
        }

        command.getSender().sendMessage(CC.translate("&cSubcomando desconocido. Uso: /rebirthadmin reset <jugador> | /rebirthadmin skip <jugador> <bloqueId> [rebirthId]"));
    }

    private void resetRebirths(org.bukkit.command.CommandSender sender, Player target) {
        RebirthManager.getInstance().resetPlayerRebirthLevel(target);

        for (RebirthBlock block : RebirthBlockManager.getInstance().getAllBlocks()) {
            RebirthUnlockHandler.removeStatBonus(target, "rebirth_" + block.getId());
        }

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "jrmca set all 10 " + target.getName());

        sender.sendMessage(CC.translate("&a✓ Rebirths de &e" + target.getName() + " &areseteados."));
        target.sendMessage(CC.translate("&c⚠ Un administrador ha reseteado tus rebirths."));
    }

    private void resetRebirthsOffline(org.bukkit.command.CommandSender sender, OfflinePlayer target) {
        RebirthManager.getInstance().resetPlayerRebirthLevel(target.getUniqueId());

        sender.sendMessage(CC.translate("&a✓ Rebirths de &e" + target.getName() + " &areseteados (jugador offline)."));
    }

    private void skipRebirths(CommandSender sender, Player target, RebirthBlock block, int destRebirthId) {
        int currentLevel = RebirthManager.getInstance().getPlayerRebirthLevel(target);

        if (destRebirthId <= currentLevel) {
            sender.sendMessage(CC.translate("&c✗ El jugador ya tiene desbloqueado ese rebirth o uno superior."));
            return;
        }

        int highestRebirth = RebirthManager.getInstance().getHighestRebirthId();
        if (destRebirthId > highestRebirth) {
            sender.sendMessage(CC.translate("&c✗ El rebirth destino no existe."));
            return;
        }

        Rebirth finalRebirth = RebirthManager.getInstance().getRebirth(destRebirthId);
        if (finalRebirth == null) {
            sender.sendMessage(CC.translate("&c✗ El rebirth destino no existe."));
            return;
        }

        RebirthUnlockHandler.giveRewardItems(target, finalRebirth.getRewardItems());
        RebirthUnlockHandler.executeRewardCommands(target, finalRebirth.getRewardCommands());
        RebirthManager.getInstance().unlockRebirth(target, destRebirthId);

        // Apply stat bonus of the destination rebirth one tick later
        final Rebirth rebirthToApply = finalRebirth;
        Bukkit.getScheduler().runTask(Main.instance, new Runnable() {
            public void run() {
                RebirthUnlockHandler.applyStatBonus(target, rebirthToApply);
            }
        });

        sender.sendMessage(CC.translate("&a✓ &e" + target.getName() + " &aha saltado hasta &e" + finalRebirth.getDisplayName() + "&a."));
        target.sendMessage(CC.translate("&6&l[Rebirths] &eHas saltado hasta &r" + finalRebirth.getDisplayName()));
    }
}
