package org.debentialc.factions.cmd;

import com.massivecraft.factions.Factions;
import com.massivecraft.factions.cmd.CmdFactions;
import com.massivecraft.factions.cmd.CmdFactionsClaim;
import com.massivecraft.factions.cmd.FactionsCommand;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.massivecore.ps.PS;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.debentialc.factions.zones.FactionZone;
import org.debentialc.factions.zones.FactionZoneManager;
import org.debentialc.service.CC;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

public class CmdFactionZoneClaim extends FactionsCommand {

    public CmdFactionZoneClaim() {
        this.addAliases("claim");
        this.desc = "Reclamar territorio";
        this.errorOnToManyArgs = false;
    }

    public void perform() {
        FactionZone zone = FactionZoneManager.getInstance().getZoneAtChunk(
                me.getWorld().getName(),
                me.getLocation().getChunk().getX(),
                me.getLocation().getChunk().getZ()
        );

        if (zone == null) {
            delegateToOriginal();
            return;
        }

        if (msenderFaction == null || msenderFaction.isNone()) {
            msg("&cNo eres miembro de ninguna facción.");
            return;
        }

        if (!msenderFaction.getId().equals(msenderFaction.getId())) {
            // no debería pasar
        }

        double power = msenderFaction.getPower();
        if (power < zone.getPowerCost()) {
            msg("&c✗ Tu facción necesita &f" + zone.getPowerCost() + " &c de power para reclamar &f" + zone.getDisplayName() + "&c. Power actual: &f" + (int) power);
            return;
        }

        if (isZoneClaimedByFaction(zone, msenderFaction)) {
            msg("&c✗ Tu facción ya reclamó &f" + zone.getDisplayName() + "&c.");
            return;
        }

        for (String chunkKey : zone.getChunks()) {
            String[] parts = chunkKey.split(",");
            int cx = Integer.parseInt(parts[0]);
            int cz = Integer.parseInt(parts[1]);
            PS ps = PS.valueOf(zone.getWorld(), null, null, null, null, null, null, cx, cz, null, null, null, null, null);
            Faction at = BoardColl.get().getFactionAt(ps);
            if (at != null && !at.isNone() && !at.getId().equals(msenderFaction.getId())) {
                msg("&c✗ La zona &f" + zone.getDisplayName() + " &cestá ocupada por otra facción.");
                return;
            }
        }

        for (String chunkKey : zone.getChunks()) {
            String[] parts = chunkKey.split(",");
            int cx = Integer.parseInt(parts[0]);
            int cz = Integer.parseInt(parts[1]);
            PS ps = PS.valueOf(zone.getWorld(), null, null, null, null, null, null, cx, cz, null, null, null, null, null);
            BoardColl.get().setFactionAt(ps, msenderFaction);
        }

        msg("&a✓ Tu facción ha reclamado toda la zona &f" + zone.getDisplayName() + "&a!");
        Bukkit.broadcastMessage(CC.translate("&a✓ La facción &f" + msenderFaction.getName() + " &aha reclamado la zona &f" + zone.getDisplayName() + "&a!"));
    }

    private void delegateToOriginal() {
        try {
            CmdFactions outerCmd = Factions.get().getOuterCmdFactions();
            Field field = CmdFactions.class.getField("cmdFactionsClaim");
            CmdFactionsClaim original = (CmdFactionsClaim) field.get(outerCmd);
            if (original != null) {
                original.execute(this.sender, this.args);
            } else {
                msg("&cError interno: no se encontró el comando original de claim.");
            }
        } catch (Exception e) {
            msg("&cError al delegar al comando original: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isZoneClaimedByFaction(FactionZone zone, Faction faction) {
        for (String chunkKey : zone.getChunks()) {
            String[] parts = chunkKey.split(",");
            int cx = Integer.parseInt(parts[0]);
            int cz = Integer.parseInt(parts[1]);
            PS ps = PS.valueOf(zone.getWorld(), null, null, null, null, null, null, cx, cz, null, null, null, null, null);
            Faction at = BoardColl.get().getFactionAt(ps);
            if (at != null && at.getId().equals(faction.getId())) {
                return true;
            }
        }
        return false;
    }
}
