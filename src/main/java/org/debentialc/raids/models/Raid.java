package org.debentialc.raids.models;

import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import java.util.*;

/**
 * Modelo de Raid - Contiene toda la información de una raid
 * CORREGIDO: Usa @Expose para evitar recursión infinita en GSON
 */
@Getter
@Setter
public class Raid {

    @Expose
    private String raidId;

    @Expose
    private String raidName;

    @Expose
    private String description;

    @Expose
    private Location arenaSpawnPoint;

    @Expose
    private Location playerSpawnPoint;

    @Expose
    private List<Wave> waves;

    @Expose
    private RaidStatus status;

    @Expose
    private long cooldownSeconds;

    @Expose
    private String menuItem;

    @Expose
    private String category;

    @Expose
    private int categoryOrder;

    @Expose
    private int minPlayers;

    @Expose
    private int maxPlayers;

    @Expose
    private boolean enabled;

    @Expose
    private long createdAt;

    @Expose
    private String createdBy;

    public Raid(String raidId, String raidName) {
        this.raidId = raidId;
        this.raidName = raidName;
        this.waves = new ArrayList<>();
        this.status = RaidStatus.IDLE;
        this.minPlayers = 1; // CORREGIDO: Permitir raids en solitario
        this.maxPlayers = 5;
        this.cooldownSeconds = 3600;
        this.enabled = true;
        this.createdAt = System.currentTimeMillis();
    }

    public Wave getWaveByIndex(int index) {
        if (index >= 0 && index < waves.size()) {
            return waves.get(index);
        }
        return null;
    }

    public void addWave(Wave wave) {
        waves.add(wave);
    }

    public int getTotalWaves() {
        return waves.size();
    }

    public String getDisplayName() {
        return (raidName != null && !raidName.isEmpty()) ? raidName : raidId;
    }

    public boolean isConfigured() {
        return
                playerSpawnPoint != null &&
                !waves.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("Raid{id='%s', name='%s', waves=%d, status=%s}",
                raidId, raidName, waves.size(), status);
    }
}