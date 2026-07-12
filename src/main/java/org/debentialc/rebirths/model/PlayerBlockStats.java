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
}
