package org.debentialc.rebirths.managers;

import org.bukkit.entity.Player;
import org.debentialc.rebirths.model.Rebirth;
import org.debentialc.rebirths.model.RebirthBlock;
import org.debentialc.rebirths.storage.RebirthStorage;

import java.util.*;

public class RebirthManager {

    private static final RebirthManager INSTANCE = new RebirthManager();

    private final Map<Integer, Rebirth> rebirths = new TreeMap<>();
    private final Map<UUID, Set<Integer>> playerUnlockedCache = new HashMap<>();

    private RebirthManager() {
        loadRebirths();
    }

    public static RebirthManager getInstance() {
        return INSTANCE;
    }

    public void loadRebirths() {
        rebirths.clear();
        rebirths.putAll(RebirthStorage.getInstance().loadAllRebirths());
    }

    public Set<Integer> getUnlockedRebirths(Player player) {
        return playerUnlockedCache.computeIfAbsent(player.getUniqueId(), uuid -> RebirthStorage.getInstance().loadPlayerUnlockedRebirths(uuid));
    }

    public Set<Integer> getUnlockedRebirths(UUID uuid) {
        return playerUnlockedCache.computeIfAbsent(uuid, u -> RebirthStorage.getInstance().loadPlayerUnlockedRebirths(u));
    }

    public boolean hasRebirth(Player player, int rebirthId) {
        return getUnlockedRebirths(player).contains(rebirthId);
    }

    public boolean hasRebirth(UUID uuid, int rebirthId) {
        return getUnlockedRebirths(uuid).contains(rebirthId);
    }

    public void unlockRebirth(Player player, int rebirthId) {
        Set<Integer> unlocked = getUnlockedRebirths(player);
        unlocked.add(rebirthId);
        RebirthStorage.getInstance().savePlayerUnlockedRebirths(player.getUniqueId(), unlocked);
    }

    public int getPlayerRebirthLevel(Player player) {
        Set<Integer> unlocked = playerUnlockedCache.get(player.getUniqueId());
        if (unlocked == null || unlocked.isEmpty()) return 0;
        return unlocked.stream().max(Integer::compareTo).orElse(0);
    }

    public void saveRebirth(Rebirth rebirth) {
        rebirths.put(rebirth.getId(), rebirth);
        RebirthStorage.getInstance().saveRebirth(rebirth);
    }

    public void deleteRebirth(int id) {
        Rebirth rebirth = rebirths.get(id);
        if (rebirth != null) {
            String blockId = rebirth.getBlockId();
            if (blockId != null) {
                RebirthBlock block = RebirthBlockManager.getInstance().getBlock(blockId);
                if (block != null) {
                    block.getRebirthIds().remove(Integer.valueOf(id));
                    RebirthBlockManager.getInstance().saveBlock(block);
                }
            }
        }
        rebirths.remove(id);
        RebirthStorage.getInstance().deleteRebirth(id);
    }

    public Rebirth createRebirthInBlock(String blockId) {
        RebirthBlock block = RebirthBlockManager.getInstance().getBlock(blockId);
        if (block == null) return null;

        int nextId = getNextRebirthId();
        Rebirth rebirth = new Rebirth(nextId);
        rebirth.setBlockId(blockId);

        // Valores por defecto para el primer rebirth
        if (nextId == 1) {
            rebirth.setRequiredLevel(1000);
            rebirth.setTpBonusPercent(15.0);
        }

        block.getRebirthIds().add(nextId);
        RebirthBlockManager.getInstance().saveBlock(block);
        saveRebirth(rebirth);

        return rebirth;
    }

    public List<Rebirth> getRebirthsInBlock(String blockId) {
        List<Rebirth> result = new ArrayList<>();
        RebirthBlock block = RebirthBlockManager.getInstance().getBlock(blockId);
        if (block == null) return result;
        for (int rebirthId : block.getRebirthIds()) {
            Rebirth rebirth = rebirths.get(rebirthId);
            if (rebirth != null) {
                result.add(rebirth);
            }
        }
        return result;
    }

    public Rebirth getRebirth(int id) {
        return rebirths.get(id);
    }

    public Collection<Rebirth> getAllRebirths() {
        return Collections.unmodifiableCollection(rebirths.values());
    }

    public boolean rebirthExists(int id) {
        return rebirths.containsKey(id);
    }

    public int getNextRebirthId() {
        if (rebirths.isEmpty()) {
            return 1;
        }
        return rebirths.keySet().stream().max(Integer::compareTo).orElse(0) + 1;
    }

    public void setPlayerRebirthLevel(Player player, int level) {
        // Backward compatibility: creates a sequential set up to level
        Set<Integer> unlocked = new HashSet<>();
        for (int i = 1; i <= level; i++) {
            unlocked.add(i);
        }
        playerUnlockedCache.put(player.getUniqueId(), unlocked);
        RebirthStorage.getInstance().savePlayerUnlockedRebirths(player.getUniqueId(), unlocked);
    }

    public void resetPlayerRebirthLevel(Player player) {
        playerUnlockedCache.remove(player.getUniqueId());
        RebirthStorage.getInstance().resetPlayerUnlockedRebirths(player.getUniqueId());
    }

    public void resetPlayerRebirthLevel(UUID uuid) {
        playerUnlockedCache.remove(uuid);
        RebirthStorage.getInstance().resetPlayerUnlockedRebirths(uuid);
    }

    public void loadPlayerData(Player player) {
        Set<Integer> unlocked = RebirthStorage.getInstance().loadPlayerUnlockedRebirths(player.getUniqueId());
        playerUnlockedCache.put(player.getUniqueId(), unlocked);
    }

    public void unloadPlayerData(Player player) {
        playerUnlockedCache.remove(player.getUniqueId());
    }

    public boolean canUnlockRebirth(Player player, Rebirth rebirth) {
        return RebirthBlockManager.getInstance().canUnlockRebirthInBlock(player, rebirth);
    }

    public double getRebirthMultiplier(Player player) {
        Set<Integer> unlocked = getUnlockedRebirths(player);
        if (unlocked.isEmpty()) return 1.0;

        double totalBonus = 0.0;
        for (int id : unlocked) {
            Rebirth rebirth = rebirths.get(id);
            if (rebirth != null) {
                totalBonus += rebirth.getTpBonusPercent();
            }
        }

        return 1.0 + (totalBonus / 100.0);
    }

    public int getHighestRebirthId() {
        return rebirths.keySet().stream().max(Integer::compareTo).orElse(0);
    }
}
