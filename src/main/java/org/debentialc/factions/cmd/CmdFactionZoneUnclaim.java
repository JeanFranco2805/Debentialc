package org.debentialc.factions.cmd;

import com.massivecraft.factions.Factions;
import com.massivecraft.factions.Rel;
import com.massivecraft.factions.cmd.CmdFactions;
import com.massivecraft.factions.cmd.CmdFactionsUnclaim;
import com.massivecraft.factions.cmd.FactionsCommand;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.massivecore.ps.PS;
import org.bukkit.Bukkit;
import org.debentialc.factions.zones.FactionZone;
import org.debentialc.factions.zones.FactionZoneManager;
import org.debentialc.service.CC;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class CmdFactionZoneUnclaim extends FactionsCommand {

    public CmdFactionZoneUnclaim() {
        this.addAliases("unclaim");
        this.desc = "Desreclamar territorio";
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

        if (!isZoneClaimedByFaction(zone, msenderFaction)) {
            msg("&c✗ Tu facción no reclamó esta zona.");
            return;
        }

        boolean canDecide = msender.getRole().isAtLeast(Rel.OFFICER);
        if (!canDecide) {
            msg("&c✗ Solo el &flíder &cu &foficiales &cpueden decidir abandonar una zona especial.");
            return;
        }

        List<FactionZone> claimedZones = new ArrayList<FactionZone>();
        for (FactionZone z : FactionZoneManager.getInstance().getAllZones().values()) {
            if (isZoneClaimedByFaction(z, msenderFaction)) {
                claimedZones.add(z);
            }
        }

        for (FactionZone z : claimedZones) {
            for (String chunkKey : z.getChunks()) {
                String[] parts = chunkKey.split(",");
                int cx = Integer.parseInt(parts[0]);
                int cz = Integer.parseInt(parts[1]);
                PS ps = PS.valueOf(z.getWorld(), null, null, null, null, null, null, cx, cz, null, null, null, null, null);
                Faction wilderness = Faction.get("none");
                BoardColl.get().setFactionAt(ps, wilderness);
            }
        }

        msg("&a✓ Tu facción ha abandonado todas sus zonas especiales.");
        Bukkit.broadcastMessage(CC.translate("&a✓ La facción &f" + msenderFaction.getName() + " &aha abandonado sus zonas especiales."));
    }

    private void delegateToOriginal() {
        try {
            CmdFactions outerCmd = Factions.get().getOuterCmdFactions();
            Field field = CmdFactions.class.getField("cmdFactionsUnclaim");
            CmdFactionsUnclaim original = (CmdFactionsUnclaim) field.get(outerCmd);
            if (original != null) {
                original.execute(this.sender, this.args);
            } else {
                msg("&cError interno: no se encontró el comando original de unclaim.");
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
