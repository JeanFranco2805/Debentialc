package org.debentialc.raids.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.debentialc.raids.effects.RaidEffects;
import org.debentialc.raids.models.*;
import org.debentialc.service.CC;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RaidSessionManager - Gestor de sesiones de raid en progreso
 * Responsable de manejar raids activas
 */
public class RaidSessionManager {

    private static final Map<String, RaidSession> activeSessions = new ConcurrentHashMap<>();
    private static final Map<UUID, String> playerToSession = new ConcurrentHashMap<>();
    private static int sessionCounter = 0;

    /**
     * Crea una nueva sesión de raid para una party
     */
    public static RaidSession createRaidSession(Raid raid, Party party) {
        if (raid == null || party == null) {
            return null;
        }

        String sessionId = "session_" + (++sessionCounter) + "_" + System.currentTimeMillis();
        RaidSession session = new RaidSession(sessionId, raid, party);

        activeSessions.put(sessionId, session);

        // Mapear jugadores a sesión
        for (UUID playerId : party.getActivePlayers()) {
            playerToSession.put(playerId, sessionId);
        }

        System.out.println("[Raids] Sesión de raid creada: " + sessionId + " - Raid: " + raid.getRaidId());
        return session;
    }

    /**
     * Crea una nueva sesión de raid para un único jugador (sin party)
     */
    public static RaidSession createRaidSession(Raid raid, Player player) {
        if (raid == null || player == null) {
            return null;
        }

        String sessionId = "session_" + (++sessionCounter) + "_" + System.currentTimeMillis();
        Set<UUID> players = new HashSet<>(Collections.singletonList(player.getUniqueId()));
        RaidSession session = new RaidSession(sessionId, raid, players);

        activeSessions.put(sessionId, session);
        playerToSession.put(player.getUniqueId(), sessionId);

        System.out.println("[Raids] Sesión de raid en solitario creada: " + sessionId + " - Raid: " + raid.getRaidId());
        return session;
    }

    /**
     * Obtiene la sesión activa de un jugador
     */
    public static RaidSession getPlayerSession(UUID playerId) {
        String sessionId = playerToSession.get(playerId);
        if (sessionId == null) {
            return null;
        }
        return activeSessions.get(sessionId);
    }

    /**
     * Obtiene una sesión por ID
     */
    public static RaidSession getSessionById(String sessionId) {
        return activeSessions.get(sessionId);
    }

    /**
     * Obtiene todas las sesiones activas
     */
    public static Collection<RaidSession> getAllActiveSessions() {
        return new ArrayList<>(activeSessions.values());
    }

    /**
     * Obtiene la sesión de una raid específica
     */
    public static RaidSession getSessionByRaid(Raid raid) {
        for (RaidSession session : activeSessions.values()) {
            if (session.getRaid().getRaidId().equals(raid.getRaidId())) {
                return session;
            }
        }
        return null;
    }

    /**
     * Verifica si hay una sesión activa para una raid
     */
    public static boolean hasActiveSession(String raidId) {
        for (RaidSession session : activeSessions.values()) {
            if (session.getRaid().getRaidId().equals(raidId) &&
                    session.getStatus() == RaidStatus.IN_PROGRESS) {
                return true;
            }
        }
        return false;
    }

    /**
     * Completa una oleada
     */
    public static void completeWave(RaidSession session) {
        if (session == null) {
            return;
        }

        session.getCurrentWave().setStatus(WaveStatus.COMPLETED);
        System.out.println("[Raids] Oleada completada: " + session.getSessionId() + " - Wave " +
                (session.getCurrentWaveIndex() + 1));

        if (session.getCurrentWave().hasRewards()) {
            executeWaveRewards(session);
        }

        if (session.hasNextWave()) {
            session.moveToNextWave();
            session.getCurrentWave().setStatus(WaveStatus.ACTIVE);
            spawnWaveNPCs(session);

            System.out.println("[Raids] Siguiente oleada iniciada: Wave " +
                    (session.getCurrentWaveIndex() + 1));
        } else {
            completeRaid(session);
        }
    }

