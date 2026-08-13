package org.debentialc.service;
import org.bukkit.ChatColor;
import java.util.List;
import java.util.stream.Collectors;

public class CC {
    public static String translate(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static List<String> translate(List<String> list) {
        return list.stream().map(CC::translate).collect(Collectors.toList());
    }

    public static String safeTitle(String title) {
        return safeTitle(title, 32);
    }

    public static String safeTitle(String title, int max) {
        if (title == null) return "";
        if (title.length() <= max) return translate(title);
        String cut = title.substring(0, max);
        // Evitar cortar a la mitad un código de color (&X)
        if (cut.endsWith("&") && title.length() > max) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return translate(cut);
    }
}
