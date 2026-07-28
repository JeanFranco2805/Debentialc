package org.debentialc.boosters.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.debentialc.boosters.core.BoosterParser;
import org.debentialc.boosters.core.BoosterSettings;
import org.debentialc.boosters.managers.GlobalBoosterManager;
import org.debentialc.boosters.managers.PersonalBoosterManager;
import org.debentialc.boosters.models.GlobalBooster;
import org.debentialc.boosters.models.PersonalBooster;
import org.debentialc.boosters.storage.BoosterStorage;
import org.debentialc.customitems.tools.permissions.Permissions;
import org.debentialc.service.CC;
import org.debentialc.service.commands.BaseCommand;
import org.debentialc.service.commands.Command;
import org.debentialc.service.commands.CommandArgs;

import java.io.IOException;

public class BoosterCommand extends BaseCommand {

    @Command(name = "booster", aliases = {"booster", "boosters", "boost"}, permission = Permissions.COMMAND + "booster"
    , inGameOnly = false)
    @Override
    public void onCommand(CommandArgs command) throws IOException {
        CommandSender sender = command.getSender();

        if (command.length() < 1) {
            sendHelp(sender);
            return;
        }

        String arg0 = command.getArgs(0);

        switch (arg0.toLowerCase()) {
            case "global":
                handleGlobalCommand(command);
                break;

            case "personal":
                handlePersonalCommand(command);
                break;

            case "info":
                handleInfoCommand(command);
                break;

            case "save":
                BoosterStorage.saveAllData();
                sender.sendMessage(CC.translate("&a✓ Datos guardados."));
                break;

            case "load":
                BoosterStorage.loadAllData();
                sender.sendMessage(CC.translate("&a✓ Datos cargados."));
                break;

            case "help":
                sendHelp(sender);
                break;

            default:
                sender.sendMessage(CC.translate("&cSubcomando desconocido: &f" + arg0));
                sendHelp(sender);
                break;
        }
    }

    private void handleGlobalCommand(CommandArgs command) {
        CommandSender sender = command.getSender();
        if (command.length() < 2) {
            sender.sendMessage(CC.translate("&cUso: /booster global <activate|deactivate|info>"));
            return;
        }

        String action = command.getArgs(1).toLowerCase();

        switch (action) {
            case "activate":
            case "activar":
                if (command.length() < 3) {
                    sender.sendMessage(CC.translate("&cUso: /booster global activate <porcentaje> [tiempo]"));
                    sender.sendMessage(CC.translate("&7Ej: /booster global activate 50% 1h"));
                    return;
                }

                try {
                    double multiplier = BoosterParser.parsePercentageToMultiplier(command.getArgs(2));
                    long duration = BoosterSettings.getGlobalBoosterDuration();
                    if (command.length() >= 4) {
                        duration = BoosterParser.parseTimeToSeconds(command.getArgs(3));
                    }

                    GlobalBoosterManager.activateBooster(multiplier, sender.getName(), duration);

                    String percent = BoosterParser.formatMultiplierAsPercentage(multiplier);
                    String time = BoosterParser.formatSecondsToTime(duration);

                    sender.sendMessage(CC.translate("&a✓ Booster global activado: &6" + percent + " &apor &6" + time));
                    Bukkit.broadcastMessage(CC.translate("&8&m━━━━━━━━━━━━━━━━━━━━━━━"));
                    Bukkit.broadcastMessage(CC.translate("&6&l⚡ BOOSTER GLOBAL ⚡"));
                    Bukkit.broadcastMessage(CC.translate("&7Bono: &a" + percent));
                    Bukkit.broadcastMessage(CC.translate("&7Tiempo: &f" + time));
                    Bukkit.broadcastMessage(CC.translate("&7Por: &6" + sender.getName()));
                    Bukkit.broadcastMessage(CC.translate("&8&m━━━━━━━━━━━━━━━━━━━━━━━"));

                } catch (IllegalArgumentException e) {
                    sender.sendMessage(CC.translate("&c✗ " + e.getMessage()));
                }
                break;

            case "deactivate":
            case "desactivar":
                if (GlobalBoosterManager.isBoosterActive()) {
                    GlobalBoosterManager.deactivateBooster();
                    sender.sendMessage(CC.translate("&a✓ Booster global desactivado."));
                    Bukkit.broadcastMessage(CC.translate("&e[BOOSTER] &cGlobal desactivado"));
                } else {
                    sender.sendMessage(CC.translate("&cNo hay booster global activo."));
                }
                break;

            case "info":
            case "multiplier":
                GlobalBooster global = GlobalBoosterManager.getActiveBooster();
                if (global != null) {
                    sender.sendMessage(CC.translate("&6=== Global ==="));
                    sender.sendMessage(CC.translate("&7Bono: &a" + BoosterParser.formatMultiplierAsPercentage(global.getMultiplier())));
                    sender.sendMessage(CC.translate("&7Restante: &6" + global.getFormattedTime()));
                    sender.sendMessage(CC.translate("&7Por: &6" + global.getActivatedBy()));
                } else {
                    sender.sendMessage(CC.translate("&cGlobal inactivo."));
                }
                break;

            default:
                sender.sendMessage(CC.translate("&cAcción desconocida: &f" + action));
                break;
        }
    }

