package org.debentialc.rebirths.model;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Rebirth {

    private int id;
    private int blockId;
    private String displayName;
    private int requiredLevel;
    private double tpBonusPercent;
    private List<String> allowedRegions;
    private List<ItemStack> rewardItems;
    private List<String> rewardCommands;

    public Rebirth(int id) {
        this.id = id;
        this.blockId = 0;
        this.displayName = "Rebirth " + id;
        this.requiredLevel = 0;
        this.tpBonusPercent = 0.0;
        this.allowedRegions = new ArrayList<>();
        this.rewardItems = new ArrayList<>();
        this.rewardCommands = new ArrayList<>();
    }

    public Rebirth(int id, int blockId, String displayName, int requiredLevel, double tpBonusPercent, List<String> allowedRegions, List<ItemStack> rewardItems, List<String> rewardCommands) {
        this.id = id;
        this.blockId = blockId;
        this.displayName = displayName;
        this.requiredLevel = requiredLevel;
        this.tpBonusPercent = tpBonusPercent;
        this.allowedRegions = allowedRegions != null ? allowedRegions : new ArrayList<>();
        this.rewardItems = rewardItems != null ? rewardItems : new ArrayList<>();
        this.rewardCommands = rewardCommands != null ? rewardCommands : new ArrayList<>();
    }

    public double getMultiplier() {
        return 1.0 + (tpBonusPercent / 100.0);
    }
}
