package org.debentialc.rebirths.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class RebirthBlock {

    private String id;
    private String name;
    private boolean vip = false;
    private String requiredPermission;
    private List<Integer> rebirthIds;

    public RebirthBlock(String id) {
        this.id = id;
        this.name = "Bloque " + id;
        this.rebirthIds = new ArrayList<>();
    }

    public RebirthBlock(String id, String name, List<Integer> rebirthIds) {
        this.id = id;
        this.name = name;
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
