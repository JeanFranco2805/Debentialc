package org.debentialc.trainings.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class Training {

    private String id;
    private String name;
    private int materialId;
    private boolean vip;
    private String requiredPermission;
    private List<TrainingLevel> levels;
    private Map<String, Integer> npcTpBase;

    public Training(String id) {
        this.id = id;
        this.name = "Training " + id;
        this.materialId = 339;
        this.vip = false;
        this.requiredPermission = null;
        this.levels = new ArrayList<>();
        this.npcTpBase = new HashMap<>();
    }

    public Training(String id, String name, int materialId, boolean vip, String requiredPermission, List<TrainingLevel> levels, Map<String, Integer> npcTpBase) {
        this.id = id;
        this.name = name;
        this.materialId = materialId;
        this.vip = vip;
        this.requiredPermission = requiredPermission;
        this.levels = levels != null ? levels : new ArrayList<>();
        this.npcTpBase = npcTpBase != null ? npcTpBase : new HashMap<>();
    }

    public TrainingLevel getLevel(int levelNumber) {
        for (TrainingLevel level : levels) {
            if (level.getLevel() == levelNumber) {
                return level;
            }
        }
        return null;
    }

    public int getMaxLevel() {
        return levels.isEmpty() ? 0 : levels.get(levels.size() - 1).getLevel();
    }

    public double getTotalBonusPercent() {
        if (levels.isEmpty()) return 0.0;
        TrainingLevel lastLevel = levels.get(levels.size() - 1);
        return lastLevel.getTpBonusPercent();
    }

    public void addNpc(String npcId, int baseTp) {
        if (npcTpBase == null) npcTpBase = new HashMap<>();
        npcTpBase.put(npcId, baseTp);
    }

    public void removeNpc(String npcId) {
        if (npcTpBase != null) npcTpBase.remove(npcId);
    }

    public Integer getNpcBaseTp(String npcId) {
        return npcTpBase != null ? npcTpBase.get(npcId) : null;
    }
}
