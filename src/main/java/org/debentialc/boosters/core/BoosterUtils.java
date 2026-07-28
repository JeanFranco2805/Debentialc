package org.debentialc.boosters.core;

import org.debentialc.boosters.managers.GlobalBoosterManager;
import org.debentialc.boosters.managers.PersonalBoosterManager;
import java.util.UUID;

public class BoosterUtils {

    public static double calculateCombinedMultiplier(UUID playerId) {
        double global = GlobalBoosterManager.getCurrentMultiplier();
        double personal = PersonalBoosterManager.getActiveMultiplier(playerId);
        return global * personal;
    }

    public static String formatMultiplier(double multiplier) {
        return String.format("%.2fx", multiplier);
    }

    public static String formatPercentage(double multiplier) {
        int percentage = (int) ((multiplier - 1.0) * 100);
        if (percentage > 0) {
            return "§a+" + percentage + "%";
        } else if (percentage < 0) {
            return "§c" + percentage + "%";
        } else {
            return "§60%";
        }
    }

    public static String formatTime(long seconds) {
        if (seconds <= 0) return "§cExpired";

        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format("§6%dh %dm %ds", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("§6%dm %ds", minutes, secs);
        } else {
            return String.format("§6%ds", secs);
        }
    }

    public static int getPercentageBonus(double multiplier) {
        return (int) ((multiplier - 1.0) * 100);
    }

    public static boolean isMultiplierValid(double multiplier) {
        return multiplier > 0 && multiplier < 1000;
    }

    public static void cleanupExpiredBoosters(UUID playerId) {
        PersonalBoosterManager.getActiveMultiplier(playerId);
    }

    public static void cleanupAllExpiredBoosters() {
        GlobalBoosterManager.checkExpiration();
    }
}