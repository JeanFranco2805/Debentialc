package org.debentialc.factions.cmd;

import com.massivecraft.factions.cmd.FactionsCommand;
import com.massivecraft.factions.entity.Faction;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.debentialc.factions.FactionWarp;
import org.debentialc.factions.FactionWarpStorage;
import org.debentialc.factions.menus.FactionWarpMenu;

public class CmdFactionWarpCommand extends FactionsCommand {

    public CmdFactionWarpCommand() {
        this.addAliases("warp");
        this.desc = "Teletransportate a un warp de tu faction";
        this.addOptionalArg("nombre", "");
    }

    public void perform() {
        if (!msender.hasFaction()) {
            msg("&cNo eres miembro de ninguna faction.");
            return;
        }

        if (msenderFaction.isNone()) {
            msg("&cNo tienes warps sin faction.");
            return;
        }

        String factionId = msenderFaction.getId();

        if (argIsSet(0)) {
            String warpName = arg(0);
            teleportToWarp(me, factionId, warpName);
        } else {
            FactionWarpMenu.open(me, msenderFaction);
        }
    }

    public static boolean teleportToWarp(Player player, String factionId, String warpName) {
        FactionWarp warp = FactionWarpStorage.getInstance().getWarp(factionId, warpName);
        if (warp == null) {
            player.sendMessage(org.debentialc.service.CC.translate("&c✗ Warp no encontrado: &f" + warpName));
            return false;
        }

        Location location = warp.toLocation();
        if (location == null) {
            player.sendMessage(org.debentialc.service.CC.translate("&c✗ El mundo de este warp ya no existe."));
            return false;
        }

        player.teleport(location);
        player.sendMessage(org.debentialc.service.CC.translate("&a✓ Teletransportado a &e" + warp.getName()));
        return true;
    }
}
