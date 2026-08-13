package org.debentialc.scoreboard;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.debentialc.placeholder.DebentialcPlaceHolder;

public class PlaceholderParser {

    private static DebentialcPlaceHolder debentialcExpansion;

    static {
        debentialcExpansion = new DebentialcPlaceHolder();
    }

    public static String parse(String text, Player player) {
        if (text == null || text.isEmpty()) return "";
        String result = text;

        // Built-in player placeholders
        if (player != null) {
            result = result.replace("%player_name%", player.getName());
            result = result.replace("%player_displayname%", player.getDisplayName());
            result = result.replace("%player_uuid%", player.getUniqueId().toString());
            result = result.replace("%player_world%", player.getWorld().getName());
        } else {
            result = result.replace("%player_name%", "Jugador");
            result = result.replace("%player_displayname%", "Jugador");
            result = result.replace("%player_uuid%", "");
            result = result.replace("%player_world%", "");
        }

        // Debentialc placeholders
        result = parseDebentialc(result, player);

        // PlaceholderAPI fallback for any other placeholders
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null && player != null) {
            try {
                result = PlaceholderAPI.setPlaceholders(player, result);
            } catch (Exception ignored) {
            }
        }

        return result;
    }

    private static String parseDebentialc(String text, Player player) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            int start = text.indexOf('%', i);
            if (start == -1) {
                result.append(text.substring(i));
                break;
            }
            result.append(text.substring(i, start));
            int end = text.indexOf('%', start + 1);
            if (end == -1) {
                result.append(text.substring(start));
                break;
            }
            String placeholder = text.substring(start + 1, end);
            String replacement = placeholder;
            if (placeholder.toLowerCase().startsWith("debentialc_")) {
                String identifier = placeholder.substring("debentialc_".length());
                String value = debentialcExpansion.onRequest(player, identifier);
                replacement = value != null ? value : "";
            }
            result.append(replacement);
            i = end + 1;
        }
        return result.toString();
    }
}
