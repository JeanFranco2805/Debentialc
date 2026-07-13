package org.debentialc.rebirths.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class RebirthBlock {

    private int id;
    private String name;
    private boolean saveLevel;
    private List<Integer> rebirthIds;

    public RebirthBlock(int id) {
        this.id = id;
        this.name = "Bloque " + id;
        this.saveLevel = false;
        this.rebirthIds = new ArrayList<>();
    }

    public RebirthBlock(int id, String name, boolean saveLevel, List<Integer> rebirthIds) {
        this.id = id;
        this.name = name;
        this.saveLevel = saveLevel;
        this.rebirthIds = rebirthIds != null ? rebirthIds : new ArrayList<>();
    }

    public boolean containsRebirth(int rebirthId) {
        return rebirthIds.contains(rebirthId);
    }

    public int getStartRebirthId() {
        return rebirthIds.isEmpty() ? 0 : rebirthIds.get(0);
    }

    public int getEndRebirthId() {
        return rebirthIds.isEmpty() ? 0 : rebirthIds.get(rebirthIds.size() - 1);
    }

    public boolean isCompleted(int playerRebirthLevel) {
        return !rebirthIds.isEmpty() && playerRebirthLevel >= getEndRebirthId();
    }
}
