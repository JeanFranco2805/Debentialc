package org.debentialc.factions.cmd;

import com.massivecraft.factions.cmd.FactionsCommand;
import com.massivecraft.factions.entity.MPlayer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.debentialc.service.CC;

public class CmdFactionGivePower extends FactionsCommand {

    public CmdFactionGivePower() {
        this.addAliases("givepower");
        this.desc = "Ajusta el power de un jugador o facción";
        this.addRequiredArg("cantidad");
        this.addOptionalArg("target", "jugador|faction");
    }

    public void perform() {
        if (!sender.hasPermission("dbcplugin.factions.power")) {
            msg(CC.translate("&c✗ No tenés permiso para usar este comando."));
            return;
        }

        String amountArg = arg(0);
        double amount;
        try {
            amount = Double.parseDouble(amountArg);
        } catch (NumberFormatException e) {
            msg(CC.translate("&c✗ Cantidad inválida: &f" + amountArg));
            return;
        }

        if (args.size() < 2) {
            if (me == null) {
                msg(CC.translate("&c✗ Debés ser un jugador o especificar un target."));
                return;
            }
            msender.setPower(amount);
            msg(CC.translate("&a✓ Tu power ahora es &f" + amount + "&a."));
            return;
        }

        String target = arg(1);
        if (target.equalsIgnoreCase("faction")) {
            if (msenderFaction == null || msenderFaction.isNone()) {
                msg(CC.translate("&c✗ No estás en una facción."));
                return;
            }
            for (MPlayer member : msenderFaction.getMPlayers()) {
                member.setPower(amount);
            }
            msg(CC.translate("&a✓ Power de todos los miembros de &f" + msenderFaction.getName() + " &aajustado a &f" + amount + "&a."));
            return;
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(target);
        if (offlinePlayer == null || (!offlinePlayer.hasPlayedBefore() && offlinePlayer.getPlayer() == null)) {
            msg(CC.translate("&c✗ Jugador no encontrado: &f" + target));
            return;
        }
        MPlayer targetMPlayer = MPlayer.get(offlinePlayer);
        if (targetMPlayer == null) {
            msg(CC.translate("&c✗ Jugador no encontrado: &f" + target));
            return;
        }

        // Si te das power a vos mismo, mostramos un solo mensaje
        if (msender != null && targetMPlayer.getId().equals(msender.getId())) {
            targetMPlayer.setPower(amount);
            msg(CC.translate("&a✓ Tu power ahora es &f" + amount + "&a."));
            return;
        }

        targetMPlayer.setPower(amount);
        msg(CC.translate("&a✓ Power de &f" + targetMPlayer.getName() + " &aajustado a &f" + amount + "&a."));
        if (targetMPlayer.isOnline()) {
            targetMPlayer.getPlayer().sendMessage(CC.translate("&a✓ Tu power ahora es &f" + amount + "&a."));
        }
    }
}
