package org.debentialc.trainings.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class TrainingLevel {

    private int level;
    private String description;
    private double tpBonusPercent;
    private List<String> rewardCommands;
    private Map<String, Integer> statCosts;
    private int tpCost;

    public TrainingLevel(int level) {
        this.level = level;
        this.description = null;
        this.tpBonusPercent = 0.0;
        this.rewardCommands = new ArrayList<>();
        this.statCosts = new HashMap<>();
        this.tpCost = 0;
    }

    public TrainingLevel(int level, String description, double tpBonusPercent, List<String> rewardCommands,
                         Map<String, Integer> statCosts, int tpCost) {
        this.level = level;
        this.description = description;
        this.tpBonusPercent = tpBonusPercent;
        this.rewardCommands = rewardCommands != null ? rewardCommands : new ArrayList<>();
        this.statCosts = statCosts != null ? statCosts : new HashMap<>();
        this.tpCost = tpCost;
    }

    public int getStatCost(String stat) {
        if (statCosts == null) return 0;
        Integer value = statCosts.get(stat.toUpperCase());
        return value != null ? value : 0;
    }
}
