package org.debentialc.trainings.storage;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.debentialc.Main;
import org.debentialc.trainings.model.Training;
import org.debentialc.trainings.model.TrainingLevel;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TrainingStorage {

    private static final TrainingStorage INSTANCE = new TrainingStorage();

    private File trainingsFile;
    private FileConfiguration trainingsConfig;

    private TrainingStorage() {
        File dataFolder = Main.instance.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        this.trainingsFile = new File(dataFolder, "trainings.yml");
        loadTrainingsConfig();
    }

    public static TrainingStorage getInstance() {
        return INSTANCE;
    }

    private void loadTrainingsConfig() {
        if (!trainingsFile.exists()) {
            try {
                trainingsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.trainingsConfig = YamlConfiguration.loadConfiguration(trainingsFile);
    }

    public void saveTraining(Training training) {
        String path = "trainings." + training.getId();
        trainingsConfig.set(path + ".name", training.getName());
        trainingsConfig.set(path + ".materialId", training.getMaterialId());
        trainingsConfig.set(path + ".vip", training.isVip());
        trainingsConfig.set(path + ".requiredPermission", training.getRequiredPermission());

        trainingsConfig.set(path + ".npcTpBase", null);
        if (training.getNpcTpBase() != null && !training.getNpcTpBase().isEmpty()) {
            for (Map.Entry<String, Integer> entry : training.getNpcTpBase().entrySet()) {
                trainingsConfig.set(path + ".npcTpBase." + entry.getKey(), entry.getValue());
            }
        }

        trainingsConfig.set(path + ".levels", null);
        for (TrainingLevel level : training.getLevels()) {
            String levelPath = path + ".levels." + level.getLevel();
            trainingsConfig.set(levelPath + ".description", level.getDescription());
            trainingsConfig.set(levelPath + ".tpBonusPercent", level.getTpBonusPercent());
            trainingsConfig.set(levelPath + ".rewardCommands", level.getRewardCommands());
            trainingsConfig.set(levelPath + ".tpCost", level.getTpCost());

            trainingsConfig.set(levelPath + ".statCosts", null);
            if (level.getStatCosts() != null && !level.getStatCosts().isEmpty()) {
                for (Map.Entry<String, Integer> entry : level.getStatCosts().entrySet()) {
                    if (entry.getValue() != null && entry.getValue() > 0) {
                        trainingsConfig.set(levelPath + ".statCosts." + entry.getKey().toUpperCase(), entry.getValue());
                    }
                }
            }
        }

        saveTrainingsConfig();
    }

    public void deleteTraining(String id) {
        trainingsConfig.set("trainings." + id, null);
        saveTrainingsConfig();
    }

    public Map<String, Training> loadAllTrainings() {
        Map<String, Training> trainings = new LinkedHashMap<>();

        if (!trainingsConfig.contains("trainings")) {
            return trainings;
        }

        org.bukkit.configuration.ConfigurationSection section = trainingsConfig.getConfigurationSection("trainings");
        if (section == null) return trainings;

        for (String key : section.getKeys(false)) {
            String path = "trainings." + key;
            String name = trainingsConfig.getString(path + ".name", "Training " + key);
            int materialId = trainingsConfig.getInt(path + ".materialId", 339);
            boolean vip = trainingsConfig.getBoolean(path + ".vip", false);
            String requiredPermission = trainingsConfig.getString(path + ".requiredPermission", null);

            Map<String, Integer> npcTpBase = new HashMap<>();
            if (trainingsConfig.contains(path + ".npcTpBase")) {
                org.bukkit.configuration.ConfigurationSection npcSection = trainingsConfig.getConfigurationSection(path + ".npcTpBase");
                if (npcSection != null) {
                    for (String npcId : npcSection.getKeys(false)) {
                        int value = trainingsConfig.getInt(path + ".npcTpBase." + npcId, 0);
                        if (value > 0) {
                            npcTpBase.put(npcId, value);
                        }
                    }
                }
            }

            List<TrainingLevel> levels = new ArrayList<>();
            if (trainingsConfig.contains(path + ".levels")) {
                org.bukkit.configuration.ConfigurationSection levelsSection = trainingsConfig.getConfigurationSection(path + ".levels");
                if (levelsSection != null) {
                    List<String> levelKeys = new ArrayList<>(levelsSection.getKeys(false));
                    levelKeys.sort(Comparator.comparingInt(Integer::parseInt));

                    for (String levelKey : levelKeys) {
                        try {
                            int levelNumber = Integer.parseInt(levelKey);
                            String levelPath = path + ".levels." + levelKey;
                            String description = trainingsConfig.getString(levelPath + ".description", null);
                            double tpBonusPercent = trainingsConfig.getDouble(levelPath + ".tpBonusPercent", 0.0);
                            List<String> rewardCommands = trainingsConfig.getStringList(levelPath + ".rewardCommands");
                            int tpCost = trainingsConfig.getInt(levelPath + ".tpCost", 0);

                            Map<String, Integer> statCosts = new HashMap<>();
                            if (trainingsConfig.contains(levelPath + ".statCosts")) {
                                org.bukkit.configuration.ConfigurationSection costsSection = trainingsConfig.getConfigurationSection(levelPath + ".statCosts");
                                if (costsSection != null) {
                                    for (String statKey : costsSection.getKeys(false)) {
                                        int value = trainingsConfig.getInt(levelPath + ".statCosts." + statKey, 0);
                                        if (value > 0) {
                                            statCosts.put(statKey.toUpperCase(), value);
                                        }
                                    }
                                }
                            }

                            levels.add(new TrainingLevel(levelNumber, description, tpBonusPercent, rewardCommands, statCosts, tpCost));
                        } catch (NumberFormatException e) {
                            System.err.println("[Trainings] Número de nivel inválido en config: " + levelKey);
                        }
                    }
                }
            }

            trainings.put(key, new Training(key, name, materialId, vip, requiredPermission, levels, npcTpBase));
        }

        return trainings;
    }

    private void saveTrainingsConfig() {
        try {
            trainingsConfig.save(trainingsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
