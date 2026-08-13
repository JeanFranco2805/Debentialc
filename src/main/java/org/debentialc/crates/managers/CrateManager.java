package org.debentialc.crates.managers;

import com.gmail.filoghost.holograms.api.Hologram;
import com.gmail.filoghost.holograms.api.HolographicDisplaysAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import org.debentialc.Main;
import org.debentialc.crates.models.Crate;
import org.debentialc.crates.models.CrateItem;
import org.debentialc.crates.models.CrateRarity;
import org.debentialc.crates.storage.CrateStorage;
import org.debentialc.crates.utils.CrateItemSerializer;
import org.debentialc.customitems.tools.nbt.NbtHandler;
import org.debentialc.service.CC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CrateManager {

    private static CrateManager instance;
    private final CrateStorage storage;
    private final Map<String, CrateRarity> rarities = new HashMap<>();
    private final Map<String, Crate> crates = new HashMap<>();
    private final Map<String, Hologram> holograms = new HashMap<>();

    private CrateManager() {
        this.storage = CrateStorage.getInstance();
        load();
    }

    public static synchronized CrateManager getInstance() {
        if (instance == null) {
            instance = new CrateManager();
        }
        return instance;
    }

    public void load() {
        rarities.clear();
        crates.clear();
        clearHolograms();
        rarities.putAll(storage.loadRarities());
        crates.putAll(storage.loadCrate(rarities));
        for (Crate crate : crates.values()) {
            spawnCrateBlock(crate);
            spawnHologram(crate);
        }
    }

    public void reload() {
        storage.load();
        load();
    }

    private void clearHolograms() {
        for (Hologram hologram : holograms.values()) {
            if (hologram != null) {
                hologram.delete();
            }
        }
        holograms.clear();
    }

    public boolean createRarity(String id, String displayName, String color, double weight, boolean announce) {
        id = id.toUpperCase();
        if (rarities.containsKey(id)) {
            return false;
        }
        CrateRarity rarity = new CrateRarity(id, displayName, color, weight, announce);
        rarities.put(id, rarity);
        storage.saveRarities(rarities);
        return true;
    }

    public boolean updateRarity(String id, String displayName, Double weight, String color, Boolean announce) {
        id = id.toUpperCase();
        CrateRarity rarity = rarities.get(id);
        if (rarity == null) {
            return false;
        }
        if (displayName != null) {
            rarity.setDisplayName(displayName);
        }
        if (weight != null) {
            rarity.setWeight(weight);
        }
        if (color != null) {
            rarity.setColor(color);
        }
        if (announce != null) {
            rarity.setAnnounce(announce);
        }
        storage.saveRarities(rarities);
        return true;
    }

    public boolean setRarityWeight(String id, double weight) {
        return updateRarity(id, null, weight, null, null);
    }

    public boolean deleteRarity(String id) {
        id = id.toUpperCase();
        if (!rarities.containsKey(id)) {
            return false;
        }
        for (Crate crate : crates.values()) {
            for (CrateItem item : crate.getItems().values()) {
                if (item.getRarityId().equalsIgnoreCase(id)) {
                    return false;
                }
            }
        }
        rarities.remove(id);
        storage.deleteRarity(id);
        return true;
    }

    public CrateRarity getRarity(String id) {
        return rarities.get(id.toUpperCase());
    }

    public Map<String, CrateRarity> getAllRarities() {
        return new HashMap<>(rarities);
    }

    public boolean createCrate(String id, String displayName, Location location) {
        id = id.toLowerCase();
        if (crates.containsKey(id)) {
            return false;
        }
        Crate crate = new Crate(id, displayName);
        if (location != null) {
            crate.setWorld(location.getWorld().getName());
            crate.setX(location.getBlockX());
            crate.setY(location.getBlockY());
            crate.setZ(location.getBlockZ());
        }
        crates.put(id, crate);
        storage.saveCrate(crate);
        spawnCrateBlock(crate);
        spawnHologram(crate);
        return true;
    }

    public boolean deleteCrate(String id) {
        id = id.toLowerCase();
        Crate crate = crates.remove(id);
        if (crate == null) {
            return false;
        }
        removeCrateBlock(crate);
        removeHologram(crate);
        storage.deleteCrate(id);
        return true;
    }

    public Crate getCrate(String id) {
        return crates.get(id.toLowerCase());
    }

    public Map<String, Crate> getAllCrates() {
        return new HashMap<>(crates);
    }

    public Crate getCrateAtLocation(Location location) {
        if (location == null) return null;
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        String world = location.getWorld().getName();
        for (Crate crate : crates.values()) {
            if (crate.hasLocation()
                    && crate.getWorld().equalsIgnoreCase(world)
                    && crate.getX() == x
                    && crate.getY() == y
                    && crate.getZ() == z) {
                return crate;
            }
        }
        return null;
    }

    public void setCrateLocation(Crate crate, Location location) {
        removeCrateBlock(crate);
        removeHologram(crate);
        crate.setWorld(location.getWorld().getName());
        crate.setX(location.getBlockX());
        crate.setY(location.getBlockY());
        crate.setZ(location.getBlockZ());
        storage.saveCrate(crate);
        spawnCrateBlock(crate);
        spawnHologram(crate);
    }

    public void spawnCrateBlock(Crate crate) {
        if (!crate.hasLocation()) return;
        World world = Bukkit.getWorld(crate.getWorld());
        if (world == null) return;
        Block block = world.getBlockAt(crate.getX(), crate.getY(), crate.getZ());
        block.setType(Material.CHEST);
    }

    public void removeCrateBlock(Crate crate) {
        if (!crate.hasLocation()) return;
        World world = Bukkit.getWorld(crate.getWorld());
        if (world == null) return;
        Block block = world.getBlockAt(crate.getX(), crate.getY(), crate.getZ());
        if (block.getType() == Material.CHEST) {
            block.setType(Material.AIR);
        }
    }

    public void spawnHologram(Crate crate) {
        if (!crate.hasLocation()) return;
        World world = Bukkit.getWorld(crate.getWorld());
        if (world == null) return;
        removeHologram(crate);
        Location loc = new Location(world, crate.getX() + 0.5, crate.getY() + 1.5, crate.getZ() + 0.5);
        Hologram hologram = HolographicDisplaysAPI.createHologram(Main.instance, loc, CC.translate(crate.getDisplayName()));
        holograms.put(crate.getId(), hologram);
    }

    public void removeHologram(Crate crate) {
        Hologram hologram = holograms.remove(crate.getId());
        if (hologram != null) {
            hologram.delete();
        }
    }

    public boolean addItemToCrate(String crateId, String itemId, String rarityId, ItemStack item) {
        crateId = crateId.toLowerCase();
        Crate crate = crates.get(crateId);
        if (crate == null) {
            return false;
        }
        CrateRarity rarity = rarities.get(rarityId.toUpperCase());
        if (rarity == null) {
            return false;
        }
        String base64 = CrateItemSerializer.itemToBase64(item);
        if (base64 == null) {
            return false;
        }
        CrateItem crateItem = new CrateItem(itemId.toLowerCase(), rarityId.toUpperCase(), base64, 0.0);
        crate.getItems().put(crateItem.getId(), crateItem);
        storage.saveCrate(crate);
        return true;
    }

    public boolean addItemToCrate(String crateId, String rarityId, ItemStack item) {
        String itemId = "item_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 1000);
        return addItemToCrate(crateId, itemId, rarityId, item);
    }

    public boolean removeItemFromCrate(String crateId, String itemId) {
        crateId = crateId.toLowerCase();
        Crate crate = crates.get(crateId);
        if (crate == null) {
            return false;
        }
        if (crate.getItems().remove(itemId.toLowerCase()) == null) {
            return false;
        }
        storage.deleteCrateItem(crateId, itemId.toLowerCase());
        return true;
    }

    public void clearItems(String crateId) {
        crateId = crateId.toLowerCase();
        Crate crate = crates.get(crateId);
        if (crate == null) return;
        crate.getItems().clear();
        storage.saveCrate(crate);
    }

    public void saveCrate(Crate crate) {
        if (crate == null) return;
        crates.put(crate.getId().toLowerCase(), crate);
        storage.saveCrate(crate);
    }

    public ItemStack createKey(String crateId) {
        Crate crate = crates.get(crateId.toLowerCase());
        if (crate == null) {
            return null;
        }
        ItemStack key = new ItemStack(4426, 1);
        org.bukkit.inventory.meta.ItemMeta meta = key.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(CC.translate("&b&lLlave Misteriosa &8C#" + crate.getId()));
            List<String> lore = new ArrayList<>();
            lore.add(CC.translate("&8"));
            lore.add(CC.translate("&7Una llave misteriosa que"));
            lore.add(CC.translate("&7abre la crate:"));
            lore.add(CC.translate("&8"));
            lore.add(CC.translate(crate.getDisplayName()));
            lore.add(CC.translate("&8"));
            lore.add(CC.translate("&7Haz click derecho para"));
            lore.add(CC.translate("&7obtener un item exclusivo."));
            lore.add(CC.translate("&8"));
            lore.add(CC.translate("&e&lLLAVE"));
            meta.setLore(lore);
            key.setItemMeta(meta);
        }
        NbtHandler nbt = new NbtHandler(key);
        nbt.setString("crate_key", crate.getId());
        return nbt.getItemStack();
    }

    public String getKeyCrateId(ItemStack item) {
        if (item == null || item.getTypeId() == 0) {
            return null;
        }
        NbtHandler nbt = new NbtHandler(item);
        if (!nbt.hasNBT() || !nbt.hasKey("crate_key")) {
            return null;
        }
        String crateId = nbt.getString("crate_key");
        if (crateId == null || crateId.isEmpty()) {
            return null;
        }
        return crateId;
    }

    public CrateItem openCrate(String crateId) {
        Crate crate = crates.get(crateId.toLowerCase());
        return rollItem(crate);
    }

    public CrateItem rollItem(Crate crate) {
        if (crate == null || crate.getItems().isEmpty()) {
            return null;
        }
        List<CrateItem> items = new ArrayList<>(crate.getItems().values());

        // Si hay items con probabilidad personalizada, se usan esas.
        double totalChance = 0;
        for (CrateItem item : items) {
            if (item.getChance() > 0) totalChance += item.getChance();
        }
        if (totalChance > 0) {
            double random = Math.random() * totalChance;
            double current = 0;
            for (CrateItem item : items) {
                if (item.getChance() <= 0) continue;
                current += item.getChance();
                if (random <= current) return item;
            }
            return items.get(items.size() - 1);
        }

        // Fallback: usar pesos de rareza
        double totalWeight = 0;
        for (CrateItem item : items) {
            CrateRarity rarity = rarities.get(item.getRarityId());
            if (rarity != null) totalWeight += rarity.getWeight();
        }
        if (totalWeight <= 0) {
            return items.get(0);
        }
        double random = Math.random() * totalWeight;
        double current = 0;
        for (CrateItem item : items) {
            CrateRarity rarity = rarities.get(item.getRarityId());
            if (rarity == null) continue;
            current += rarity.getWeight();
            if (random <= current) return item;
        }
        return items.get(items.size() - 1);
    }

    public double getTotalChance(Crate crate) {
        double total = 0;
        for (CrateItem item : crate.getItems().values()) {
            if (item.getChance() > 0) total += item.getChance();
        }
        return total;
    }

    public double getItemChancePercent(Crate crate, CrateItem item) {
        double total = getTotalChance(crate);
        if (total <= 0) {
            // Fallback: rareza
            double totalWeight = 0;
            for (CrateItem i : crate.getItems().values()) {
                CrateRarity rarity = rarities.get(i.getRarityId());
                if (rarity != null) totalWeight += rarity.getWeight();
            }
            CrateRarity rarity = rarities.get(item.getRarityId());
            if (totalWeight <= 0 || rarity == null) return 0;
            return Math.round((rarity.getWeight() / totalWeight) * 1000.0) / 10.0;
        }
        if (item.getChance() <= 0) return 0;
        return Math.round((item.getChance() / total) * 1000.0) / 10.0;
    }

    public void giveCrateReward(Player player, Crate crate, CrateItem won) {
        if (won == null) return;
        ItemStack reward = CrateItemSerializer.itemFromBase64(won.getItemBase64());
        if (reward == null) {
            player.sendMessage(CC.translate("&c✗ Error al crear el item ganado."));
            return;
        }
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(reward);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
        player.sendMessage(CC.translate("&a✓ ¡Has abierto la crate &f" + crate.getDisplayName() + "&a!"));
        CrateRarity rarity = getRarity(won.getRarityId());
        if (rarity != null && rarity.isAnnounce()) {
            String itemName = reward.hasItemMeta() && reward.getItemMeta().hasDisplayName()
                    ? reward.getItemMeta().getDisplayName()
                    : reward.getType().name();
            Bukkit.broadcastMessage(CC.translate(rarity.getColor() + "¡" + player.getName() + " ha obtenido " + itemName + " de " + crate.getDisplayName() + "!"));
        }
    }

    public void giveKey(Player player, String crateId, int amount) {
        ItemStack key = createKey(crateId);
        if (key == null) {
            return;
        }
        key.setAmount(amount);
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(key);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
    }
}
