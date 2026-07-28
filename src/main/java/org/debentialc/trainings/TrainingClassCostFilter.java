package org.debentialc.trainings;

import org.bukkit.entity.Player;
import org.debentialc.service.General;

import java.util.HashMap;
import java.util.Map;

public class TrainingClassCostFilter {

    public static Map<String, Integer> filterCosts(Map<String, Integer> statCosts, Player player) {
        String playerClass = getPlayerClass(player);
        return filterCosts(statCosts, playerClass);
    }

    public static Map<String, Integer> filterCosts(Map<String, Integer> statCosts, String playerClass) {
        Map<String, Integer> filtered = new HashMap<>();
        if (statCosts == null) {
            return filtered;
        }

        if (playerClass == null) {
            for (Map.Entry<String, Integer> entry : statCosts.entrySet()) {
                if (entry.getValue() != null && entry.getValue() > 0) {
                    filtered.put(entry.getKey().toUpperCase(), entry.getValue());
                }
            }
            return filtered;
        }

        String normalizedClass = playerClass.toLowerCase().replaceAll("[^a-z]", "");

        for (Map.Entry<String, Integer> entry : statCosts.entrySet()) {
            String stat = entry.getKey().toUpperCase();
            Integer value = entry.getValue();
            if (value == null || value <= 0) continue;

            if (isSpiritualist(normalizedClass)) {
                if (stat.equals("SPI") || stat.equals("WIL") || stat.equals("CON")) {
                    filtered.put(stat, value);
                }
            } else if (isWarriorOrMartialArtist(normalizedClass)) {
                if (stat.equals("STR") || stat.equals("CON") || stat.equals("DEX")) {
                    filtered.put(stat, value);
                }
            } else {
                filtered.put(stat, value);
            }
        }

        return filtered;
    }

    public static String getPlayerClass(Player player) {
        try {
            return General.getClass(player);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isSpiritualist(String normalizedClass) {
        return normalizedClass.contains("spiritualist") || normalizedClass.equals("spiritual");
    }

    private static boolean isWarriorOrMartialArtist(String normalizedClass) {
        return normalizedClass.contains("warrior") || normalizedClass.contains("martialartist")
                || normalizedClass.contains("martial");
    }
}
