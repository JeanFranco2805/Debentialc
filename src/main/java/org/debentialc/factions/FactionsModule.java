package org.debentialc.factions;

import com.massivecraft.factions.Factions;
import com.massivecraft.factions.cmd.CmdFactions;
import com.massivecraft.massivecore.cmd.MassiveCommand;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.Listener;
import org.debentialc.Main;
import org.debentialc.factions.cmd.CmdFactionAdminZone;
import org.debentialc.factions.cmd.CmdFactionGivePower;
import org.debentialc.factions.cmd.CmdFactionSetWarpCommand;
import org.debentialc.factions.cmd.CmdFactionWarpCommand;
import org.debentialc.factions.cmd.CmdFactionZone;
import org.debentialc.factions.cmd.CmdFactionZoneClaim;
import org.debentialc.factions.cmd.CmdFactionZoneUnclaim;
import org.debentialc.factions.zones.FactionZoneListener;

import java.util.ArrayList;
import java.util.List;

public class FactionsModule {

    public static void initialize() {
        Plugin factionsPlugin = Bukkit.getPluginManager().getPlugin("Factions");
        if (factionsPlugin == null || !factionsPlugin.isEnabled()) {
            Bukkit.getScheduler().runTaskLater(Main.instance, FactionsModule::initialize, 40L);
            return;
        }

        try {
            Factions factions = Factions.get();
            if (factions == null) {
                Bukkit.getScheduler().runTaskLater(Main.instance, FactionsModule::initialize, 40L);
                return;
            }

            CmdFactions outerCmd = factions.getOuterCmdFactions();
            if (outerCmd == null) {
                Bukkit.getScheduler().runTaskLater(Main.instance, FactionsModule::initialize, 40L);
                return;
            }

            outerCmd.addSubCommand(new CmdFactionZoneClaim(), 0);
            outerCmd.addSubCommand(new CmdFactionZoneUnclaim(), 0);
            outerCmd.addSubCommand(new CmdFactionSetWarpCommand());
            outerCmd.addSubCommand(new CmdFactionWarpCommand());
            outerCmd.addSubCommand(new CmdFactionAdminZone());
            outerCmd.addSubCommand(new CmdFactionZone());

            // Reemplazar el givepower por defecto de Factions por el nuestro
            List<MassiveCommand> subCommands = new ArrayList<>(outerCmd.getSubCommands());
            subCommands.removeIf(child -> {
                if (child == null) return false;
                if (child instanceof CmdFactionGivePower) return false;
                for (String alias : child.getAliases()) {
                    if (alias.equalsIgnoreCase("givepower")) return true;
                }
                return false;
            });
            outerCmd.setSubCommands(subCommands);

            outerCmd.addSubCommand(new CmdFactionGivePower());

            Main.instance.getServer().getPluginManager().registerEvents((Listener) new FactionZoneListener(), Main.instance);

            Main.instance.getLogger().info("[Factions] Comandos /f setwarp, /f warp, /f adminzone, /f zone, /f claim (zone), /f unclaim (zone) y /f givepower registrados.");
        } catch (Exception e) {
            Main.instance.getLogger().warning("[Factions] Error registrando comandos: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
