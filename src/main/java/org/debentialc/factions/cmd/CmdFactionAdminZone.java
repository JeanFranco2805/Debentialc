package org.debentialc.factions.cmd;

import com.massivecraft.factions.cmd.FactionsCommand;
import org.debentialc.factions.menus.FactionAdminZoneMenu;

public class CmdFactionAdminZone extends FactionsCommand {

    public CmdFactionAdminZone() {
        this.addAliases("adminzone");
        this.desc = "Menú de administración de zonas";
    }

    public void perform() {
        if (!me.hasPermission("debentialc.factions.adminzone")) {
            msg("&cNo tienes permiso para usar este comando.");
            return;
        }
        FactionAdminZoneMenu.openMainMenu(me, 1);
    }
}
