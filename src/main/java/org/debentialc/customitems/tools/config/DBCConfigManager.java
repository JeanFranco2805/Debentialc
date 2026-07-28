package org.debentialc.customitems.tools.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DBCConfigManager {

    private static final Map<String, Map<String, Double>> configCache = new HashMap<>();

    public static void loadAllConfigs() {
        String[] races = {"human", "saiyan", "half-saiyan", "namekian", "arcosian", "majin"};

        for (String race : races) {
            loadRaceConfig(race);
        }
    }

    private static void loadRaceConfig(String race) {
        File configFile = new File("config/jingames/dbc/" + race + "/main.cfg");

        if (!configFile.exists()) {
            System.out.println("[DBCConfigManager] Archivo no encontrado: " + configFile.getAbsolutePath());
            return;
        }

        try {
            String content = new String(Files.readAllBytes(configFile.toPath()));

            // Formatear nombre: "half-saiyan" -> "Half-Saiyan"
            String raceName = Arrays.stream(race.split("-"))
                    .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase())
                    .collect(Collectors.joining("-"));

            String[] classes = {"Spiritualist", "Warrior", "MartialArtist"};

            for (String dbcClass : classes) {
                String key = race + "_" + dbcClass;

                double body = extractMultiplier(content, raceName, dbcClass, "Body");
                double energyPool = extractMultiplier(content, raceName, dbcClass, "EnergyPool");
                double stamina = extractMultiplier(content, raceName, dbcClass, "Stamina");
                double melee = extractMultiplier(content, raceName, dbcClass, "Melee");
                Map<String, Double> classConfig = new HashMap<>();
                classConfig.put("Body", body);
                classConfig.put("EnergyPool", energyPool);
                classConfig.put("Stamina", stamina);
                classConfig.put("Melee", melee);

                configCache.put(key, classConfig);

                System.out.println("[DBCConfigManager] Cargado: " + key + " | Body: " + body + " | EnergyPool: " + energyPool + " | Stamina: " + stamina);
            }

        } catch (IOException e) {
            System.out.println("[DBCConfigManager] Error al leer " + configFile.getAbsolutePath());
            e.printStackTrace();
        }
    }

    private static double extractMultiplier(String content, String race, String dbcClass, String stat) {
        // Patrón: busca "DBC {Race} {Class} Stat Multiplier from Attribute" y luego el stat
        String pattern = "DBC " + race + " " + dbcClass + " Stat Multiplier from Attribute[\\s\\S]*?" + stat + "\\s+([0-9.]+)";

        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(content);

        if (m.find()) {
            try {
                double value = Double.parseDouble(m.group(1));
                return value;
            } catch (NumberFormatException e) {
                return getDefault(stat);
            }
        }

        return getDefault(stat);
    }

    private static double getDefault(String stat) {
        switch (stat) {
            case "Body":
                return 25.0;
            case "EnergyPool":
                return 40.0;
            case "Stamina":
                return 3.0;
            default:
                return 0.0;
        }
    }

    public static double getBodyMultiplier(String race, String dbcClass) {
        String key = race.toLowerCase() + "_" + dbcClass;
        Map<String, Double> config = configCache.getOrDefault(key, new HashMap<>());
        return config.getOrDefault("Body", 25.0);
    }

    public static double getEnergyPoolMultiplier(String race, String dbcClass) {
        String key = race.toLowerCase() + "_" + dbcClass;
        Map<String, Double> config = configCache.getOrDefault(key, new HashMap<>());
        return config.getOrDefault("EnergyPool", 40.0);
    }
    public static double getMeleeMultiplier(String race, String dbcClass) {
        String key = race.toLowerCase() + "_" + dbcClass;
        Map<String, Double> config = configCache.getOrDefault(key, new HashMap<>());
        return config.getOrDefault("Melee", 1.0);
    }
    public static double getStaminaMultiplier(String race, String dbcClass) {
        String key = race.toLowerCase() + "_" + dbcClass;
        Map<String, Double> config = configCache.getOrDefault(key, new HashMap<>());
        return config.getOrDefault("Stamina", 3.0);
    }
}