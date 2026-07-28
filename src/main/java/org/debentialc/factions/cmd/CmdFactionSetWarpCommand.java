package org.debentialc.factions.cmd;

import com.massivecraft.factions.cmd.FactionsCommand;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.massivecore.ps.PS;
import org.bukkit.Location;
import org.debentialc.factions.FactionWarp;
import org.debentialc.factions.FactionWarpStorage;

public class CmdFactionSetWarpCommand extends FactionsCommand {

    public CmdFactionSetWarpCommand() {
        this.addAliases("setwarp");
        this.desc = "Crea un warp en tu territorio";
        this.addRequiredArg("nombre");
    }

    public void perform() {
        if (!msender.hasFaction()) {
            msg("&cNo eres miembro de ninguna faction.");
            return;
        }

        if (msenderFaction.isNone()) {
            msg("&cNo puedes crear warps sin faction.");
            return;
        }

        String warpName = arg(0);
        if (warpName == null || warpName.trim().isEmpty()) {
            msg("&cDebes escribir un nombre para el warp.");
            return;
        }

        if (!me.isOnGround()) {
            msg("&cDebes estar en el suelo para setear un warp.");
            return;
        }

        Location location = me.getLocation();
        Faction factionAt = BoardColl.get().getFactionAt(PS.valueOf(location));
        if (factionAt == null || !factionAt.getId().equals(msenderFaction.getId())) {
            msg("&cSolo puedes crear warps dentro de tu propio territorio.");
            return;
        }

        FactionWarp warp = new FactionWarp(warpName, location);
        FactionWarpStorage.getInstance().setWarp(msenderFaction.getId(), warp);

        msg("&a✓ Warp &e" + warpName + " &acreado en tu territorio.");
    }
}
