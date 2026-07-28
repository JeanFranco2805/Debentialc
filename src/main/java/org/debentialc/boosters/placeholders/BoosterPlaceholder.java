package org.debentialc.boosters.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.debentialc.boosters.core.BoosterSettings;
import org.debentialc.boosters.managers.GlobalBoosterManager;
import org.debentialc.boosters.managers.PersonalBoosterManager;
import org.debentialc.boosters.models.PersonalBooster;

/**
 * Expansión de PlaceholderAPI para el sistema de boosters
 * VERSIÓN CORREGIDA: Implementación completa y correcta
 */
public class BoosterPlaceholder extends PlaceholderExpansion {

    @Override
    public String getIdentifier() {
        return "boosters";
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
    public String onPlaceholderRequest(Player player,  String identifier) {
        if (player == null && requiresPlayer(identifier)) {
            return "";
        }

        switch (identifier.toLowerCase()) {
            case "global_active":
                return GlobalBoosterManager.isBoosterActive() ? "§aActivo" : "§cInactivo";

            case "global_multiplier":
                double globalMult = (GlobalBoosterManager.getCurrentMultiplier() == 1.0) ? 0 : GlobalBoosterManager.getCurrentMultiplier();
                return String.format("§6%.2fx", globalMult);

            case "global_percentage":
                double mult = GlobalBoosterManager.getCurrentMultiplier();
                int percentage = (int) ((mult - 1.0) * 100);
                return percentage > 0 ? "§a+" + percentage + "%" : "§70%";

            case "global_time":
                long seconds = GlobalBoosterManager.getRemainingSeconds();
                if (seconds <= 0) return "§cExpirado";
                return formatTime(seconds);

            case "global_activator":
                org.debentialc.boosters.models.GlobalBooster gb = GlobalBoosterManager.getActiveBooster();
                return gb != null ? "§6" + gb.getActivatedBy() : "§7Ninguno";

            case "personal_active":
                if (player == null) return "";
                PersonalBooster personal = PersonalBoosterManager.getActiveBooster(player.getUniqueId());
                return personal != null ? "§aActivo" : "§cInactivo";

            case "personal_multiplier":
                if (player == null) return "§61.00x";
                PersonalBooster pb = PersonalBoosterManager.getActiveBooster(player.getUniqueId());
                return pb != null ? String.format("§6%.2fx", pb.getMultiplier()) : "§61.00x";

            case "personal_percentage":
                if (player == null) return "§70%";
                PersonalBooster pboost = PersonalBoosterManager.getActiveBooster(player.getUniqueId());
                if (pboost != null) {
                    int pct = pboost.getPercentageBonus();
                    return pct > 0 ? "§a+" + pct + "%" : "§70%";
                }
                return "§70%";

            case "personal_level":
                if (player == null) return "§7Ninguno";
                PersonalBooster plevel = PersonalBoosterManager.getActiveBooster(player.getUniqueId());
                return plevel != null ? "§6" + plevel.getLevelName() : "§7Ninguno";

            case "personal_time":
                if (player == null) return "§cInactivo";
                PersonalBooster ptime = PersonalBoosterManager.getActiveBooster(player.getUniqueId());
                if (ptime != null) {
                    long remaining = ptime.getActivationTimeRemaining();
                    if (remaining < 0) return "§6∞";
                    return remaining > 0 ? formatTime(remaining) : "§cExpirado";
                }
                return "§cInactivo";

            case "personal_count":
                if (player == null) return "§60";
                int count = PersonalBoosterManager.hasActiveBooster(player.getUniqueId()) ? 1 : 0;
                return "§6" + count;

            case "combined_multiplier":
                if (player == null) return "§61.00x";
                double combined = GlobalBoosterManager.getCurrentMultiplier() *
                        PersonalBoosterManager.getActiveMultiplier(player.getUniqueId());
                return String.format("§6%.2fx", combined);

            case "combined_percentage":
                if (player == null) return "§70%";
                double combinedMult = GlobalBoosterManager.getCurrentMultiplier() *
                        PersonalBoosterManager.getActiveMultiplier(player.getUniqueId());
                int combinedPct = (int) ((combinedMult - 1.0) * 100);
                return combinedPct > 0 ? "§a+" + combinedPct + "%" : "§70%";

            case "any_active":
                if (player == null) {
                    return GlobalBoosterManager.isBoosterActive() ? "§aActivo" : "§cInactivo";
                }
                boolean anyActive = GlobalBoosterManager.isBoosterActive() ||
                        PersonalBoosterManager.getActiveBooster(player.getUniqueId()) != null;
                return anyActive ? "§aActivo" : "§cInactivo";

            default:
                return null;
        }
    }

    /**
     * Verifica si un placeholder requiere un jugador
     */
    private boolean requiresPlayer(String identifier) {
        String lower = identifier.toLowerCase();
        return lower.startsWith("personal_") ||
                lower.equals("combined_multiplier") ||
                lower.equals("combined_percentage") ||
                lower.equals("any_active");
    }

    /**
     * Formatea tiempo en segundos a formato legible
     */
    private String formatTime(long seconds) {
        if (seconds <= 0) {
            return "§c0s";
        }

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
}