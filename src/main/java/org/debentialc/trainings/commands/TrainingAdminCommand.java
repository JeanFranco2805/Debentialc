package org.debentialc.trainings.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.debentialc.customitems.tools.permissions.Permissions;
import org.debentialc.service.CC;
import org.debentialc.service.commands.BaseCommand;
import org.debentialc.service.commands.Command;
import org.debentialc.service.commands.CommandArgs;
import org.debentialc.trainings.managers.TrainingManager;
import org.debentialc.trainings.menus.TrainingAdminMenus;
import org.debentialc.trainings.model.Training;

import java.io.IOException;
import java.util.UUID;

public class TrainingAdminCommand extends BaseCommand {

    @Command(name = "tadmin",
            permission = Permissions.COMMAND + "training.admin",
            inGameOnly = false)
    @Override
    public void onCommand(CommandArgs command) throws IOException {
        if (command.length() == 0) {
            if (!command.isPlayer()) {
                command.getSender().sendMessage(CC.translate("&cUso: /tadmin reset <jugador> <trainingId>"));
                return;
            }
            Player player = command.getPlayer();
            TrainingAdminMenus.createMainMenu(1).open(player);
            return;
        }

        String subCommand = command.getArgs(0).toLowerCase();

        if (subCommand.equals("reset")) {
            if (command.length() < 3) {
                command.getSender().sendMessage(CC.translate("&cUso: /tadmin reset <jugador> <trainingId>"));
                return;
            }

            String targetName = command.getArgs(1);
            String trainingId = command.getArgs(2);

            Player onlineTarget = Bukkit.getPlayerExact(targetName);
            if (onlineTarget != null) {
                TrainingManager.getInstance().setPlayerTrainingLevel(onlineTarget, trainingId, 0);
                command.getSender().sendMessage(CC.translate("&a✓ Nivel de training &e" + trainingId + " &areseteado para &e" + onlineTarget.getName()));
                onlineTarget.sendMessage(CC.translate("&c⚠ Un administrador ha reseteado tu progreso en &e" + trainingId));
                return;
            }

            @SuppressWarnings("deprecation")
            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
            if (offlineTarget == null || !offlineTarget.hasPlayedBefore()) {
                command.getSender().sendMessage(CC.translate("&c✗ Jugador no encontrado: &f" + targetName));
                return;
            }

            TrainingManager.getInstance().setPlayerTrainingLevel(offlineTarget.getUniqueId(), trainingId, 0);
            command.getSender().sendMessage(CC.translate("&a✓ Nivel de training &e" + trainingId + " &areseteado para &e" + offlineTarget.getName() + " &7(offline)"));
            return;
        }

        if (subCommand.equals("level") || subCommand.equals("setlevel") || subCommand.equals("addlevel")) {
            if (command.length() < 4) {
                command.getSender().sendMessage(CC.translate("&cUso: /tadmin level <jugador> <trainingId> <cantidad>"));
                return;
            }

            String targetName = command.getArgs(1);
            String trainingId = command.getArgs(2);
            int amount;
            try {
                amount = Integer.parseInt(command.getArgs(3));
            } catch (NumberFormatException e) {
                command.getSender().sendMessage(CC.translate("&cCantidad inválida: &f" + command.getArgs(3)));
                return;
            }

            Player onlineTarget = Bukkit.getPlayerExact(targetName);
            UUID targetUuid;
            String resolvedName;
            if (onlineTarget != null) {
                targetUuid = onlineTarget.getUniqueId();
                resolvedName = onlineTarget.getName();
            } else {
                @SuppressWarnings("deprecation")
                OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
                if (offlineTarget == null || !offlineTarget.hasPlayedBefore()) {
                    command.getSender().sendMessage(CC.translate("&c✗ Jugador no encontrado: &f" + targetName));
                    return;
                }
                targetUuid = offlineTarget.getUniqueId();
                resolvedName = offlineTarget.getName();
            }

            if (trainingId.equalsIgnoreCase("last")) {
                trainingId = TrainingManager.getInstance().getLatestTrainingId(targetUuid);
                if (trainingId == null) {
                    command.getSender().sendMessage(CC.translate("&c✗ No se pudo determinar el último training del jugador."));
                    return;
                }
            }

            Training training = TrainingManager.getInstance().getTraining(trainingId);
            if (training == null) {
                command.getSender().sendMessage(CC.translate("&c✗ Training no encontrado: &f" + trainingId));
                return;
            }

            int currentLevel = TrainingManager.getInstance().getPlayerTrainingLevel(targetUuid, trainingId);
            int newLevel = Math.max(0, Math.min(training.getMaxLevel(), currentLevel + amount));
            TrainingManager.getInstance().setPlayerTrainingLevel(targetUuid, trainingId, newLevel);
            String offlineSuffix = onlineTarget == null ? " &7(offline)" : "";
            command.getSender().sendMessage(CC.translate("&a✓ Training &e" + trainingId + " &ade &e" + resolvedName + " &apasó de nivel &e" + currentLevel + " &aa &e" + newLevel + offlineSuffix));
            if (onlineTarget != null) {
                onlineTarget.sendMessage(CC.translate("&a⚡ Tu training &e" + trainingId + " &aha pasado a nivel &e" + newLevel));
            }
            return;
        }

        if (!command.isPlayer()) {
            command.getSender().sendMessage(CC.translate("&cUso: /tadmin reset <jugador> <trainingId>"));
            return;
        }

        Player player = command.getPlayer();
        TrainingAdminMenus.createMainMenu(1).open(player);
    }
}
