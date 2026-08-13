package org.debentialc.factions.cmd;

import com.massivecraft.factions.cmd.FactionsCommand;
import org.debentialc.factions.menus.FactionZoneMenu;

public class CmdFactionZone extends FactionsCommand {

    public CmdFactionZone() {
        this.addAliases("zone", "zones");
        this.desc = "Ver zonas disponibles";
    }

    public void perform() {
        FactionZoneMenu.open(me, 1);
    }
}
