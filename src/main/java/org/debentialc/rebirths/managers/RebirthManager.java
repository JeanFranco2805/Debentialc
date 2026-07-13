package org.debentialc.rebirths.managers;

import org.bukkit.entity.Player;
import org.debentialc.rebirths.model.Rebirth;
import org.debentialc.rebirths.model.RebirthBlock;
import org.debentialc.rebirths.storage.RebirthStorage;

import java.util.*;

public class RebirthManager {

    private static final RebirthManager INSTANCE = new RebirthManager();

    private final Map<Integer, Rebirth> rebirths = new TreeMap<>();
    private final Map<UUID, Integer> playerRebirthCache = new HashMap<>();

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

    public void saveRebirth(Rebirth rebirth) {
        rebirths.put(rebirth.getId(), rebirth);
        RebirthStorage.getInstance().saveRebirth(rebirth);
    }

    public void deleteRebirth(int id) {
        Rebirth rebirth = rebirths.get(id);
        if (rebirth != null) {
            RebirthBlock block = RebirthBlockManager.getInstance().getBlock(rebirth.getBlockId());
            if (block != null) {
                block.getRebirthIds().remove(Integer.valueOf(id));
                RebirthBlockManager.getInstance().saveBlock(block);
            }
        }
        rebirths.remove(id);
        RebirthStorage.getInstance().deleteRebirth(id);
    }

    public Rebirth createRebirthInBlock(int blockId) {
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

    public List<Rebirth> getRebirthsInBlock(int blockId) {
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

    public int getPlayerRebirthLevel(Player player) {
        return playerRebirthCache.getOrDefault(player.getUniqueId(), 0);
    }

    public void setPlayerRebirthLevel(Player player, int level) {
        playerRebirthCache.put(player.getUniqueId(), level);
        RebirthStorage.getInstance().savePlayerRebirthLevel(player.getUniqueId(), level);
    }

    public void resetPlayerRebirthLevel(Player player) {
        playerRebirthCache.put(player.getUniqueId(), 0);
        RebirthStorage.getInstance().resetPlayerRebirthLevel(player.getUniqueId());
    }

    public void resetPlayerRebirthLevel(UUID uuid) {
        playerRebirthCache.remove(uuid);
        RebirthStorage.getInstance().resetPlayerRebirthLevel(uuid);
    }

    public void loadPlayerData(Player player) {
        int level = RebirthStorage.getInstance().loadPlayerRebirthLevel(player.getUniqueId());
        playerRebirthCache.put(player.getUniqueId(), level);
    }

    public void unloadPlayerData(Player player) {
        playerRebirthCache.remove(player.getUniqueId());
    }

    public boolean canUnlockRebirth(Player player, Rebirth rebirth) {
        return RebirthBlockManager.getInstance().canUnlockRebirthInBlock(player, rebirth);
    }

    public boolean hasRebirth(Player player, int rebirthId) {
        return getPlayerRebirthLevel(player) >= rebirthId;
    }

    public double getRebirthMultiplier(Player player) {
        int level = getPlayerRebirthLevel(player);
        if (level <= 0) return 1.0;

        Rebirth rebirth = rebirths.get(level);
        if (rebirth == null) return 1.0;

        return rebirth.getMultiplier();
    }

    public int getHighestRebirthId() {
        return rebirths.keySet().stream().max(Integer::compareTo).orElse(0);
    }
}
