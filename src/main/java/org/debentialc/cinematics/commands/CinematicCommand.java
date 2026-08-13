package org.debentialc.cinematics.commands;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.debentialc.cinematics.manager.CinematicManager;
import org.debentialc.cinematics.manager.CinematicSessionManager;
import org.debentialc.cinematics.model.Cinematic;
import org.debentialc.cinematics.model.CinematicEvent;
import org.debentialc.cinematics.model.CinematicEvent.Type;
import org.debentialc.cinematics.model.Waypoint;
import org.debentialc.service.CC;
import org.debentialc.service.commands.BaseCommand;
import org.debentialc.service.commands.Command;
import org.debentialc.service.commands.CommandArgs;

import java.io.IOException;

public class CinematicCommand extends BaseCommand {

    private final CinematicManager manager;

    public CinematicCommand(CinematicManager manager) {
        this.manager = manager;
    }

    @Command(name = "cinematic", aliases = {"cine", "cin"}, permission = "dbcplugin.cinematic.admin", inGameOnly = false)
    @Override
    public void onCommand(CommandArgs command) throws IOException {
        if (command.length() < 1) {
            sendHelp(command.getSender());
            return;
        }

        String sub = command.getArgs(0).toLowerCase();
        switch (sub) {
            case "create":
                handleCreate(command);
                break;
            case "delete":
                handleDelete(command);
                break;
            case "addpoint":
                handleAddPoint(command);
                break;
            case "addevent":
                handleAddEvent(command);
                break;
            case "play":
                handlePlay(command);
                break;
            case "stop":
                handleStop(command);
                break;
            case "list":
                handleList(command);
                break;
            case "info":
                handleInfo(command);
                break;
            case "reload":
                manager.loadAll();
                command.getSender().sendMessage(CC.translate("&7[&aCinematic&7] &fCinematics recargadas."));
                break;
            case "help":
            default:
                sendHelp(command.getSender());
                break;
        }
    }

    private void handleCreate(CommandArgs command) {
        if (command.length() < 2) {
            command.getSender().sendMessage(CC.translate("&7[&cCinematic&7] &fUso: /cinematic create <id>"));
            return;
        }
        String id = command.getArgs(1);
        if (manager.getCinematic(id) != null) {
            command.getSender().sendMessage(CC.translate("&7[&cCinematic&7] &fYa existe una cinematica con ese ID."));
            return;
        }
        Cinematic cinematic = new Cinematic(id);
        manager.save(cinematic);
        command.getSender().sendMessage(CC.translate("&7[&aCinematic&7] &fCinematica '&7" + id + "&f' creada."));
    }

    private void handleDelete(CommandArgs command) {
        if (command.length() < 2) {
            command.getSender().sendMessage(CC.translate("&7[&cCinematic&7] &fUso: /cinematic delete <id>"));
            return;
        }
        String id = command.getArgs(1);
        if (manager.getCinematic(id) == null) {
            command.getSender().sendMessage(CC.translate("&7[&cCinematic&7] &fCinematica no encontrada."));
            return;
        }
        manager.delete(id);
        command.getSender().sendMessage(CC.translate("&7[&aCinematic&7] &fCinematica '&7" + id + "&f' eliminada."));
    }

    private void handleAddPoint(CommandArgs command) {
        if (!command.isPlayer()) {
            command.getSender().sendMessage(CC.translate("&7[&cCinematic&7] &fSolo jugadores."));
            return;
        }
        if (command.length() < 3) {
            command.getSender().sendMessage(CC.translate("&7[&cCinematic&7] &fUso: /cinematic addpoint <id> <tick> [easing]"));
            return;
        }
        Player player = command.getPlayer();
        String id = command.getArgs(1);
        Cinematic cinematic = manager.getCinematic(id);
        if (cinematic == null) {
            command.getSender().sendMessage(CC.translate("&7[&cCinematic&7] &fCinematica no encontrada."));
            return;
        }
        long tick;
        try {
            tick = Long.parseLong(command.getArgs(2));
        } catch (NumberFormatException e) {
            command.getSender().sendMessage(CC.translate("&7[&cCinematic&7] &fTick invalido."));
            return;
        }
        Waypoint.Easing easing = Waypoint.Easing.LINEAR;
        if (command.length() >= 4) {
            try {
                easing = Waypoint.Easing.valueOf(command.getArgs(3).toUpperCase());
            } catch (Exception ignored) {
            }
        }
        cinematic.addWaypoint(new Waypoint(player.getLocation(), tick, easing));
        manager.save(cinematic);
        command.getSender().sendMessage(CC.translate("&7[&aCinematic&7] &fPunto anadido a '&7" + id + "&f' en tick &7" + tick + "&f."));
    }

