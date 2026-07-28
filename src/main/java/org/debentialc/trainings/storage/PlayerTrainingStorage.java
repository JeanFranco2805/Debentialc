package org.debentialc.trainings.storage;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.debentialc.Main;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class PlayerTrainingStorage {

    private static final PlayerTrainingStorage INSTANCE = new PlayerTrainingStorage();

    private File playerTrainingsFile;
    private FileConfiguration playerTrainingsConfig;

    private PlayerTrainingStorage() {
        File dataFolder = Main.instance.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        this.playerTrainingsFile = new File(dataFolder, "player_trainings.yml");
        loadPlayerTrainingsConfig();
    }

    public static PlayerTrainingStorage getInstance() {
        return INSTANCE;
    }

    private void loadPlayerTrainingsConfig() {
        if (!playerTrainingsFile.exists()) {
            try {
                playerTrainingsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.playerTrainingsConfig = YamlConfiguration.loadConfiguration(playerTrainingsFile);
    }

    public int getPlayerTrainingLevel(UUID uuid, String trainingId) {
        return playerTrainingsConfig.getInt("players." + uuid.toString() + "." + trainingId, 0);
    }

    public void setPlayerTrainingLevel(UUID uuid, String trainingId, int level) {
        playerTrainingsConfig.set("players." + uuid.toString() + "." + trainingId, level);
        savePlayerTrainingsConfig();
    }

    public void resetPlayerTrainings(UUID uuid) {
        playerTrainingsConfig.set("players." + uuid.toString(), null);
        savePlayerTrainingsConfig();
    }

    private void savePlayerTrainingsConfig() {
        try {
            playerTrainingsConfig.save(playerTrainingsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
