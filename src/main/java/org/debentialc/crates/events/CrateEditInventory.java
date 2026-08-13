package org.debentialc.crates.events;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.debentialc.crates.managers.CrateManager;
import org.debentialc.crates.models.Crate;
import org.debentialc.crates.models.CrateItem;
import org.debentialc.crates.models.CrateRarity;
import org.debentialc.crates.utils.CrateItemSerializer;
import org.debentialc.service.CC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CrateEditInventory implements Listener {

    // 28 central slots for items (4 rows x 7 columns)
    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    private static final Map<UUID, String> selectedRarity = new HashMap<>();
    private static final Map<UUID, Map<Integer, String>> originalRarities = new HashMap<>();
    private static final Map<UUID, Map<Integer, String>> originalBase64 = new HashMap<>();
    private static final Map<UUID, Map<Integer, Double>> originalChances = new HashMap<>();
    private static final Map<UUID, Map<Integer, String>> displayBase64 = new HashMap<>();
    private static final Map<UUID, String> openTitles = new HashMap<>();
    private static final Map<String, Crate> crateByTitle = new HashMap<>();
    private static final Map<String, Integer> titleRefCount = new HashMap<>();
    private static final Set<String> viewOnlyTitles = new HashSet<>();

    public static void open(Player player, Crate crate) {
        String title = CC.translate("&cEditar: &f" + crate.getId());
        if (title.length() > 32) {
            title = title.substring(0, 32);
        }
        Inventory inv = Bukkit.createInventory(null, 54, title);

        fillBorders(inv);
        updateRarityButton(inv, player);
        inv.setItem(51, createProbabilityButton());
        inv.setItem(53, createCloseButton());

        Map<Integer, String> rarities = new HashMap<>();
        Map<Integer, String> base64s = new HashMap<>();
        Map<Integer, Double> chances = new HashMap<>();
        Map<Integer, String> displays = new HashMap<>();
        int index = 0;
        String dominantRarity = null;
        for (CrateItem crateItem : crate.getItems().values()) {
            if (index >= ITEM_SLOTS.length) break;
            int slot = ITEM_SLOTS[index];
            ItemStack item = CrateItemSerializer.itemFromBase64(crateItem.getItemBase64());
            if (item == null) {
                index++;
                continue;
            }
            item = addRarityLore(item, crate, crateItem);
            inv.setItem(slot, item);
            rarities.put(slot, crateItem.getRarityId());
            base64s.put(slot, crateItem.getItemBase64());
            chances.put(slot, crateItem.getChance());
            String disp = CrateItemSerializer.itemToBase64(item);
            if (disp != null) {
                displays.put(slot, disp);
            }
            dominantRarity = crateItem.getRarityId();
            index++;
        }

        originalRarities.put(player.getUniqueId(), rarities);
        originalBase64.put(player.getUniqueId(), base64s);
        originalChances.put(player.getUniqueId(), chances);
        displayBase64.put(player.getUniqueId(), displays);
        selectedRarity.put(player.getUniqueId(), dominantRarity != null ? dominantRarity : selectedRarity.getOrDefault(player.getUniqueId(), "COMMON"));
        openTitles.put(player.getUniqueId(), title);
        registerTitle(title, crate, false);
        player.openInventory(inv);
    }

    public static void openView(Player player, Crate crate) {
        String title = CC.translate("&cVer: &f" + crate.getId());
        if (title.length() > 32) {
            title = title.substring(0, 32);
        }
        Inventory inv = Bukkit.createInventory(null, 54, title);

        fillBorders(inv);
        inv.setItem(4, createInfoButton(crate));
        inv.setItem(49, createCloseButton());

        int index = 0;
        for (CrateItem crateItem : crate.getItems().values()) {
            if (index >= ITEM_SLOTS.length) break;
            ItemStack item = CrateItemSerializer.itemFromBase64(crateItem.getItemBase64());
            if (item == null) continue;
            item = addRarityLore(item, crate, crateItem);
            inv.setItem(ITEM_SLOTS[index++], item);
        }

        openTitles.put(player.getUniqueId(), title);
        registerTitle(title, crate, true);
        player.openInventory(inv);
    }

    private static void registerTitle(String title, Crate crate, boolean viewOnly) {
        crateByTitle.put(title, crate);
        titleRefCount.put(title, titleRefCount.getOrDefault(title, 0) + 1);
        if (viewOnly) {
            viewOnlyTitles.add(title);
        }
    }

    private static void fillBorders(Inventory inv) {
        for (int i = 0; i < 54; i++) {
            if (isBorder(i)) {
                inv.setItem(i, createGlassPane((short) 14));
            } else {
                inv.setItem(i, null);
            }
        }
    }

    private static boolean isBorder(int slot) {
        int row = slot / 9;
        int col = slot % 9;
        return row == 0 || row == 5 || col == 0 || col == 8;
    }

    private static boolean isItemSlot(int slot) {
        for (int s : ITEM_SLOTS) {
            if (s == slot) return true;
        }
        return false;
    }

    private static boolean isCrateTitle(String title) {
        return crateByTitle.containsKey(title);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        if (!isCrateTitle(title)) return;

        Inventory inv = event.getInventory();
        Crate crate = crateByTitle.get(title);
        boolean viewOnly = viewOnlyTitles.contains(title);
        int rawSlot = event.getRawSlot();

        if (viewOnly) {
            event.setCancelled(true);
            if (rawSlot == 49) {
                player.closeInventory();
            }
            return;
        }

        if (rawSlot == 51) {
            event.setCancelled(true);
            player.closeInventory();
            if (crate != null) {
                org.debentialc.crates.menus.CrateProbabilityMenu.open(player, crate);
            }
            return;
        }

        // Clicks in the player's own inventory (bottom part) are allowed
        if (rawSlot >= inv.getSize()) {
            return;
        }

        if (rawSlot < 0 || rawSlot >= inv.getSize()) {
            return;
        }

        // Bottom bar buttons: rarity (49) and close (53)
        if (rawSlot == 49) {
            event.setCancelled(true);
            cycleRarity(player);
            updateRarityButton(inv, player);
            return;
        }
        if (rawSlot == 53) {
            event.setCancelled(true);
            player.closeInventory();
            return;
        }

        // Borders are locked
        if (isBorder(rawSlot)) {
            event.setCancelled(true);
            return;
        }

        // Item slots: allow editing, right-click removes
        if (isItemSlot(rawSlot)) {
            ItemStack current = inv.getItem(rawSlot);
            if (current != null && current.getTypeId() != 0 && event.isRightClick()) {
                event.setCancelled(true);
                inv.setItem(rawSlot, null);
                player.updateInventory();
            }
            return;
        }

        // Any other non-item slot is locked
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        if (!isCrateTitle(title)) return;

        Inventory inv = event.getInventory();
        boolean viewOnly = viewOnlyTitles.contains(title);

        if (viewOnly) {
            event.setCancelled(true);
            return;
        }

        for (int slot : event.getRawSlots()) {
            // Player's own inventory slots are allowed
            if (slot >= inv.getSize()) {
                continue;
            }
            // Crate slots must be item slots, otherwise cancel the drag
            if (!isItemSlot(slot)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();
        if (!isCrateTitle(title)) return;

        try {
            openTitles.remove(player.getUniqueId());
            Inventory inv = event.getInventory();
            Crate crate = crateByTitle.get(title);
            if (crate == null) return;

            boolean viewOnly = viewOnlyTitles.contains(title);
            if (viewOnly) {
                releaseTitle(title);
                return;
            }

            List<CrateItem> newItems = new ArrayList<>();
            Map<Integer, String> rarities = originalRarities.getOrDefault(player.getUniqueId(), new HashMap<>());
            Map<Integer, String> base64s = originalBase64.getOrDefault(player.getUniqueId(), new HashMap<>());
            Map<Integer, Double> chances = originalChances.getOrDefault(player.getUniqueId(), new HashMap<>());
            Map<Integer, String> displays = displayBase64.getOrDefault(player.getUniqueId(), new HashMap<>());
            String selected = selectedRarity.getOrDefault(player.getUniqueId(), "COMMON");
            if (CrateManager.getInstance().getRarity(selected) == null) {
                if (!CrateManager.getInstance().getAllRarities().isEmpty()) {
                    selected = CrateManager.getInstance().getAllRarities().keySet().iterator().next();
                } else {
                    player.sendMessage(CC.translate("&c✗ No hay rarezas creadas. Crea una antes de guardar items."));
                    releaseTitle(title);
                    return;
                }
            }

            for (int slot : ITEM_SLOTS) {
                ItemStack item = inv.getItem(slot);
                if (item == null || item.getTypeId() == 0) continue;

                String rawBase64 = CrateItemSerializer.itemToBase64(item);
                String displayB64 = displays.get(slot);
                String originalB64 = base64s.get(slot);

                String base64;
                // Si el item mostrado es exactamente el original con el lore añadido,
                // usamos el base64 original para no perder ni un espacio del lore.
                if (displayB64 != null && displayB64.equals(rawBase64) && originalB64 != null) {
                    base64 = originalB64;
                } else {
                    item = removeRarityLore(item);
                    base64 = CrateItemSerializer.itemToBase64(item);
                    if (base64 == null) continue;
                }

                String originalRarity = rarities.get(slot);
                Double originalChance = chances.get(slot);
                String itemRarity;
                double itemChance;
                if (originalB64 != null && originalB64.equals(base64) && originalRarity != null
                        && CrateManager.getInstance().getRarity(originalRarity) != null) {
                    itemRarity = originalRarity;
                    itemChance = originalChance != null ? originalChance : 0.0;
                } else {
                    itemRarity = selected;
                    itemChance = 0.0;
                }

                String itemId = "item_" + System.currentTimeMillis() + "_" + slot;
                newItems.add(new CrateItem(itemId, itemRarity, base64, itemChance));
            }

            crate.getItems().clear();
            for (CrateItem crateItem : newItems) {
                crate.getItems().put(crateItem.getId(), crateItem);
            }
            CrateManager.getInstance().saveCrate(crate);
            player.sendMessage(CC.translate("&a✓ Items de la crate &f" + crate.getDisplayName() + " &aguardados."));
        } catch (Exception e) {
            player.sendMessage(CC.translate("&c✗ Error interno al guardar la crate. Revisa la consola."));
            e.printStackTrace();
        } finally {
            originalRarities.remove(player.getUniqueId());
            originalBase64.remove(player.getUniqueId());
            originalChances.remove(player.getUniqueId());
            displayBase64.remove(player.getUniqueId());
            releaseTitle(title);
        }
    }

    private static void releaseTitle(String title) {
        int count = titleRefCount.getOrDefault(title, 0) - 1;
        if (count <= 0) {
            titleRefCount.remove(title);
            crateByTitle.remove(title);
            viewOnlyTitles.remove(title);
        } else {
            titleRefCount.put(title, count);
        }
    }

    private static void cycleRarity(Player player) {
        List<String> rarityIds = new ArrayList<>(CrateManager.getInstance().getAllRarities().keySet());
        if (rarityIds.isEmpty()) {
            player.sendMessage(CC.translate("&c✗ No hay rarezas creadas. Usa &f/crate rarity create <id>&c."));
            return;
        }
        String current = selectedRarity.getOrDefault(player.getUniqueId(), rarityIds.get(0));
        int index = rarityIds.indexOf(current);
        String next = rarityIds.get((index + 1) % rarityIds.size());
        selectedRarity.put(player.getUniqueId(), next);
        CrateRarity rarity = CrateManager.getInstance().getRarity(next);
        String name = rarity != null ? rarity.getDisplayName() : next;
        player.sendMessage(CC.translate("&a✓ Rareza seleccionada: &f" + rarity.getColor() + name + " &7(" + rarity.getWeight() + ")"));
    }

    private static void updateRarityButton(Inventory inv, Player player) {
        inv.setItem(49, createRarityButton(selectedRarity.getOrDefault(player.getUniqueId(), "COMMON")));
    }

    private static ItemStack createRarityButton(String rarityId) {
        CrateRarity rarity = CrateManager.getInstance().getRarity(rarityId);
        ItemStack item = new ItemStack(Material.DIAMOND);
        ItemMeta meta = item.getItemMeta();
        if (rarity != null) {
            meta.setDisplayName(CC.translate("&b&lRareza: " + rarity.getColor() + rarity.getDisplayName()));
            List<String> lore = new ArrayList<>();
            lore.add(CC.translate("&7Peso (probabilidad): &f" + rarity.getWeight()));
            lore.add(CC.translate("&7Anunciar: &f" + (rarity.isAnnounce() ? "Sí" : "No")));
            lore.add(CC.translate("&7Click para cambiar"));
            meta.setLore(lore);
        } else {
            meta.setDisplayName(CC.translate("&b&lRareza seleccionada: &f" + rarityId));
            List<String> lore = new ArrayList<>();
            lore.add(CC.translate("&cNo existe. Crea una con /crate rarity create"));
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createCloseButton() {
        ItemStack item = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CC.translate("&c&lGuardar y cerrar"));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createProbabilityButton() {
        ItemStack item = new ItemStack(Material.BOOK_AND_QUILL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CC.translate("&e&lProbabilidades"));
        List<String> lore = new ArrayList<>();
        lore.add(CC.translate("&7Click para editar la"));
        lore.add(CC.translate("&7probabilidad de cada item"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createInfoButton(Crate crate) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CC.translate("&b&lItems de la crate"));
        List<String> lore = new ArrayList<>();
        lore.add(CC.translate("&7Total: &f" + crate.getItems().size()));
        lore.add(CC.translate("&7Solo visualización"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createGlassPane() {
        return createGlassPane((short) 14);
    }

    private static ItemStack createGlassPane(short color) {
        ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, color);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CC.translate("&8"));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack addRarityLore(ItemStack item, Crate crate, CrateItem crateItem) {
        item = item.clone();
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        double percent = CrateManager.getInstance().getItemChancePercent(crate, crateItem);
        lore.add(CC.translate("&8"));
        lore.add(CC.translate("&7Rareza: &f" + crateItem.getRarityId()));
        if (crateItem.getChance() > 0) {
            lore.add(CC.translate("&7Chance: &f" + crateItem.getChance()));
        }
        lore.add(CC.translate("&7Probabilidad: &f" + percent + "%"));
        lore.add(CC.translate("&8"));
        lore.add(CC.translate("&cClick derecho para eliminar"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack removeRarityLore(ItemStack item) {
        item = item.clone();
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return item;
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();
        List<String> newLore = new ArrayList<>();
        for (String line : lore) {
            String translated = CC.translate(line);
            if (translated.contains("Rareza:") || translated.contains("Probabilidad:") || translated.contains("Chance:") || translated.contains("eliminar")) continue;
            if (line.trim().isEmpty()) continue;
            newLore.add(line);
        }
        meta.setLore(newLore);
        item.setItemMeta(meta);
        return item;
    }
}
