package org.debentialc.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import noppes.npcs.api.entity.IDBCPlayer;
import noppes.npcs.scripted.NpcAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.debentialc.boosters.core.BoosterSettings;
import org.debentialc.boosters.managers.GlobalBoosterManager;
import org.debentialc.boosters.managers.PersonalBoosterManager;
import org.debentialc.boosters.models.PersonalBooster;
import org.debentialc.service.General;

public class DebentialcPlaceHolder extends PlaceholderExpansion {

    @Override
    public String getIdentifier() {
        return "debentialc";
    }

    @Override
    public String getAuthor() {
        return "Debentialc";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offline, String identifier) {
        if (offline == null) return "";
        Player player = offline.getPlayer();
        switch (identifier.toLowerCase()) {

            case "level":
                if (player == null) return "0";
                try {
                    IDBCPlayer dbcPlayer = NpcAPI.Instance().getPlayer(player.getName()).getDBCPlayer();
                    return String.valueOf(General.getLVL(player));
                } catch (Exception e) {
                    return "0";
                }

            case "tps":
                if (player == null) return "0";
                try {
                    IDBCPlayer dbcPlayer = NpcAPI.Instance().getPlayer(player.getName()).getDBCPlayer();
                    return String.valueOf(dbcPlayer.getTP());
                } catch (Exception e) {
                    return "0";
                }

            case "bank-balance":
                if (player == null) return "0";
                try {
                    IDBCPlayer dbcPlayer = NpcAPI.Instance().getPlayer(player.getName()).getDBCPlayer();
                    return String.valueOf(0);
                } catch (Exception e) {
                    return "0";
                }

            case "online":
                return String.valueOf(Bukkit.getOnlinePlayers().length);

            case "booster_global_multiplier":
            case "booster-global-multiplier":
                double globalMult = GlobalBoosterManager.getCurrentMultiplier();
                return String.format("%.2fx", globalMult);

            case "booster_global_remaining":
            case "booster-global-remaining":
                long seconds = GlobalBoosterManager.getRemainingSeconds();
                return seconds > 0 ? formatTime(seconds) : "Inactivo";

            case "booster_global_active":
            case "booster-global-active":
                return GlobalBoosterManager.isBoosterActive() ? "Activo" : "Inactivo";

            case "booster_global_percentage":
            case "booster-global-percentage":
                double mult = GlobalBoosterManager.getCurrentMultiplier();
                int percentage = (int) ((mult - 1.0) * 100);
                return percentage > 0 ? "+" + percentage + "%" : "0%";

            case "booster_personal_multiplier":
            case "booster-personal-multiplier":
                if (player == null) return "1.00x";
                double personalMult = PersonalBoosterManager.getActiveMultiplier(player.getUniqueId());
                return String.format("%.2fx", personalMult);

            case "booster_personal_remaining":
            case "booster-personal-remaining":
                if (player == null) return "Inactivo";
                PersonalBooster pb = PersonalBoosterManager.getActiveBooster(player.getUniqueId());
                if (pb != null) {
                    long remaining = pb.getActivationTimeRemaining(BoosterSettings.getPersonalBoosterDuration());
                    return remaining > 0 ? formatTime(remaining) : "Inactivo";
                }
                return "Inactivo";

            case "booster_personal_active":
            case "booster-personal-active":
                if (player == null) return "Inactivo";
                return PersonalBoosterManager.getActiveBooster(player.getUniqueId()) != null ? "Activo" : "Inactivo";

            case "booster_personal_level":
            case "booster-personal-level":
                if (player == null) return "Ninguno";
                PersonalBooster plevel = PersonalBoosterManager.getActiveBooster(player.getUniqueId());
                return plevel != null ? plevel.getLevelName() : "Ninguno";

            case "booster_combined_multiplier":
            case "booster-combined-multiplier":
                if (player == null) {
                    return String.format("%.2fx", GlobalBoosterManager.getCurrentMultiplier());
                }
                double combined = GlobalBoosterManager.getCurrentMultiplier() *
                        PersonalBoosterManager.getActiveMultiplier(player.getUniqueId());
                return String.format("%.2fx", combined);

            case "booster_combined_percentage":
            case "booster-combined-percentage":
                if (player == null) {
                    double m = GlobalBoosterManager.getCurrentMultiplier();
                    int p = (int) ((m - 1.0) * 100);
                    return p > 0 ? "+" + p + "%" : "0%";
                }
                double combinedMult = GlobalBoosterManager.getCurrentMultiplier() *
                        PersonalBoosterManager.getActiveMultiplier(player.getUniqueId());
                int combinedPct = (int) ((combinedMult - 1.0) * 100);
                return combinedPct > 0 ? "+" + combinedPct + "%" : "0%";

            case "booster_any_active":
            case "booster-any-active":
                if (player == null) {
                    return GlobalBoosterManager.isBoosterActive() ? "Activo" : "Inactivo";
                }
                boolean anyActive = GlobalBoosterManager.isBoosterActive() ||
                        PersonalBoosterManager.getActiveBooster(player.getUniqueId()) != null;
                return anyActive ? "Activo" : "Inactivo";
            case "race":
                return General.getRace(player);
            case "category":
                return General.getRankColorCode(player);
            default:
                return null;
        }
    }

    private String formatTime(long seconds) {
        if (seconds <= 0) {
            return "0s";
        }

        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, secs);
        } else {
            return String.format("%ds", secs);
        }
    }
}