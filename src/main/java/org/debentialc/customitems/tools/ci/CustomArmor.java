package org.debentialc.customitems.tools.ci;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
public class CustomArmor {
    private int count = 1;
    private int material;
    private String displayName;
    private List<String> lore;
    private double value;
    private HashMap<String, String> operation = new HashMap<>();
    private HashMap<String, Double> valueByStat = new HashMap<>();
    private HashMap<String, Double> effects = new HashMap<>();

    private String id;
    private boolean isArmor = true;

    // Durabilidad máxima personalizada (-1 = sin durabilidad custom)
    private int maxDurability = -1;

    // Item irrompible (no recibe daño)
    private boolean unbreakable = false;

    // Rebirth/permission gating
    private String requiredRebirthBlock = null;
    private int requiredRebirthLevel = 0;
    private String requiredPermission = null;

    private boolean ownerOnly = false;

    // Tiempo de expiración en segundos (0 = no expira)
    private int expirationSeconds = 0;

    public CustomArmor setMaterial(int material) {
        this.material = material;
        return this;
    }

    public CustomArmor setDisplayName(String displayName){
        this.displayName = displayName;
        return this;
    }

    public CustomArmor setLore(List<String> lore){
        this.lore = lore;
        return this;
    }

    public CustomArmor setOperation(String operation, String stat){
        this.operation.put(stat, operation);
        return this;
    }

    public CustomArmor setBonusStat(String stat, double value){
        this.valueByStat.put(stat, value);
        return this;
    }

    public CustomArmor setId(String id){
        this.id = id;
        return this;
    }

    public CustomArmor setMaxDurability(int maxDurability) {
        this.maxDurability = maxDurability;
        return this;
    }

    public CustomArmor setUnbreakable(boolean unbreakable) {
        this.unbreakable = unbreakable;
        return this;
    }

    public boolean isOwnerOnly() {
        return ownerOnly;
    }

    public CustomArmor setOwnerOnly(boolean ownerOnly) {
        this.ownerOnly = ownerOnly;
        return this;
    }

    public CustomArmor setExpirationSeconds(int expirationSeconds) {
        this.expirationSeconds = expirationSeconds;
        return this;
    }

    public boolean matchesItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.getItemMeta() == null) return false;

        String displayName = itemStack.getItemMeta().getDisplayName();
        int materialId = itemStack.getTypeId();

        return this.displayName != null &&
                this.displayName.equals(displayName) &&
                this.material == materialId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        CustomArmor item = (CustomArmor) o;
        return material == item.material &&
                Objects.equals(displayName, item.displayName) &&
                Objects.equals(lore, item.lore);
    }

    @Override
    public int hashCode() {
        return Objects.hash(material, displayName, lore);
    }
}