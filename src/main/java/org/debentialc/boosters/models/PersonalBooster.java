package org.debentialc.boosters.models;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class PersonalBooster implements Serializable {
    private static final long serialVersionUID = 2L;

    private UUID playerId;
    private double multiplier;
    private Instant activationTime;
    private boolean active;
    private long durationSeconds;

    public PersonalBooster() {
        this.active = false;
        this.durationSeconds = 0;
    }

    public PersonalBooster(UUID playerId, double multiplier, long durationSeconds) {
        this.playerId = playerId;
        this.multiplier = multiplier;
        this.durationSeconds = durationSeconds;
        this.activationTime = Instant.now();
        this.active = true;
    }

    public boolean isStillActive() {
        if (!active || activationTime == null) return false;
        if (durationSeconds <= 0) return true;
        Duration elapsed = Duration.between(activationTime, Instant.now());
        return elapsed.getSeconds() < durationSeconds;
    }

    public long getActivationTimeRemaining() {
        if (!active || activationTime == null) return 0;
        if (durationSeconds <= 0) return -1;
        Duration elapsed = Duration.between(activationTime, Instant.now());
        long remaining = durationSeconds - elapsed.getSeconds();
        return Math.max(0, remaining);
    }

    public int getPercentageBonus() {
        return (int) ((multiplier - 1.0) * 100);
    }

    public String getLevelName() {
        return "Personal";
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public void setPlayerId(UUID playerId) {
        this.playerId = playerId;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    public Instant getActivationTime() {
        return activationTime;
    }

    public void setActivationTime(Instant activationTime) {
        this.activationTime = activationTime;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }
}
