package org.debentialc.scoreboard.commands;

import org.bukkit.command.CommandSender;
import org.debentialc.scoreboard.ScoreboardModule;
import org.debentialc.service.CC;
import org.debentialc.service.commands.BaseCommand;
import org.debentialc.service.commands.Command;
import org.debentialc.service.commands.CommandArgs;

import java.io.IOException;

public class ScoreboardCommand extends BaseCommand {

    public ScoreboardCommand() {
    }

    @Command(name = "sb", aliases = {"dsc", "dscoreboard", "debscoreboard"}, permission = "dbcplugin.scoreboard.admin", inGameOnly = false)
    @Override
    public void onCommand(CommandArgs command) throws IOException {
        CommandSender sender = command.getSender();

        if (command.length() < 1) {
            sendHelp(sender);
            return;
        }

        String arg = command.getArgs(0).toLowerCase();
        switch (arg) {
            case "reload":
            case "recargar":
                ScoreboardModule.reload();
                sender.sendMessage(CC.translate("&7[&aScoreboard&7] &fConfiguracion recargada."));
                break;

            case "on":
            case "activar":
                if (!ScoreboardModule.getManager().isRunning()) {
                    ScoreboardModule.getManager().start();
                }
                sender.sendMessage(CC.translate("&7[&aScoreboard&7] &fActivado."));
                break;

            case "off":
            case "desactivar":
                if (ScoreboardModule.getManager().isRunning()) {
                    ScoreboardModule.getManager().stop();
                }
                sender.sendMessage(CC.translate("&7[&cScoreboard&7] &fDesactivado."));
                break;

            case "toggle":
                boolean enabled = !ScoreboardModule.getManager().isRunning();
                if (enabled) {
                    ScoreboardModule.getManager().start();
                    sender.sendMessage(CC.translate("&7[&aScoreboard&7] &fActivado."));
                } else {
                    ScoreboardModule.getManager().stop();
                    sender.sendMessage(CC.translate("&7[&cScoreboard&7] &fDesactivado."));
                }
                break;

            case "help":
            case "ayuda":
                sendHelp(sender);
                break;

            default:
                sender.sendMessage(CC.translate("&7[&cScoreboard&7] &fSubcomando desconocido: &7" + arg));
                sendHelp(sender);
                break;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(CC.translate(""));
        sender.sendMessage(CC.translate("&7&m--------------------&r &6Scoreboard &7&m--------------------"));
        sender.sendMessage(CC.translate(" &7/sb toggle      &fActiva o desactiva el scoreboard"));
        sender.sendMessage(CC.translate(" &7/sb on          &fActiva el scoreboard"));
        sender.sendMessage(CC.translate(" &7/sb off         &fDesactiva el scoreboard"));
        sender.sendMessage(CC.translate(" &7/sb reload      &fRecarga el archivo scoreboard.yml"));
        sender.sendMessage(CC.translate(" &7/sb help        &fMuestra este menu"));
        sender.sendMessage(CC.translate(" &7/scoreboard     &fAlias compatible con el comando vanilla"));
        sender.sendMessage(CC.translate("&7&m----------------------------------------------------"));
        sender.sendMessage(CC.translate(""));
    }
}
