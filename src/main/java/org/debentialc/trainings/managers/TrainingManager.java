package org.debentialc.trainings.managers;

import noppes.npcs.api.entity.IDBCPlayer;
import noppes.npcs.scripted.NpcAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.debentialc.service.CC;
import org.debentialc.service.General;
import org.debentialc.trainings.TrainingClassCostFilter;
import org.debentialc.trainings.model.Training;
import org.debentialc.trainings.model.TrainingLevel;
import org.debentialc.trainings.storage.PlayerTrainingStorage;
import org.debentialc.trainings.storage.TrainingStorage;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TrainingManager {

    private static final TrainingManager INSTANCE = new TrainingManager();

    private final Map<String, Training> trainings = new LinkedHashMap<>();
    private final Map<UUID, Map<String, Integer>> playerLevelCache = new HashMap<>();

    private static final Pattern TRAINING_ID_PATTERN = Pattern.compile("^[Tt](\\d+)$");

    private TrainingManager() {
        loadTrainings();
    }

    public static TrainingManager getInstance() {
        return INSTANCE;
    }

    public void loadTrainings() {
        trainings.clear();
        trainings.putAll(TrainingStorage.getInstance().loadAllTrainings());
    }

    public void saveTraining(Training training) {
        trainings.put(training.getId(), training);
        TrainingStorage.getInstance().saveTraining(training);
    }

    public void deleteTraining(String id) {
        trainings.remove(id);
        TrainingStorage.getInstance().deleteTraining(id);
    }

    public Training getTraining(String id) {
        return trainings.get(id);
    }

    public Collection<Training> getAllTrainings() {
        return Collections.unmodifiableCollection(trainings.values());
    }

    public List<Training> getNormalTrainings() {
        List<Training> result = new ArrayList<>();
        for (Training training : trainings.values()) {
            if (!training.isVip()) {
                result.add(training);
            }
        }
        return result;
    }

    public List<Training> getVipTrainings() {
        List<Training> result = new ArrayList<>();
        for (Training training : trainings.values()) {
            if (training.isVip()) {
                result.add(training);
            }
        }
        return result;
    }

    public String getNextTrainingId() {
        int maxNumber = 0;
        for (String id : trainings.keySet()) {
            Matcher matcher = TRAINING_ID_PATTERN.matcher(id);
            if (matcher.matches()) {
                int number = Integer.parseInt(matcher.group(1));
                if (number > maxNumber) {
                    maxNumber = number;
                }
            }
        }
        return "T" + (maxNumber + 1);
    }

    public int getPlayerTrainingLevel(Player player, String trainingId) {
        return getPlayerTrainingLevel(player.getUniqueId(), trainingId);
    }

    public void setPlayerTrainingLevel(Player player, String trainingId, int level) {
        setPlayerTrainingLevel(player.getUniqueId(), trainingId, level);
    }

    public int getPlayerTrainingLevel(UUID uuid, String trainingId) {
        Map<String, Integer> levels = playerLevelCache.get(uuid);
        if (levels != null && levels.containsKey(trainingId)) {
            return levels.get(trainingId);
        }
        int level = PlayerTrainingStorage.getInstance().getPlayerTrainingLevel(uuid, trainingId);
        cachePlayerLevel(uuid, trainingId, level);
        return level;
    }

    public String getLatestTrainingId(UUID uuid) {
        String highestWithLevel = null;
        int highestWithLevelNumber = -1;
        String lowestId = null;
        int lowestNumber = Integer.MAX_VALUE;

        for (String id : trainings.keySet()) {
            Matcher matcher = TRAINING_ID_PATTERN.matcher(id);
            if (!matcher.matches()) {
                continue;
            }
            int number = Integer.parseInt(matcher.group(1));
            if (number < lowestNumber) {
                lowestNumber = number;
                lowestId = id;
            }
            int level = getPlayerTrainingLevel(uuid, id);
            if (level > 0 && number > highestWithLevelNumber) {
                highestWithLevelNumber = number;
                highestWithLevel = id;
            }
        }
        return highestWithLevel != null ? highestWithLevel : lowestId;
    }

    public void setPlayerTrainingLevel(UUID uuid, String trainingId, int level) {
        cachePlayerLevel(uuid, trainingId, level);
        PlayerTrainingStorage.getInstance().setPlayerTrainingLevel(uuid, trainingId, level);
    }

    public void resetPlayerTrainings(UUID uuid) {
        playerLevelCache.remove(uuid);
        PlayerTrainingStorage.getInstance().resetPlayerTrainings(uuid);
    }

    public void loadPlayerData(Player player) {
        playerLevelCache.remove(player.getUniqueId());
    }

    public void unloadPlayerData(Player player) {
        playerLevelCache.remove(player.getUniqueId());
    }

    private void cachePlayerLevel(UUID uuid, String trainingId, int level) {
        playerLevelCache.computeIfAbsent(uuid, u -> new HashMap<>()).put(trainingId, level);
    }

    public double getTrainingMultiplier(Player player, String trainingId) {
        Training training = getTraining(trainingId);
        if (training == null) return 1.0;

        int currentLevel = getPlayerTrainingLevel(player, trainingId);
        if (currentLevel <= 0) return 1.0;

        TrainingLevel level = training.getLevel(currentLevel);
        if (level == null) return 1.0;

        return 1.0 + (level.getTpBonusPercent() / 100.0);
    }

    public boolean canUpgrade(Player player, String trainingId) {
        return getUpgradeFailureReason(player, trainingId) == null;
    }

    public String getUpgradeFailureReason(Player player, String trainingId) {
        Training training = getTraining(trainingId);
        if (training == null) return "Training no encontrado.";

        if (training.isVip()) {
            String permission = training.getRequiredPermission();
            if (permission != null && !permission.isEmpty() && !player.hasPermission(permission)) {
                return "No tienes permiso para este training VIP.";
            }
        }

        int currentLevel = getPlayerTrainingLevel(player, trainingId);
        TrainingLevel nextLevel = training.getLevel(currentLevel + 1);
        if (nextLevel == null) {
            return "Ya has alcanzado el nivel máximo.";
        }

        Map<String, Integer> filteredCosts = TrainingClassCostFilter.filterCosts(nextLevel.getStatCosts(), player);
        for (Map.Entry<String, Integer> entry : filteredCosts.entrySet()) {
            int current = getPlayerStat(player, entry.getKey());
            int required = entry.getValue();
            if (current < required) {
                return "Necesitas " + required + " de " + entry.getKey() + " (tienes " + current + ").";
            }
        }

        int currentTP = getPlayerTP(player);
        int requiredTP = nextLevel.getTpCost();
        if (currentTP < requiredTP) {
            return "Necesitas " + requiredTP + " TP (tienes " + currentTP + ").";
        }

        return null;
    }

    public boolean upgrade(Player player, String trainingId) {
        Training training = getTraining(trainingId);
        if (training == null) {
            player.sendMessage(CC.translate("&c✗ Training no encontrado."));
            return false;
        }

        String failureReason = getUpgradeFailureReason(player, trainingId);
        if (failureReason != null) {
            player.sendMessage(CC.translate("&c✗ " + failureReason));
            return false;
        }

        int currentLevel = getPlayerTrainingLevel(player, trainingId);
        TrainingLevel nextLevel = training.getLevel(currentLevel + 1);
        if (nextLevel == null) {
            player.sendMessage(CC.translate("&c✗ Ya has alcanzado el nivel máximo."));
            return false;
        }

        Map<String, Integer> filteredCosts = TrainingClassCostFilter.filterCosts(nextLevel.getStatCosts(), player);
        for (Map.Entry<String, Integer> entry : filteredCosts.entrySet()) {
            int current = getPlayerStat(player, entry.getKey());
            setPlayerStat(player, entry.getKey(), current - entry.getValue());
        }

        int currentTP = getPlayerTP(player);
        if (currentTP >= nextLevel.getTpCost()) {
            setPlayerTP(player, currentTP - nextLevel.getTpCost());
        }

        setPlayerTrainingLevel(player, trainingId, currentLevel + 1);

        for (String command : nextLevel.getRewardCommands()) {
            if (command == null || command.isEmpty()) continue;
            String formattedCommand = command.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCommand);
        }

        player.sendMessage(CC.translate("&a✓ Has subido &e" + training.getName() + " &aal nivel &f" + (currentLevel + 1) + "&a!"));
        return true;
    }

    private int getPlayerStat(Player player, String stat) {
        try {
            return General.getSTAT(stat, player);
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }

    private void setPlayerStat(Player player, String stat, int value) {
        try {
            General.setSTAT(stat, player, value);
        } catch (Exception e) {
            // ignore
        }
    }

    private int getPlayerTP(Player player) {
        try {
            IDBCPlayer dbcPlayer = NpcAPI.Instance().getPlayer(player.getName()).getDBCPlayer();
            return dbcPlayer.getTP();
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }

    private void setPlayerTP(Player player, int value) {
        try {
            IDBCPlayer dbcPlayer = NpcAPI.Instance().getPlayer(player.getName()).getDBCPlayer();
            dbcPlayer.setTP(value);
        } catch (Exception e) {
            // ignore
        }
    }
}