    private void handlePersonalCommand(CommandArgs command) {
        CommandSender sender = command.getSender();
        if (command.length() < 2) {
            sender.sendMessage(CC.translate("&cUso: /booster personal <add|remove|info>"));
            return;
        }

        String action = command.getArgs(1).toLowerCase();

        switch (action) {
            case "add":
            case "añadir":
                if (command.length() < 4) {
                    sender.sendMessage(CC.translate("&cUso: /booster personal add <jugador> <porcentaje> [tiempo]"));
                    sender.sendMessage(CC.translate("&7Ej: /booster personal add DelawareX 50% 2h"));
                    return;
                }

                Player target = Bukkit.getPlayer(command.getArgs(2));
                if (target == null) {
                    sender.sendMessage(CC.translate("&cJugador no encontrado: &f" + command.getArgs(2)));
                    return;
                }

                try {
                    double multiplier = BoosterParser.parsePercentageToMultiplier(command.getArgs(3));
                    long duration = 0;
                    if (command.length() >= 5) {
                        duration = BoosterParser.parseTimeToSeconds(command.getArgs(4));
                    }

                    PersonalBoosterManager.setBooster(target.getUniqueId(), multiplier, duration);
                    String percent = BoosterParser.formatMultiplierAsPercentage(multiplier);
                    String time = duration > 0 ? BoosterParser.formatSecondsToTime(duration) : "∞";

                    sender.sendMessage(CC.translate("&a✓ Booster personal &6" + percent + " &aactivado a &6" + target.getName() + " &a(&7" + time + "&a)"));

                    target.sendMessage(CC.translate("&8&m━━━━━━━━━━━━━━━━━━━━━━━"));
                    target.sendMessage(CC.translate("&b&l⚡ BOOSTER PERSONAL ⚡"));
                    target.sendMessage(CC.translate("&7Bono de TPs: &a" + percent));
                    target.sendMessage(CC.translate("&7Duración: &f" + time));
                    target.sendMessage(CC.translate("&8&m━━━━━━━━━━━━━━━━━━━━━━━"));

                } catch (IllegalArgumentException e) {
                    sender.sendMessage(CC.translate("&c✗ " + e.getMessage()));
                }
                break;

            case "remove":
            case "quitar":
                if (command.length() < 3) {
                    sender.sendMessage(CC.translate("&cUso: /booster personal remove <jugador>"));
                    return;
                }

                Player targetRemove = Bukkit.getPlayer(command.getArgs(2));
                if (targetRemove == null) {
                    sender.sendMessage(CC.translate("&cJugador no encontrado: &f" + command.getArgs(2)));
                    return;
                }

                if (PersonalBoosterManager.hasActiveBooster(targetRemove.getUniqueId())) {
                    PersonalBoosterManager.removeBooster(targetRemove.getUniqueId());
                    sender.sendMessage(CC.translate("&a✓ Booster personal removido de &6" + targetRemove.getName()));
                    targetRemove.sendMessage(CC.translate("&c✗ Tu booster personal ha sido removido."));
                } else {
                    sender.sendMessage(CC.translate("&c" + targetRemove.getName() + " &cno tiene booster personal activo."));
                }
                break;

            case "info":
                Player targetInfo = null;
                if (command.length() >= 3) {
                    targetInfo = Bukkit.getPlayer(command.getArgs(2));
                } else if (command.isPlayer()) {
                    targetInfo = command.getPlayer();
                }

                if (targetInfo == null) {
                    sender.sendMessage(CC.translate("&cUso: /booster personal info [jugador]"));
                    return;
                }

                PersonalBooster personal = PersonalBoosterManager.getActiveBooster(targetInfo.getUniqueId());
                if (personal != null) {
                    String percent = BoosterParser.formatMultiplierAsPercentage(personal.getMultiplier());
                    String time = personal.getDurationSeconds() > 0
                            ? BoosterParser.formatSecondsToTime(personal.getActivationTimeRemaining())
                            : "∞";
                    sender.sendMessage(CC.translate("&6=== Personal: &f" + targetInfo.getName() + " &6==="));
                    sender.sendMessage(CC.translate("&7Bono: &a" + percent));
                    sender.sendMessage(CC.translate("&7Restante: &f" + time));
                } else {
                    sender.sendMessage(CC.translate("&c" + targetInfo.getName() + " &cno tiene booster personal activo."));
                }
                break;

            default:
                sender.sendMessage(CC.translate("&cAcción desconocida: &f" + action));
                break;
        }
    }

    private void handleInfoCommand(CommandArgs command) {
        CommandSender sender = command.getSender();
        sender.sendMessage(CC.translate("&6=== Boosters ==="));

        GlobalBooster global = GlobalBoosterManager.getActiveBooster();
        if (global != null) {
            sender.sendMessage(CC.translate("&7Global: &a" + BoosterParser.formatMultiplierAsPercentage(global.getMultiplier()) + " &7(&f" + global.getFormattedTime() + "&7)"));
        } else {
            sender.sendMessage(CC.translate("&7Global: &cInactivo"));
        }

        if (command.isPlayer()) {
            Player player = command.getPlayer();
            PersonalBooster personal = PersonalBoosterManager.getActiveBooster(player.getUniqueId());
            if (personal != null) {
                sender.sendMessage(CC.translate("&7Personal: &a" + BoosterParser.formatMultiplierAsPercentage(personal.getMultiplier())));
            } else {
                sender.sendMessage(CC.translate("&7Personal: &cInactivo"));
            }
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(CC.translate("&6&lBoosters"));
        sender.sendMessage(CC.translate("&a/booster global activate <porcentaje> [tiempo]"));
        sender.sendMessage(CC.translate("&a/booster global deactivate"));
        sender.sendMessage(CC.translate("&a/booster global info"));
        sender.sendMessage(CC.translate("&a/booster personal add <jugador> <porcentaje> [tiempo]"));
        sender.sendMessage(CC.translate("&a/booster personal remove <jugador>"));
        sender.sendMessage(CC.translate("&a/booster personal info [jugador]"));
        sender.sendMessage(CC.translate("&a/booster info"));
    }
}
