package org.debentialc.raids.models;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import java.util.*;

/**
 * Sesión de Raid - Representa una raid en progreso
 */
@Getter
@Setter
public class RaidSession {

    private String sessionId;
    private Raid raid;
    private Party party;

    private int currentWaveIndex;
    private RaidStatus status;
    private long startTime;
    private long endTime;

    private Set<UUID> activePlayers;
    private Set<UUID> deadPlayers;
    private Set<UUID> leftPlayers;

    private String winner;

    public RaidSession(String sessionId, Raid raid, Party party) {
        this(sessionId, raid, party.getMembers().keySet());
        this.party = party;
    }

    public RaidSession(String sessionId, Raid raid, Set<UUID> players) {
        this.sessionId = sessionId;
        this.raid = raid;
        this.party = null;
        this.currentWaveIndex = 0;
        this.status = RaidStatus.IN_PROGRESS;
        this.startTime = System.currentTimeMillis();
        this.activePlayers = new HashSet<>(players);
        this.deadPlayers = new HashSet<>();
        this.leftPlayers = new HashSet<>();
    }

    public Wave getCurrentWave() {
        return raid.getWaveByIndex(currentWaveIndex);
    }

    public boolean hasNextWave() {
        return currentWaveIndex + 1 < raid.getTotalWaves();
    }

    public void moveToNextWave() {
        currentWaveIndex++;
    }

    public void playerDied(UUID playerId) {
        activePlayers.remove(playerId);
        deadPlayers.add(playerId);
    }

    public void playerLeft(UUID playerId) {
        activePlayers.remove(playerId);
        leftPlayers.add(playerId);
    }

    public void playerReturned(UUID playerId) {
        if (leftPlayers.contains(playerId)) {
            leftPlayers.remove(playerId);
            activePlayers.add(playerId);
        }
    }

    public boolean canPlayerRejoin(UUID playerId) {
        return !deadPlayers.contains(playerId) && status == RaidStatus.IN_PROGRESS;
    }

    public boolean isRaidFailed() {
        return activePlayers.isEmpty();
    }

    public int getProgress() {
        return (currentWaveIndex * 100) / Math.max(1, raid.getTotalWaves());
    }

    public long getDurationSeconds() {
        long end = endTime > 0 ? endTime : System.currentTimeMillis();
        return (end - startTime) / 1000;
    }

    public boolean isWaveActive() {
        return status == RaidStatus.IN_PROGRESS && getCurrentWave() != null;
    }

    @Override
    public String toString() {
        return String.format("RaidSession{id='%s', raid='%s', wave=%d/%d, players=%d, status=%s}",
                sessionId, raid.getRaidId(), currentWaveIndex + 1, raid.getTotalWaves(),
                activePlayers.size(), status);
    }
}