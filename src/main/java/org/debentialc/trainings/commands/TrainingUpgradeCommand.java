package org.debentialc.trainings.commands;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.debentialc.customitems.tools.permissions.Permissions;
import org.debentialc.service.CC;
import org.debentialc.service.commands.BaseCommand;
import org.debentialc.service.commands.Command;
import org.debentialc.service.commands.CommandArgs;
import org.debentialc.trainings.managers.TrainingManager;
import org.debentialc.trainings.menus.TrainingPlayerMenus;
import org.debentialc.trainings.model.Training;

import java.io.IOException;

public class TrainingUpgradeCommand extends BaseCommand {

    @Command(name = "tupgrade", permission = Permissions.COMMAND + "tupgrade", inGameOnly = false)
    @Override
    public void onCommand(CommandArgs command) throws IOException {
        if (command.length() == 0) {
            if (!command.isPlayer()) {
                command.getSender().sendMessage(CC.translate("&cUso: /tupgrade <trainingId> <jugador>"));
                return;
            }
            TrainingPlayerMenus.createMainMenu(command.getPlayer(), 1).open(command.getPlayer());
            return;
        }

        String trainingId = command.getArgs(0);
        Training training = TrainingManager.getInstance().getTraining(trainingId);
        if (training == null) {
            command.getSender().sendMessage(CC.translate("&c✗ Training no encontrado: &f" + trainingId));
            return;
        }

        Player target;
        if (command.length() >= 2) {
            String targetName = command.getArgs(1);
            target = Bukkit.getPlayer(targetName);
            if (target == null) {
                command.getSender().sendMessage(CC.translate("&c✗ Jugador no encontrado: &f" + targetName));
                return;
            }
        } else {
            if (!command.isPlayer()) {
                command.getSender().sendMessage(CC.translate("&cDebes especificar un jugador: /tupgrade <trainingId> <jugador>"));
                return;
            }
            target = command.getPlayer();
        }

        TrainingPlayerMenus.createTrainingLevelsMenu(target, trainingId, 1).open(target);
    }
}
