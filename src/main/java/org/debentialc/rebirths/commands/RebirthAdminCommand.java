package org.debentialc.rebirths.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.debentialc.customitems.tools.permissions.Permissions;
import org.debentialc.rebirths.managers.RebirthBlockManager;
import org.debentialc.rebirths.managers.RebirthManager;
import org.debentialc.rebirths.menus.RebirthAdminMenus;
import org.debentialc.service.CC;
import org.debentialc.service.commands.BaseCommand;
import org.debentialc.service.commands.Command;
import org.debentialc.service.commands.CommandArgs;

import java.io.IOException;

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

        command.getSender().sendMessage(CC.translate("&cSubcomando desconocido. Uso: /rebirthadmin reset <jugador>"));
    }

    private void resetRebirths(org.bukkit.command.CommandSender sender, Player target) {
        RebirthManager.getInstance().resetPlayerRebirthLevel(target);
        RebirthBlockManager.getInstance().clearAllPlayerBlockStats(target);

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "jrmcrei " + target.getName() + " 0 true true true");

        sender.sendMessage(CC.translate("&a✓ Rebirths de &e" + target.getName() + " &areseteados."));
        target.sendMessage(CC.translate("&c⚠ Un administrador ha reseteado tus rebirths."));
    }

    private void resetRebirthsOffline(org.bukkit.command.CommandSender sender, OfflinePlayer target) {
        RebirthManager.getInstance().resetPlayerRebirthLevel(target.getUniqueId());
        RebirthBlockManager.getInstance().clearAllPlayerBlockStats(target.getUniqueId());

        sender.sendMessage(CC.translate("&a✓ Rebirths de &e" + target.getName() + " &areseteados (jugador offline)."));
    }
}
