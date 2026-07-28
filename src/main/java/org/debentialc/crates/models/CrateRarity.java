package org.debentialc.crates.models;

public class CrateRarity {

    private String id;
    private String displayName;
    private String color;
    private double weight;
    private boolean announce;

    public CrateRarity(String id, String displayName, String color, double weight, boolean announce) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.weight = weight;
        this.announce = announce;
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

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public boolean isAnnounce() {
        return announce;
    }

    public void setAnnounce(boolean announce) {
        this.announce = announce;
    }
}
