package org.debentialc.crates.models;

public class CrateItem {

    private String id;
    private String rarityId;
    private String itemBase64;

    public CrateItem(String id, String rarityId, String itemBase64) {
        this.id = id;
        this.rarityId = rarityId;
        this.itemBase64 = itemBase64;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRarityId() {
        return rarityId;
    }

    public void setRarityId(String rarityId) {
        this.rarityId = rarityId;
    }

    public String getItemBase64() {
        return itemBase64;
    }

    public void setItemBase64(String itemBase64) {
        this.itemBase64 = itemBase64;
    }
}