    private void handleAddEvent(CommandArgs command) {
        if (command.length() < 5) {
            command.getSender().sendMessage(CC.translate("&7[&cCinematic&7] &fUso: /cinematic addevent <id> <tick> <type> <valor> [extra]"));
            command.getSender().sendMessage(CC.translate("&7Tipos: &fMESSAGE, TITLE, SUBTITLE, SOUND, COMMAND, PARTICLE, EFFECT"));
            return;
        }
        String id = command.getArgs(1);
        Cinematic cinematic = manager.getCinematic(id);
        if (cinematic == null) {
            command.getSender().sendMessage(CC.translate("&7[&cCinematic&7] &fCinematica no encontrada."));
            return;
        }
        long tick;
        try {
            tick = Long.parseLong(command.getArgs(2));
        } catch (NumberFormatException e) {
            command.getSender().sendMessage(CC.translate("&7[&cCinematic&7] &fTick invalido."));
            return;
        }
        Type type;
        try {
            type = Type.valueOf(command.getArgs(3).toUpperCase());
        } catch (Exception e) {
            command.getSender().sendMessage(CC.translate("&7[&cCinematic&7] &fTipo invalido."));
            return;
        }
        String value = command.getArgs(4);
        String extra = command.length() >= 6 ? command.getArgs(5) : null;
        cinematic.addEvent(new CinematicEvent(tick, type, value, extra));
        manager.save(cinematic);
        command.getSender().sendMessage(CC.translate("&7[&aCinematic&7] &fEvento anadido a '&7" + id + "&f' en tick &7" + tick + "&f."));
    }

    private void handlePlay(CommandArgs command) {
        if (!command.isPlayer()) {
            command.getSender().sendMessage(CC.translate("&7[&cCinematic&7] &fSolo jugadores."));
            return;
        }
        if (command.length() < 2) {
            command.getSender().sendMessage(CC.translate("&7[&cCinematic&7] &fUso: /cinematic play <id>"));
            return;
        }
        Player player = command.getPlayer();
        String id = command.getArgs(1);
        Cinematic cinematic = manager.getCinematic(id);
        if (cinematic == null) {
            command.getSender().sendMessage(CC.translate("&7[&cCinematic&7] &fCinematica no encontrada."));
            return;
        }
        CinematicSessionManager.play(player, new org.debentialc.cinematics.manager.CinematicPlayer(cinematic, player));
    }

    private void handleStop(CommandArgs command) {
        if (!command.isPlayer()) {
            command.getSender().sendMessage(CC.translate("&7[&cCinematic&7] &fSolo jugadores."));
            return;
        }
        CinematicSessionManager.stop(command.getPlayer());
        command.getPlayer().sendMessage(CC.translate("&7[&aCinematic&7] &fCinematica detenida."));
    }

    private void handleList(CommandArgs command) {
        if (manager.getAllCinematics().isEmpty()) {
            command.getSender().sendMessage(CC.translate("&7[&cCinematic&7] &fNo hay cinematicas."));
            return;
        }
        command.getSender().sendMessage(CC.translate("&7[&aCinematic&7] &fLista de cinematicas:"));
        for (org.debentialc.cinematics.model.Cinematic cinematic : manager.getAllCinematics()) {
            command.getSender().sendMessage(CC.translate(" &7- &f" + cinematic.getId() + " &7(" + cinematic.getWaypoints().size() + " puntos, " + cinematic.getEvents().size() + " eventos)"));
        }
    }

    private void handleInfo(CommandArgs command) {
        if (command.length() < 2) {
            command.getSender().sendMessage(CC.translate("&7[&cCinematic&7] &fUso: /cinematic info <id>"));
            return;
        }
        String id = command.getArgs(1);
        Cinematic cinematic = manager.getCinematic(id);
        if (cinematic == null) {
            command.getSender().sendMessage(CC.translate("&7[&cCinematic&7] &fCinematica no encontrada."));
            return;
        }
        command.getSender().sendMessage(CC.translate("&7[&aCinematic&7] &fInformacion de '&7" + id + "&f':"));
        command.getSender().sendMessage(CC.translate(" &7Duracion: &f" + cinematic.getDurationTicks() + " ticks"));
        command.getSender().sendMessage(CC.translate(" &7Puntos: &f" + cinematic.getWaypoints().size()));
        command.getSender().sendMessage(CC.translate(" &7Eventos: &f" + cinematic.getEvents().size()));
    }

    private void sendHelp(org.bukkit.command.CommandSender sender) {
        sender.sendMessage(CC.translate(""));
        sender.sendMessage(CC.translate("&7&m--------------------&r &6Cinematic &7&m--------------------"));
        sender.sendMessage(CC.translate(" &7/cinematic create <id>                  &fCrea una cinematica"));
        sender.sendMessage(CC.translate(" &7/cinematic delete <id>                  &fElimina una cinematica"));
        sender.sendMessage(CC.translate(" &7/cinematic addpoint <id> <tick> [ease] &fAnade punto de ruta"));
        sender.sendMessage(CC.translate(" &7/cinematic addevent <id> <tick> <tipo> <valor> &fAnade evento"));
        sender.sendMessage(CC.translate(" &7/cinematic play <id>                   &fReproduce la cinematica"));
        sender.sendMessage(CC.translate(" &7/cinematic stop                        &fDetiene la reproduccion"));
        sender.sendMessage(CC.translate(" &7/cinematic list                        &fLista cinematicas"));
        sender.sendMessage(CC.translate(" &7/cinematic info <id>                   &fInformacion"));
        sender.sendMessage(CC.translate(" &7/cinematic reload                      &fRecarga cinematicas"));
        sender.sendMessage(CC.translate("&7&m----------------------------------------------------"));
        sender.sendMessage(CC.translate(""));
    }
}
