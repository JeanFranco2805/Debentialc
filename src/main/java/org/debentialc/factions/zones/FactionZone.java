package org.debentialc.factions.zones;

import com.massivecraft.massivecore.ps.PS;

import java.util.HashSet;
import java.util.Set;

public class FactionZone {

    private String id;
    private String displayName;
    private String world;
    private int powerCost;
    private String rarity;
    private int minChunkX;
    private int maxChunkX;
    private int minChunkZ;
    private int maxChunkZ;
    private Set<String> chunks;

    public FactionZone() {
        this.chunks = new HashSet<>();
    }

    public FactionZone(String id, String displayName, String world, int powerCost, String rarity) {
        this.id = id;
        this.displayName = displayName;
        this.world = world;
        this.powerCost = powerCost;
        this.rarity = rarity;
        this.chunks = new HashSet<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getWorld() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public int getPowerCost() {
        return powerCost;
    }

    public void setPowerCost(int powerCost) {
        this.powerCost = powerCost;
    }

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    public int getMinChunkX() {
        return minChunkX;
    }

    public void setMinChunkX(int minChunkX) {
        this.minChunkX = minChunkX;
    }

    public int getMaxChunkX() {
        return maxChunkX;
    }

    public void setMaxChunkX(int maxChunkX) {
        this.maxChunkX = maxChunkX;
    }

    public int getMinChunkZ() {
        return minChunkZ;
    }

    public void setMinChunkZ(int minChunkZ) {
        this.minChunkZ = minChunkZ;
    }

    public int getMaxChunkZ() {
        return maxChunkZ;
    }

    public void setMaxChunkZ(int maxChunkZ) {
        this.maxChunkZ = maxChunkZ;
    }

    public Set<String> getChunks() {
        return chunks;
    }

    public void setChunks(Set<String> chunks) {
        this.chunks = chunks;
    }

    public boolean containsChunk(PS ps) {
        if (ps == null || ps.getChunkX() == null || ps.getChunkZ() == null) return false;
        return chunks.contains(ps.getChunkX() + "," + ps.getChunkZ());
    }

    public int getChunkCount() {
        return chunks.size();
    }
}
