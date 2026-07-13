package org.debentialc.rebirths.model;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class PlayerBlockStats {

    private int startRebirthId;
    private Map<String, Integer> stats;

    public PlayerBlockStats() {
        this.startRebirthId = 0;
        this.stats = new HashMap<>();
    }

    public PlayerBlockStats(int startRebirthId, Map<String, Integer> stats) {
        this.startRebirthId = startRebirthId;
        this.stats = stats != null ? stats : new HashMap<>();
    }

    /**
     * Calcula el nivel equivalente a partir de las stats guardadas.
     * Usa la misma fórmula que General.getLVL.
     */
    public int getLevel() {
        if (stats == null || stats.isEmpty()) {
            return 0;
        }

        int str = stats.getOrDefault("STR", 0);
        int dex = stats.getOrDefault("DEX", 0);
        int con = stats.getOrDefault("CON", 0);
        int wil = stats.getOrDefault("WIL", 0);
        int mnd = stats.getOrDefault("MND", 0);
        int spi = stats.getOrDefault("SPI", 0);

        return (str + dex + con + wil + mnd + spi) / 5 - 11;
    }
}