    /**
     * Ejecuta las recompensas de una oleada a todos los jugadores vivos
     */
    private static void executeWaveRewards(RaidSession session) {
        session.getCurrentWave().getRewards().forEach(reward -> {
            if (reward.shouldExecute()) {
                // Ejecutar comando para cada jugador activo
                session.getActivePlayers().forEach(playerId -> {
                    String command = reward.getCommand()
                            .replace("@a", playerId.toString())
                            .replace("{player}", playerId.toString());

                    System.out.println("[Raids] Ejecutando recompensa: " + command);
                    // TODO: Ejecutar comando via Bukkit
                });
            }
        });
    }

    /**
     * Completa la raid (victoria)
     */
    public static void completeRaid(RaidSession session) {
        if (session == null) {
            return;
        }

        session.setStatus(RaidStatus.COMPLETED);
        session.setEndTime(System.currentTimeMillis());

        System.out.println("[Raids] Raid completada: " + session.getSessionId());
        System.out.println("[Raids] Duración: " + session.getDurationSeconds() + "s");
        System.out.println("[Raids] Jugadores activos: " + session.getActivePlayers().size());

        Set<UUID> allParticipants = new HashSet<>();
        allParticipants.addAll(session.getActivePlayers());
        allParticipants.addAll(session.getDeadPlayers());
        allParticipants.addAll(session.getLeftPlayers());

        for (UUID playerId : allParticipants) {
            CooldownManager.setCooldown(playerId, session.getRaid().getRaidId(),
                    session.getRaid().getCooldownSeconds());
        }

        scheduleSessionRemoval(session.getSessionId());
    }

    /**
     * Falla la raid (derrota - todos mueren)
     */
    public static void failRaid(RaidSession session) {
        if (session == null) {
            return;
        }

        session.setStatus(RaidStatus.FAILED);
        session.setEndTime(System.currentTimeMillis());

        // Despawnear todos los NPCs de la sesión (oleadas completadas y actual)
        for (int i = 0; i <= session.getCurrentWaveIndex(); i++) {
            String waveId = session.getSessionId() + "_wave_" + i;
            NPCSpawnManager.despawnWaveNpcs(waveId);
        }

        System.out.println("[Raids] Raid fallida: " + session.getSessionId());
        removeSession(session.getSessionId());
    }

    /**
     * Maneja la muerte de un jugador en la raid
     */
    public static void playerDied(UUID playerId) {
        RaidSession session = getPlayerSession(playerId);
        if (session == null) {
            return;
        }

        session.playerDied(playerId);
        System.out.println("[Raids] Jugador muerto en raid: " + playerId);

        // Verificar si la raid falló (todos muertos)
        if (session.isRaidFailed()) {
            failRaid(session);
        }
    }

    /**
     * Maneja cuando un jugador se va de la raid
     */
    public static void playerLeft(UUID playerId) {
        RaidSession session = getPlayerSession(playerId);
        if (session == null) {
            return;
        }

        session.playerLeft(playerId);
        playerToSession.remove(playerId);
        System.out.println("[Raids] Jugador salió de la raid: " + playerId);

        if (session.isRaidFailed()) {
            failRaid(session);
        }
    }

    /**
     * Maneja cuando un jugador regresa a la raid
     */
    public static void playerReturned(UUID playerId) {
        RaidSession session = getPlayerSession(playerId);
        if (session == null) {
            return;
        }

        if (session.canPlayerRejoin(playerId)) {
            session.playerReturned(playerId);
            playerToSession.put(playerId, session.getSessionId());
            System.out.println("[Raids] Jugador regresó a la raid: " + playerId);
        }
    }

    /**
     * Obtiene el progreso de una sesión (0-100)
     */
    public static int getSessionProgress(RaidSession session) {
        return session != null ? session.getProgress() : 0;
    }

    /**
     * Obtiene la duración en segundos de una sesión
     */
    public static long getSessionDuration(RaidSession session) {
        return session != null ? session.getDurationSeconds() : 0;
    }

