package org.debentialc.raids.commands;

import org.debentialc.raids.managers.*;
import org.debentialc.raids.models.*;
import org.debentialc.service.commands.BaseCommand;
import org.debentialc.service.commands.Command;
import org.debentialc.service.commands.CommandArgs;
import org.debentialc.service.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class PartyCommand extends BaseCommand {

    private static final Map<UUID, PendingInvite> pendingInvites = new HashMap<>();

    public static class PendingInvite {
        public final String partyId;
        public final UUID inviterUuid;
        public final long timestamp;

        public PendingInvite(String partyId, UUID inviterUuid) {
            this.partyId = partyId;
            this.inviterUuid = inviterUuid;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return (System.currentTimeMillis() - timestamp) > 60000;
        }
    }

    public static void clearInvite(UUID playerId) {
        pendingInvites.remove(playerId);
    }

    @Command(name = "party",
            description = "Gestiona tu party",
            usage = "/party <create|invite|accept|leave|start|info|members|disband>",
            inGameOnly = true)
    public void onCommand(CommandArgs args) {
        Player player = args.getPlayer();
        if (player == null) {
            return;
        }

        if (args.length() == 0) {
            sendPartyHelp(player);
            return;
        }

        String subCommand = args.getArgs(0).toLowerCase();

        switch (subCommand) {
            case "create":
                handleCreate(player);
                break;

            case "invite":
                if (args.length() < 2) {
                    sendError(player, "Uso: /party invite <jugador>");
                    return;
                }
                handleInvite(player, args.getArgs(1));
                break;

            case "accept":
                handleAccept(player);
                break;

            case "deny":
            case "decline":
            case "reject":
                handleDeny(player);
                break;

            case "leave":
                handleLeave(player);
                break;

            case "start":
                if (args.length() < 2) {
                    sendError(player, "Uso: /party start <raid>");
                    return;
                }
                handleStart(player, args.getArgs(1));
                break;

            case "info":
                handleInfo(player);
                break;

            case "members":
                handleMembers(player);
                break;

            case "disband":
                handleDisband(player);
                break;

            default:
                sendError(player, "Subcomando no válido: " + subCommand);
                sendPartyHelp(player);
        }
    }

    private void handleCreate(Player player) {
        Party existingParty = PartyManager.getPlayerParty(player.getUniqueId());
        if (existingParty != null) {
            sendError(player, "Ya estás en una party");
            return;
        }

        Party party = PartyManager.createParty(player.getUniqueId(), 5);
        sendSuccess(player, "Party creada. Invita jugadores con: /party invite <jugador>");
    }

    private void handleInvite(Player player, String playerName) {
        Party party = PartyManager.getPlayerParty(player.getUniqueId());

        if (party == null) {
            sendError(player, "No estás en una party. Crea una con /party create");
            return;
        }

        if (!party.isLeader(player.getUniqueId())) {
            sendError(player, "Solo el líder puede invitar jugadores");
            return;
        }

        if (party.isFull()) {
            sendError(player, "La party está llena (máximo " + party.getMaxSize() + ")");
            return;
        }

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sendError(player, "Jugador no encontrado: " + playerName);
            return;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            sendError(player, "No puedes invitarte a ti mismo");
            return;
        }

        if (PartyManager.isPlayerInParty(target.getUniqueId())) {
            sendError(player, target.getName() + " ya está en una party");
            return;
        }

        PendingInvite existing = pendingInvites.get(target.getUniqueId());
        if (existing != null && !existing.isExpired()) {
            sendError(player, target.getName() + " ya tiene una invitación pendiente");
            return;
        }

        pendingInvites.put(target.getUniqueId(), new PendingInvite(party.getPartyId(), player.getUniqueId()));

        sendSuccess(player, "Invitación enviada a " + target.getName());

        target.sendMessage("");
        target.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        target.sendMessage(CC.translate("&6&l  INVITACIÓN DE PARTY"));
        target.sendMessage("");
        target.sendMessage(CC.translate("&f  " + player.getName() + " &7te invitó a su party"));
        target.sendMessage("");
        target.sendMessage(CC.translate("&a  /party accept &7- Aceptar"));
        target.sendMessage(CC.translate("&c  /party deny &7- Rechazar"));
        target.sendMessage("");
        target.sendMessage(CC.translate("&7  La invitación expira en 60 segundos"));
        target.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        target.sendMessage("");

        final UUID targetId = target.getUniqueId();
        Bukkit.getScheduler().scheduleSyncDelayedTask(
                org.debentialc.Main.instance,
                () -> {
                    PendingInvite invite = pendingInvites.get(targetId);
                    if (invite != null && invite.isExpired()) {
                        pendingInvites.remove(targetId);
                        Player targetPlayer = Bukkit.getPlayer(targetId);
                        if (targetPlayer != null) {
                            targetPlayer.sendMessage(CC.translate("&7La invitación de party ha expirado"));
                        }
                    }
                },
                1200L
        );
    }

    private void handleAccept(Player player) {
        PendingInvite invite = pendingInvites.get(player.getUniqueId());

        if (invite == null) {
            sendError(player, "No tienes invitaciones pendientes");
            return;
        }

        if (invite.isExpired()) {
            pendingInvites.remove(player.getUniqueId());
            sendError(player, "La invitación ha expirado");
            return;
        }

        if (PartyManager.isPlayerInParty(player.getUniqueId())) {
            pendingInvites.remove(player.getUniqueId());
            sendError(player, "Ya estás en una party. Usa /party leave primero");
            return;
        }

        Party party = PartyManager.getPartyById(invite.partyId);
        if (party == null) {
            pendingInvites.remove(player.getUniqueId());
            sendError(player, "La party ya no existe");
            return;
        }

        if (party.isFull()) {
            pendingInvites.remove(player.getUniqueId());
            sendError(player, "La party está llena");
            return;
        }

        boolean joined = PartyManager.joinParty(player.getUniqueId(), party);
        pendingInvites.remove(player.getUniqueId());

        if (joined) {
            sendSuccess(player, "Te has unido a la party");

            Player inviter = Bukkit.getPlayer(invite.inviterUuid);
            if (inviter != null) {
                sendSuccess(inviter, player.getName() + " aceptó la invitación");
            }

            for (UUID memberId : party.getActivePlayers()) {
                if (!memberId.equals(player.getUniqueId()) && !memberId.equals(invite.inviterUuid)) {
                    Player member = Bukkit.getPlayer(memberId);
                    if (member != null) {
                        sendInfo(member, player.getName() + " se unió a la party");
                    }
                }
            }
        } else {
            sendError(player, "No se pudo unir a la party");
        }
    }

    private void handleDeny(Player player) {
        PendingInvite invite = pendingInvites.remove(player.getUniqueId());

        if (invite == null) {
            sendError(player, "No tienes invitaciones pendientes");
            return;
        }

        sendSuccess(player, "Invitación rechazada");

        Player inviter = Bukkit.getPlayer(invite.inviterUuid);
        if (inviter != null) {
            sendError(inviter, player.getName() + " rechazó la invitación");
        }
    }

    private void handleLeave(Player player) {
        Party party = PartyManager.getPlayerParty(player.getUniqueId());

        if (party == null) {
            sendError(player, "No estás en una party");
            return;
        }

        PartyManager.leaveParty(player.getUniqueId());
        sendSuccess(player, "Has abandonado la party");
    }

    private void handleStart(Player player, String raidName) {
        Party party = PartyManager.getPlayerParty(player.getUniqueId());

        if (party == null) {
            sendError(player, "No estás en una party");
            return;
        }

        if (!party.isLeader(player.getUniqueId())) {
            sendError(player, "Solo el líder puede iniciar la raid");
            return;
        }

        if (!PartyManager.canStartRaid(party)) {
            sendError(player, "Se necesitan al menos 2 jugadores (actual: " + party.getMemberCount() + ")");
            return;
        }

        Raid raid = RaidManager.getRaidByName(raidName);
        if (raid == null) {
            sendError(player, "Raid no encontrada: " + raidName);
            return;
        }

        if (!RaidManager.isRaidReadyToPlay(raid.getRaidId())) {
            sendError(player, "La raid no está configurada correctamente");
            return;
        }

        // =====================================================
        // VALIDACIÓN DE COOLDOWN - Todos los jugadores
        // =====================================================
        for (UUID memberId : party.getActivePlayers()) {
            if (CooldownManager.hasCooldown(memberId, raid.getRaidId())) {
                Player member = Bukkit.getPlayer(memberId);
                String memberName = member != null ? member.getName() : memberId.toString();
                long remaining = CooldownManager.getCooldownRemaining(memberId, raid.getRaidId());
                String timeFormatted = CooldownManager.getCooldownFormattedTime(memberId, raid.getRaidId());

                player.sendMessage("");
                player.sendMessage(CC.translate("&c✗ &f" + memberName + " tiene cooldown activo"));
                player.sendMessage(CC.translate("&7Tiempo restante: &f" + timeFormatted));
                player.sendMessage("");
                return;
            }
        }

        if (RaidSessionManager.hasActiveSession(raid.getRaidId())) {
            sendError(player, "Otra party ya está haciendo esta raid. Espera a que terminen.");
            return;
        }

        List<Player> partyPlayers = new ArrayList<>();
        for (UUID memberId : party.getActivePlayers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null) {
                partyPlayers.add(member);
            }
        }

        if (partyPlayers.size() < raid.getMinPlayers()) {
            sendError(player, "Se necesitan al menos " + raid.getMinPlayers() + " jugadores online (actual: " + partyPlayers.size() + ")");
            return;
        }
        if (partyPlayers.size() > raid.getMaxPlayers()) {
            sendError(player, "Máximo " + raid.getMaxPlayers() + " jugadores permitidos (actual: " + partyPlayers.size() + ")");
            return;
        }

        boolean started = RaidSessionManager.startRaid(raid, partyPlayers, player);
        if (started) {
            sendSuccess(player, "Raid iniciada: " + raid.getDisplayName());
        } else {
            sendError(player, "No se pudo iniciar la raid. Verifica la configuración.");
        }
    }

    private void handleInfo(Player player) {
        Party party = PartyManager.getPlayerParty(player.getUniqueId());

        if (party == null) {
            sendError(player, "No estás en una party");
            return;
        }

        player.sendMessage(PartyManager.getPartyInfo(party));
    }

    private void handleMembers(Player player) {
        Party party = PartyManager.getPlayerParty(player.getUniqueId());

        if (party == null) {
            sendError(player, "No estás en una party");
            return;
        }

        player.sendMessage(PartyManager.listPartyMembers(party));
    }

    private void handleDisband(Player player) {
        Party party = PartyManager.getPlayerParty(player.getUniqueId());

        if (party == null) {
            sendError(player, "No estás en una party");
            return;
        }

        if (!party.isLeader(player.getUniqueId())) {
            sendError(player, "Solo el líder puede disolver la party");
            return;
        }

        for (UUID memberId : party.getActivePlayers()) {
            if (!memberId.equals(player.getUniqueId())) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null) {
                    sendError(member, "La party ha sido disuelta por el líder");
                }
            }
        }

        PartyManager.dissolveParty(party.getPartyId());
        sendSuccess(player, "Party disuelta");
    }

    private void sendPartyHelp(Player player) {
        player.sendMessage("");
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage(CC.translate("&6&l  COMANDOS DE PARTY"));
        player.sendMessage("");
        player.sendMessage(CC.translate("&f  /party create &7- Crear nueva party"));
        player.sendMessage(CC.translate("&f  /party invite <jugador> &7- Invitar jugador"));
        player.sendMessage(CC.translate("&f  /party accept &7- Aceptar invitación"));
        player.sendMessage(CC.translate("&f  /party deny &7- Rechazar invitación"));
        player.sendMessage(CC.translate("&f  /party leave &7- Abandonar party"));
        player.sendMessage(CC.translate("&f  /party start <raid> &7- Iniciar raid (solo líder)"));
        player.sendMessage(CC.translate("&f  /party info &7- Ver información de party"));
        player.sendMessage(CC.translate("&f  /party members &7- Ver miembros"));
        player.sendMessage(CC.translate("&f  /party disband &7- Disolver party (solo líder)"));
        player.sendMessage("");
        player.sendMessage(CC.translate("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage("");
    }

    private void sendError(Player player, String message) {
        player.sendMessage("");
        player.sendMessage(CC.translate("&c✗ &f" + message));
        player.sendMessage("");
    }

    private void sendSuccess(Player player, String message) {
        player.sendMessage("");
        player.sendMessage(CC.translate("&a✓ &f" + message));
        player.sendMessage("");
    }

    private void sendInfo(Player player, String message) {
        player.sendMessage("");
        player.sendMessage(CC.translate("&bℹ &f" + message));
        player.sendMessage("");
    }
}