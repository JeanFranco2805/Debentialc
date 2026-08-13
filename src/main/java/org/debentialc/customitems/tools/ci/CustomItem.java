package org.debentialc.customitems.tools.ci;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
public class CustomItem {
    private int count = 1;
    private int material;
    private short durabilityData = 0;
    private String displayName;
    private List<String> lore;
    private double value;
    private HashMap<String, String> operation = new HashMap<>();
    private HashMap<String, Double> valueByStat = new HashMap<>();
    private HashMap<String, Double> effects = new HashMap<>();

    private String id;
    private boolean isActive = true;

    private int maxDurability = -1;

    private boolean unbreakable = false;

    private boolean consumable = false;
    private int delaySeconds = 0;
    private int expirationSeconds = 0;

    private List<String> commands = new ArrayList<>();

    private int tpValue = 0;

    private boolean tpConsumeStack = false;

    private int attackDamage = -1;

    private String nbtData = null;

    private String category = null;

    // Rebirth/permission gating
    private String requiredRebirthBlock = null;
    private int requiredRebirthLevel = 0;
    private String requiredPermission = null;

    private boolean ownerOnly = false;

    public CustomItem setCategory(String category) {
        this.category = category;
        return this;
    }

    public CustomItem() {
        this.commands = new ArrayList<>();
    }

    public CustomItem setMaterial(int material) {
        this.material = material;
        return this;
    }

    public CustomItem setMaterial(int material, short data) {
        this.material = material;
        this.durabilityData = data;
        return this;
    }

    public CustomItem setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public CustomItem setLore(List<String> lore) {
        this.lore = lore;
        return this;
    }

    public CustomItem setOperation(String operation, String stat) {
        this.operation.put(stat, operation);
        return this;
    }

    public CustomItem setOperation(HashMap<String, String> operations) {
        this.operation = operations;
        return this;
    }

    public CustomItem setBonusStat(String stat, double value) {
        this.valueByStat.put(stat, value);
        return this;
    }

    public CustomItem setValueByStat(HashMap<String, Double> valueByStat) {
        this.valueByStat = valueByStat;
        return this;
    }

    public CustomItem setId(String id) {
        this.id = id;
        return this;
    }

    public CustomItem setEffects(HashMap<String, Double> effects) {
        this.effects = effects;
        return this;
    }

    public CustomItem setMaxDurability(int maxDurability) {
        this.maxDurability = maxDurability;
        return this;
    }

    public CustomItem setUnbreakable(boolean unbreakable) {
        this.unbreakable = unbreakable;
        return this;
    }

    public CustomItem setConsumable(boolean consumable) {
        this.consumable = consumable;
        return this;
    }

    public int getDelaySeconds() {
        return delaySeconds;
    }

    public CustomItem setDelaySeconds(int delaySeconds) {
        this.delaySeconds = delaySeconds;
        return this;
    }

    /**
     * @deprecated use getDelaySeconds()
     */
    public int getCooldownSeconds() {
        return delaySeconds;
    }

    /**
     * @deprecated use setDelaySeconds(int)
     */
    public CustomItem setCooldownSeconds(int cooldownSeconds) {
        this.delaySeconds = cooldownSeconds;
        return this;
    }

    public int getExpirationSeconds() {
        return expirationSeconds;
    }

    public CustomItem setExpirationSeconds(int expirationSeconds) {
        this.expirationSeconds = expirationSeconds;
        return this;
    }

    public boolean isOwnerOnly() {
        return ownerOnly;
    }

    public CustomItem setOwnerOnly(boolean ownerOnly) {
        this.ownerOnly = ownerOnly;
        return this;
    }

    public CustomItem setCommands(List<String> commands) {
        this.commands = commands;
        return this;
    }

    public CustomItem setTpValue(int tpValue) {
        this.tpValue = tpValue;
        return this;
    }

    public CustomItem setTpConsumeStack(boolean tpConsumeStack) {
        this.tpConsumeStack = tpConsumeStack;
        return this;
    }

    public CustomItem setAttackDamage(int attackDamage) {
        this.attackDamage = attackDamage;
        return this;
    }

    public CustomItem setNbtData(String nbtData) {
        this.nbtData = nbtData;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        CustomItem item = (CustomItem) o;
        return material == item.material &&
                Objects.equals(displayName, item.displayName) &&
                Objects.equals(lore, item.lore);
    }

    @Override
    public int hashCode() {
        return Objects.hash(material, displayName, lore);
    }
}