    /**
     * Obtiene información de una sesión
     */
    public static String getSessionInfo(RaidSession session) {
        if (session == null) {
            return "Sesión no encontrada";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("§6=== Información de Sesión ===\n");
        sb.append("§eID: §f").append(session.getSessionId()).append("\n");
        sb.append("§eRaid: §f").append(session.getRaid().getDisplayName()).append("\n");
        sb.append("§eOleada: §f").append(session.getCurrentWaveIndex() + 1).append("/")
                .append(session.getRaid().getTotalWaves()).append("\n");
        sb.append("§eProgreso: §f").append(session.getProgress()).append("%\n");
        sb.append("§eJugadores activos: §f").append(session.getActivePlayers().size()).append("\n");
        sb.append("§eJugadores muertos: §f").append(session.getDeadPlayers().size()).append("\n");
        sb.append("§eEstado: §f").append(session.getStatus().getDisplayName()).append("\n");
        sb.append("§eDuración: §f").append(session.getDurationSeconds()).append("s");

        return sb.toString();
    }

    /**
     * Cuenta el total de sesiones activas
     */
    public static int getTotalActiveSessions() {
        return activeSessions.size();
    }

    /**
     * Obtiene el total de jugadores en raids
     */
    public static int getTotalPlayersInRaids() {
        int total = 0;
        for (RaidSession session : activeSessions.values()) {
            total += session.getActivePlayers().size();
        }
        return total;
    }
    /**
     * Spawns NPCs for the current wave
     */
    public static void spawnWaveNPCs(RaidSession session) {
        if (session == null || session.getCurrentWave() == null) {
            return;
        }

        Wave wave = session.getCurrentWave();
        String waveId = session.getSessionId() + "_wave_" + session.getCurrentWaveIndex();

        System.out.println("[Raids] Spawneando NPCs para oleada " + wave.getWaveNumber());

        boolean spawned = NPCSpawnManager.spawnWaveNpcs(wave, waveId);

        if (!spawned) {
            System.out.println("[Raids] ERROR: No se pudo spawnear ningún NPC para la oleada " + wave.getWaveNumber());
        }
    }
    /**
     * Reinicia una raid para todos los jugadores de la sesión indicada.
     */
    public static boolean restartRaid(RaidSession session, org.bukkit.command.CommandSender initiator) {
        if (session == null) {
            return false;
        }

        Raid raid = session.getRaid();
        if (raid == null) {
            return false;
        }

        Set<UUID> allPlayers = new HashSet<>();
        allPlayers.addAll(session.getActivePlayers());
        allPlayers.addAll(session.getDeadPlayers());
        allPlayers.addAll(session.getLeftPlayers());

        List<Player> onlinePlayers = new ArrayList<>();
        for (UUID playerId : allPlayers) {
            Player p = org.bukkit.Bukkit.getPlayer(playerId);
            if (p != null) {
                onlinePlayers.add(p);
            }
        }

        if (onlinePlayers.isEmpty()) {
            return false;
        }

        // Limpiar NPCs de la sesión y eliminar la sesión actual
        for (int i = 0; i <= session.getCurrentWaveIndex(); i++) {
            String waveId = session.getSessionId() + "_wave_" + i;
            NPCSpawnManager.despawnWaveNpcs(waveId);
        }
        removeSession(session.getSessionId());

        return startRaid(raid, onlinePlayers, initiator, true);
    }

    /**
     * Inicia una raid para una lista de jugadores (usado por /raid start y /party start).
     * Retorna true si se inició correctamente.
     */
    public static boolean startRaid(Raid raid, List<Player> players, org.bukkit.command.CommandSender initiator) {
        return startRaid(raid, players, initiator, false);
    }

    private static boolean startRaid(Raid raid, List<Player> players, org.bukkit.command.CommandSender initiator, boolean ignoreCooldown) {
        if (raid == null || players == null || players.isEmpty()) {
            return false;
        }

        if (raid.getPlayerSpawnPoint() == null) {
            if (initiator != null) {
                initiator.sendMessage("§c✗ La raid no tiene punto de spawn para jugadores");
            }
            return false;
        }

        // Validación centralizada de cooldown (solo si no se pide ignorarlo)
        if (!ignoreCooldown) {
            for (Player player : players) {
                if (player == null || !player.isOnline()) {
                    continue;
                }
                if (CooldownManager.hasCooldown(player.getUniqueId(), raid.getRaidId())) {
                    if (initiator != null) {
                        String timeFormatted = CooldownManager.getCooldownFormattedTime(player.getUniqueId(), raid.getRaidId());
                        initiator.sendMessage(CC.translate("&c✗ &f" + player.getName() + " tiene cooldown activo"));
                        initiator.sendMessage(CC.translate("&7Tiempo restante: &f" + timeFormatted));
                    }
                    return false;
                }
            }
        }

        int onlineCount = 0;
        for (Player player : players) {
            if (player != null && player.isOnline()) {
                onlineCount++;
            }
        }
        if (onlineCount < raid.getMinPlayers() || onlineCount > raid.getMaxPlayers()) {
            if (initiator != null) {
                initiator.sendMessage(CC.translate("&c✗ La raid requiere entre &f" + raid.getMinPlayers() + "-" + raid.getMaxPlayers() + "&c jugadores online"));
            }
            return false;
        }

        RaidSession session;
        if (players.size() == 1) {
            session = createRaidSession(raid, players.get(0));
        } else {
            // Para múltiples jugadores se requiere una party ya creada
            Party party = PartyManager.getPlayerParty(players.get(0).getUniqueId());
            if (party == null) {
                if (initiator != null) {
                    initiator.sendMessage("§c✗ Los jugadores no están en una party");
                }
                return false;
            }
            session = createRaidSession(raid, party);
            PartyManager.setPartyStatus(party, PartyStatus.IN_RAID);
        }

        if (session == null) {
            if (initiator != null) {
                initiator.sendMessage("§c✗ No se pudo crear la sesión de raid");
            }
            return false;
        }

        Location playerSpawn = raid.getPlayerSpawnPoint();
        List<Player> teleportedPlayers = new ArrayList<>();

        for (Player player : players) {
            if (player != null && player.isOnline()) {
                player.teleport(playerSpawn);
                teleportedPlayers.add(player);
            }
        }

        Bukkit.getScheduler().scheduleSyncDelayedTask(
                org.debentialc.Main.instance,
                () -> {
                    Wave firstWave = session.getCurrentWave();
                    if (firstWave != null) {
                        for (SpawnPoint sp : firstWave.getSpawnPoints()) {
                            sp.resetAliveCount();
                        }

                        firstWave.setStatus(WaveStatus.ACTIVE);

                        String waveId = session.getSessionId() + "_wave_0";
                        boolean spawned = NPCSpawnManager.spawnWaveNpcs(firstWave, waveId);

                        if (spawned) {
                            RaidEffects.raidStartEffect(teleportedPlayers, playerSpawn);

                            for (Player member : teleportedPlayers) {
                                RaidTitleManager.showRaidStart(member, raid.getDisplayName());
                                RaidSoundManager.playRaidStartSound(member);
                                member.sendMessage("§bℹ §f¡La raid ha comenzado! Oleada 1/" + raid.getTotalWaves());
                            }
                        } else {
                            for (Player member : teleportedPlayers) {
                                member.sendMessage("§c✗ §fError al spawnear enemigos. Contacta un admin.");
                            }
                            failRaid(session);
                        }
                    }
                },
                40L
        );

        return true;
    }

    /**
     * Programa la eliminación de una sesión (después de 5 minutos)
     */
    private static void scheduleSessionRemoval(String sessionId) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                removeSession(sessionId);
            }
        }, 300000); // 5 minutos
    }

    /**
     * Elimina una sesión
     */
    public static void removeSession(String sessionId) {
        RaidSession session = activeSessions.remove(sessionId);
        if (session != null) {
            for (UUID playerId : session.getActivePlayers()) {
                playerToSession.remove(playerId);
            }
            System.out.println("[Raids] Sesión eliminada: " + sessionId);
        }
    }

    /**
     * Limpia todas las sesiones
     */
    public static void clearAllSessions() {
        activeSessions.clear();
        playerToSession.clear();
        System.out.println("[Raids] Todas las sesiones han sido limpiadas");
    }
}