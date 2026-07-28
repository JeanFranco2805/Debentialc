package org.debentialc.rebirths.managers;

import org.bukkit.entity.Player;
import org.debentialc.rebirths.model.Rebirth;
import org.debentialc.rebirths.model.RebirthBlock;
import org.debentialc.rebirths.storage.RebirthBlockStorage;
import org.debentialc.service.General;

import java.util.*;

public class RebirthBlockManager {

    private static final RebirthBlockManager INSTANCE = new RebirthBlockManager();

    private final Map<String, RebirthBlock> blocks = new LinkedHashMap<>();

    private int nextBlockNumber = 1;

    private RebirthBlockManager() {
        loadBlocks();
    }

    public static RebirthBlockManager getInstance() {
        return INSTANCE;
    }

    public void loadBlocks() {
        blocks.clear();
        blocks.putAll(RebirthBlockStorage.getInstance().loadAllBlocks());
        recalculateNextBlockNumber();
    }

    private void recalculateNextBlockNumber() {
        int max = 0;
        for (String id : blocks.keySet()) {
            if (id != null && id.length() > 1 && id.startsWith("A")) {
                try {
                    int number = Integer.parseInt(id.substring(1));
                    if (number > max) {
                        max = number;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        this.nextBlockNumber = max + 1;
    }

    public void saveBlock(RebirthBlock block) {
        blocks.put(block.getId(), block);
        RebirthBlockStorage.getInstance().saveBlock(block);
    }

    public void deleteBlock(String id) {
        blocks.remove(id);
        RebirthBlockStorage.getInstance().deleteBlock(id);
    }

    public RebirthBlock getBlock(String id) {
        return blocks.get(id);
    }

    public Collection<RebirthBlock> getAllBlocks() {
        return Collections.unmodifiableCollection(blocks.values());
    }

    public Collection<RebirthBlock> getVipBlocks() {
        List<RebirthBlock> vip = new ArrayList<>();
        for (RebirthBlock block : blocks.values()) {
            if (block.isVip()) vip.add(block);
        }
        return vip;
    }

    public Collection<RebirthBlock> getNormalBlocks() {
        List<RebirthBlock> normal = new ArrayList<>();
        for (RebirthBlock block : blocks.values()) {
            if (!block.isVip()) normal.add(block);
        }
        return normal;
    }

    public String getNextBlockId() {
        return "A" + nextBlockNumber++;
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
            if (b == block || b.getId().equals(block.getId())) break;
            previous = b;
        }
        return previous;
    }

    public int getLocalRebirthLevel(Player player, String blockId) {
        return getLocalRebirthLevel(player.getUniqueId(), blockId);
    }

    public int getLocalRebirthLevel(UUID uuid, String blockId) {
        RebirthBlock block = getBlock(blockId);
        if (block == null) return 0;

        Set<Integer> unlocked = RebirthManager.getInstance().getUnlockedRebirths(uuid);
        int localLevel = 0;
        for (int rebirthId : block.getRebirthIds()) {
            if (unlocked.contains(rebirthId)) {
                localLevel++;
            }
        }
        return localLevel;
    }

    public boolean isBlockCompleted(Player player, RebirthBlock block) {
        if (block == null || block.getRebirthIds().isEmpty()) return false;
        Set<Integer> unlocked = RebirthManager.getInstance().getUnlockedRebirths(player);
        for (int rebirthId : block.getRebirthIds()) {
            if (!unlocked.contains(rebirthId)) {
                return false;
            }
        }
        return true;
    }

    public boolean isBlockAvailable(Player player, RebirthBlock block) {
        return block != null;
    }

    public boolean canUnlockRebirthInBlock(Player player, Rebirth rebirth) {
        if (rebirth == null) return false;
        RebirthBlock block = getBlockForRebirth(rebirth.getId());
        if (block == null) return false;
        if (!isBlockAvailable(player, block)) return false;

        String requiredPermission = block.getRequiredPermission();
        if (requiredPermission != null && !requiredPermission.isEmpty() && !player.hasPermission(requiredPermission)) {
            return false;
        }

        int playerLevel = General.getLVL(player);
        return playerLevel >= rebirth.getRequiredLevel();
    }

}
