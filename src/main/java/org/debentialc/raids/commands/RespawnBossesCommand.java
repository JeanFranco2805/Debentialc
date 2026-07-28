package org.debentialc.raids.commands;

import org.debentialc.Main;
import org.debentialc.service.CC;
import org.debentialc.service.commands.BaseCommand;
import org.debentialc.service.commands.Command;
import org.debentialc.service.commands.CommandArgs;

public class RespawnBossesCommand extends BaseCommand {

    @Command(name = "respawnBosses",
            aliases = {"respawnbosses", "rbosses"},
            permission = "dbcplugin.raids.admin",
            description = "Fuerza el respawn de todos los bosses registrados",
            usage = "/respawnBosses",
            inGameOnly = false)
    public void onCommand(CommandArgs args) {
        Main.respawnAllNpc();
        args.getSender().sendMessage(CC.translate("&a\u2713 Todos los bosses han sido forzados a respawnear."));
    }
}
