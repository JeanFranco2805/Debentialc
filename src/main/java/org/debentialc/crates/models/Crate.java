package org.debentialc.crates.models;

import java.util.HashMap;
import java.util.Map;

public class Crate {

    private String id;
    private String displayName;
    private String world;
    private int x;
    private int y;
    private int z;
    private final Map<String, CrateRarity> rarities = new HashMap<>();
    private final Map<String, CrateItem> items = new HashMap<>();

    public Crate(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
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

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getZ() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public boolean hasLocation() {
        return world != null && !world.isEmpty();
    }

    public Map<String, CrateRarity> getRarities() {
        return rarities;
    }

    public Map<String, CrateItem> getItems() {
        return items;
    }

    public CrateRarity getRarity(String id) {
        return rarities.get(id.toUpperCase());
    }

    public CrateItem getItem(String id) {
        return items.get(id.toLowerCase());
    }
}
