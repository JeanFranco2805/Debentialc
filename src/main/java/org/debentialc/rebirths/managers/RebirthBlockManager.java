package org.debentialc.rebirths.managers;

import org.bukkit.entity.Player;
import org.debentialc.rebirths.model.PlayerBlockStats;
import org.debentialc.rebirths.model.Rebirth;
import org.debentialc.rebirths.model.RebirthBlock;
import org.debentialc.rebirths.storage.RebirthBlockStorage;
import org.debentialc.service.General;

import java.util.*;

public class RebirthBlockManager {

    private static final RebirthBlockManager INSTANCE = new RebirthBlockManager();

    private final Map<Integer, RebirthBlock> blocks = new TreeMap<>();
    private final Map<UUID, Map<Integer, PlayerBlockStats>> playerBlockStatsCache = new HashMap<>();

    private static final List<String> STAT_KEYS = Arrays.asList("STR", "DEX", "CON", "WIL", "MND", "SPI");

    private RebirthBlockManager() {
        loadBlocks();
    }

    public static RebirthBlockManager getInstance() {
        return INSTANCE;
    }

    public void loadBlocks() {
        blocks.clear();
        blocks.putAll(RebirthBlockStorage.getInstance().loadAllBlocks());
    }

    public void saveBlock(RebirthBlock block) {
        blocks.put(block.getId(), block);
        RebirthBlockStorage.getInstance().saveBlock(block);
    }

    public void deleteBlock(int id) {
        blocks.remove(id);
        RebirthBlockStorage.getInstance().deleteBlock(id);
    }

    public RebirthBlock getBlock(int id) {
        return blocks.get(id);
    }

    public Collection<RebirthBlock> getAllBlocks() {
        return Collections.unmodifiableCollection(blocks.values());
    }

    public int getNextBlockId() {
        if (blocks.isEmpty()) return 1;
        return blocks.keySet().stream().max(Integer::compareTo).orElse(0) + 1;
    }

    public RebirthBlock getBlockForRebirth(int rebirthId) {
        for (RebirthBlock block : blocks.values()) {
            if (block.containsRebirth(rebirthId)) {
                return block;
            }
        }
        return null;
    }

    public RebirthBlock getPreviousBlock(RebirthBlock block) {
        if (block == null) return null;
        RebirthBlock previous = null;
        for (RebirthBlock b : blocks.values()) {
            if (b.getId() >= block.getId()) break;
            previous = b;
        }
        return previous;
    }

    public int getLocalRebirthLevel(int globalRebirthLevel, int blockId) {
        RebirthBlock block = getBlock(blockId);
        if (block == null) return 0;

        int localLevel = 0;
        for (int rebirthId : block.getRebirthIds()) {
            if (globalRebirthLevel >= rebirthId) {
                localLevel++;
            } else {
                break;
            }
        }
        return localLevel;
    }

    public boolean isBlockAvailable(Player player, RebirthBlock block) {
        if (block == null) return false;
        if (block.getId() == 1) return true;
        RebirthBlock previous = getPreviousBlock(block);
        if (previous == null) return true;
        int playerLevel = RebirthManager.getInstance().getPlayerRebirthLevel(player);
        return previous.isCompleted(playerLevel);
    }

    public boolean canUnlockRebirthInBlock(Player player, Rebirth rebirth) {
        if (rebirth == null) return false;
        RebirthBlock block = getBlockForRebirth(rebirth.getId());
        if (block == null) return false;
        if (!isBlockAvailable(player, block)) return false;

        int playerRebirth = RebirthManager.getInstance().getPlayerRebirthLevel(player);
        int requiredPrevious = rebirth.getId() - 1;
        if (playerRebirth < requiredPrevious) return false;

        int playerLevel = General.getLVL(player);
        return playerLevel >= rebirth.getRequiredLevel();
    }

    public PlayerBlockStats getPlayerBlockStats(Player player, int blockId) {
        Map<Integer, PlayerBlockStats> playerStats = playerBlockStatsCache.computeIfAbsent(player.getUniqueId(), uuid -> new HashMap<>());
        return playerStats.computeIfAbsent(blockId, id -> RebirthBlockStorage.getInstance().loadPlayerBlockStats(player.getUniqueId(), id));
    }

    public void savePlayerBlockStats(Player player, int blockId, PlayerBlockStats stats) {
        playerBlockStatsCache.computeIfAbsent(player.getUniqueId(), uuid -> new HashMap<>()).put(blockId, stats);
        RebirthBlockStorage.getInstance().savePlayerBlockStats(player.getUniqueId(), blockId, stats);
    }

    public void clearPlayerBlockStats(Player player, int blockId) {
        Map<Integer, PlayerBlockStats> playerStats = playerBlockStatsCache.get(player.getUniqueId());
        if (playerStats != null) {
            playerStats.remove(blockId);
        }
        RebirthBlockStorage.getInstance().clearPlayerBlockStats(player.getUniqueId(), blockId);
    }

    public void clearAllPlayerBlockStats(Player player) {
        playerBlockStatsCache.remove(player.getUniqueId());
        RebirthBlockStorage.getInstance().clearAllPlayerBlockStats(player.getUniqueId());
    }

    public void clearAllPlayerBlockStats(UUID uuid) {
        playerBlockStatsCache.remove(uuid);
        RebirthBlockStorage.getInstance().clearAllPlayerBlockStats(uuid);
    }

    public void unloadPlayerData(Player player) {
        playerBlockStatsCache.remove(player.getUniqueId());
    }

    public Map<String, Integer> capturePlayerStats(Player player) {
        Map<String, Integer> stats = new LinkedHashMap<>();
        for (String stat : STAT_KEYS) {
            stats.put(stat, General.getSTAT(stat, player));
        }
        return stats;
    }

    public void restorePlayerStats(Player player, Map<String, Integer> stats) {
        for (Map.Entry<String, Integer> entry : stats.entrySet()) {
            General.setSTAT(entry.getKey(), player, entry.getValue());
        }
    }
}
