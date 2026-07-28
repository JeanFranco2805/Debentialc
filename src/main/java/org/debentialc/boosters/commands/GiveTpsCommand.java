package org.debentialc.boosters.commands;

import noppes.npcs.api.entity.IDBCPlayer;
import noppes.npcs.scripted.NpcAPI;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.debentialc.Main;
import org.debentialc.service.CC;
import org.debentialc.service.commands.BaseCommand;
import org.debentialc.service.commands.Command;
import org.debentialc.service.commands.CommandArgs;

import java.io.IOException;

import static org.debentialc.boosters.events.GiveTpsCommandInterceptor.applyBoosterAndGiveTPs;

public class GiveTpsCommand extends BaseCommand {
    @Command(aliases = "dartps", inGameOnly = false, permission = "debentialc.dartps", name = "dartps")
    @Override
    public void onCommand(CommandArgs command) throws IOException {
        String targetName = command.getArgs(0);
        Player target = Main.instance.getServer().getPlayer(targetName);
        if (target == null) {
            command.getSender().sendMessage(CC.translate("&cJugador no encontrado: " + targetName));
            return;
        }
        int baseTPs = Integer.parseInt(command.getArgs(1));
        String trainingId = command.length() >= 3 ? command.getArgs(2) : null;
        applyBoosterAndGiveTPs(command.getSender(), target, baseTPs, trainingId);
    }
}
