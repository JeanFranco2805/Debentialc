package org.debentialc.scoreboard.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.debentialc.scoreboard.animation.ScoreboardAnimation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ScoreboardConfig {

    private static boolean enabled = true;
    private static int updateInterval = 20;
    private static ScoreboardAnimation titleAnimation = new ScoreboardAnimation("&6&lDebentialc", ScoreboardAnimation.Type.STATIC, 20, 26);
    private static List<ScoreboardLine> lines = new ArrayList<>();

    private static File configFile;
    private static FileConfiguration config;

    public static void load(JavaPlugin plugin) {
        configFile = new File(plugin.getDataFolder(), "scoreboard.yml");
        if (!configFile.exists()) {
            plugin.saveResource("scoreboard.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        enabled = config.getBoolean("enabled", true);
        updateInterval = config.getInt("update-interval", 20);
        if (updateInterval < 1) updateInterval = 1;

        titleAnimation = loadAnimation(config.getConfigurationSection("title"), "&6&lDebentialc");

        lines.clear();
        List<?> rawLines = config.getList("lines");
        if (rawLines != null) {
            for (Object obj : rawLines) {
                if (obj instanceof String) {
                    lines.add(new ScoreboardLine((String) obj));
                } else if (obj instanceof ConfigurationSection || obj instanceof java.util.Map) {
                    ConfigurationSection section;
                    if (obj instanceof ConfigurationSection) {
                        section = (ConfigurationSection) obj;
                    } else {
                        section = config.createSection("temp");
                        for (java.util.Map.Entry<?, ?> entry : ((java.util.Map<?, ?>) obj).entrySet()) {
                            section.set(entry.getKey().toString(), entry.getValue());
                        }
                    }
                    ConfigurationSection animSection = section.getConfigurationSection("animation");
                    if (animSection != null) {
                        lines.add(new ScoreboardLine(loadAnimation(animSection, "")));
                    } else {
                        String text = section.getString("text", "");
                        lines.add(new ScoreboardLine(text));
                    }
                }
            }
        }

        if (lines.isEmpty()) {
            lines.add(new ScoreboardLine("&7Scoreboard cargado"));
        }
    }

    private static ScoreboardAnimation loadAnimation(ConfigurationSection section, String defaultText) {
        if (section == null) {
            return new ScoreboardAnimation(defaultText, ScoreboardAnimation.Type.STATIC, 20, 26);
        }
        String typeName = section.getString("type", "static").toUpperCase();
        ScoreboardAnimation.Type type;
        try {
            type = ScoreboardAnimation.Type.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            type = ScoreboardAnimation.Type.STATIC;
        }
        int interval = section.getInt("interval", 20);
        int width = section.getInt("width", 26);
        String text = section.getString("text", defaultText);
        List<String> frames = section.getStringList("frames");
        return new ScoreboardAnimation(text, type, interval, width, frames);
    }

    public static void reload(JavaPlugin plugin) {
        load(plugin);
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static int getUpdateInterval() {
        return updateInterval;
    }

    public static ScoreboardAnimation getTitleAnimation() {
        return titleAnimation;
    }

    public static List<ScoreboardLine> getLines() {
        return new ArrayList<>(lines);
    }

    public static FileConfiguration getConfig() {
        return config;
    }

    public static File getConfigFile() {
        return configFile;
    }

    public static class ScoreboardLine {
        private final String text;
        private final ScoreboardAnimation animation;

        public ScoreboardLine(String text) {
            this.text = text;
            this.animation = null;
        }

        public ScoreboardLine(ScoreboardAnimation animation) {
            this.text = null;
            this.animation = animation;
        }

        public boolean isAnimated() {
            return animation != null;
        }

        public String getText() {
            return text;
        }

        public ScoreboardAnimation getAnimation() {
            return animation;
        }
    }
}
