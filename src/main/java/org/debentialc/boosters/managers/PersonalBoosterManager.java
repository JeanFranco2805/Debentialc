package org.debentialc.boosters.managers;

import org.debentialc.boosters.models.PersonalBooster;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PersonalBoosterManager {

    private static final Map<UUID, PersonalBooster> activeBoosters = new ConcurrentHashMap<>();

    public static void setBooster(UUID playerId, double multiplier, long durationSeconds) {
        PersonalBooster booster = new PersonalBooster(playerId, multiplier, durationSeconds);
        activeBoosters.put(playerId, booster);
    }

    public static void removeBooster(UUID playerId) {
        activeBoosters.remove(playerId);
    }

    public static PersonalBooster getActiveBooster(UUID playerId) {
        PersonalBooster booster = activeBoosters.get(playerId);
        if (booster != null && booster.isStillActive()) {
            return booster;
        }
        if (booster != null && !booster.isStillActive()) {
            activeBoosters.remove(playerId);
        }
        return null;
    }

    public static double getActiveMultiplier(UUID playerId) {
        PersonalBooster booster = getActiveBooster(playerId);
        return (booster != null) ? booster.getMultiplier() : 1.0;
    }

    public static boolean hasActiveBooster(UUID playerId) {
        return getActiveBooster(playerId) != null;
    }

    public static void clearBoosters(UUID playerId) {
        activeBoosters.remove(playerId);
    }

    public static void clearAllBoosters() {
        activeBoosters.clear();
    }
}
