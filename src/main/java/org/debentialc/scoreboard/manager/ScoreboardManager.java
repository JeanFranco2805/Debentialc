package org.debentialc.scoreboard.manager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.debentialc.scoreboard.PlaceholderParser;
import org.debentialc.scoreboard.animation.ScoreboardAnimation;
import org.debentialc.scoreboard.config.ScoreboardConfig;
import org.debentialc.scoreboard.config.ScoreboardConfig.ScoreboardLine;
import org.debentialc.service.CC;
import org.debentialc.service.ServerUtil;

import java.util.*;

public class ScoreboardManager {

    private static final int MAX_LINES = 15;
    private static final int MAX_TITLE = 32;
    private static final int MAX_PREFIX = 16;
    private static final int MAX_SUFFIX = 16;
    private static final String[] ENTRIES = {
            "§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7",
            "§8", "§9", "§a", "§b", "§c", "§d", "§e"
    };

    private final Map<UUID, PlayerScoreboard> playerBoards = new HashMap<>();
    private final List<ScoreboardAnimation> activeAnimations = new ArrayList<>();
    private int taskId = -1;

    public void start() {
        stop();
        activeAnimations.clear();
        activeAnimations.add(ScoreboardConfig.getTitleAnimation());
        for (ScoreboardLine line : ScoreboardConfig.getLines()) {
            if (line.isAnimated()) {
                activeAnimations.add(line.getAnimation());
            }
        }

        int interval = ScoreboardConfig.getUpdateInterval();
        taskId = Bukkit.getScheduler().runTaskTimer(org.debentialc.Main.instance, new Runnable() {
            @Override
            public void run() {
                tickAnimations();
                updateAll();
            }
        }, 0L, interval).getTaskId();
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        for (PlayerScoreboard ps : new ArrayList<>(playerBoards.values())) {
            if (ps.player.isOnline()) {
                ps.player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        }
        playerBoards.clear();
    }

    public boolean isRunning() {
        return taskId != -1;
    }

    public void reload() {
        stop();
        ScoreboardConfig.load(org.debentialc.Main.instance);
        if (ScoreboardConfig.isEnabled()) {
            start();
            for (Player player : ServerUtil.getOnlinePlayers()) {
                createScoreboard(player);
            }
        }
    }

    public void createScoreboard(Player player) {
        if (!ScoreboardConfig.isEnabled()) return;
        removeScoreboard(player);
        PlayerScoreboard ps = new PlayerScoreboard(player);
        playerBoards.put(player.getUniqueId(), ps);
        ps.update();
    }

    public void removeScoreboard(Player player) {
        PlayerScoreboard ps = playerBoards.remove(player.getUniqueId());
        if (ps != null && player.isOnline()) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    private void tickAnimations() {
        for (ScoreboardAnimation animation : activeAnimations) {
            animation.tick();
        }
    }

    private void updateAll() {
        for (PlayerScoreboard ps : new ArrayList<>(playerBoards.values())) {
            ps.update();
        }
    }

    private class PlayerScoreboard {
        private final Player player;
        private final Scoreboard scoreboard;
        private final Objective objective;
        private final Team[] teams;
        private final String[] entries;

        PlayerScoreboard(Player player) {
            this.player = player;
            this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            this.objective = scoreboard.registerNewObjective("deb_sb", "dummy");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);

            String initialTitle = PlaceholderParser.parse(ScoreboardConfig.getTitleAnimation().getCurrentFrame(), player);
            if (initialTitle == null || initialTitle.isEmpty()) {
                initialTitle = "Scoreboard";
            }
            objective.setDisplayName(truncate(initialTitle, MAX_TITLE));

            this.teams = new Team[MAX_LINES];
            this.entries = new String[MAX_LINES];
            for (int i = 0; i < MAX_LINES; i++) {
                Team team = scoreboard.registerNewTeam("deb_line_" + i);
                String entry = ENTRIES[i];
                try {
                    team.addPlayer(Bukkit.getOfflinePlayer(entry));
                } catch (Exception e) {
                    // Fallback: use a regular unique string if color-code entry fails
                    entry = "DEB" + i;
                    team.addPlayer(Bukkit.getOfflinePlayer(entry));
                }
                teams[i] = team;
                entries[i] = entry;
                objective.getScore(entry).setScore(MAX_LINES - i);
            }
            player.setScoreboard(scoreboard);
        }

        void update() {
            if (!player.isOnline()) return;

            String title = PlaceholderParser.parse(ScoreboardConfig.getTitleAnimation().getCurrentFrame(), player);
            if (title == null || title.isEmpty()) {
                title = "Scoreboard";
            }
            objective.setDisplayName(truncate(title, MAX_TITLE));

            List<ScoreboardLine> lines = ScoreboardConfig.getLines();
            int lineCount = Math.min(lines.size(), MAX_LINES);
            for (int i = 0; i < lineCount; i++) {
                ScoreboardLine line = lines.get(i);
                String text;
                if (line.isAnimated()) {
                    text = line.getAnimation().getCurrentFrame();
                } else {
                    text = line.getText();
                }
                text = PlaceholderParser.parse(text, player);
                setLine(i, text);
            }
            for (int i = lineCount; i < MAX_LINES; i++) {
                setLine(i, "");
            }
        }

        private void setLine(int index, String text) {
            String colored = CC.translate(text);
            Team team = teams[index];
            if (colored == null || colored.isEmpty()) {
                team.setPrefix(" ");
                team.setSuffix("");
                return;
            }

            String[] split = splitForScoreboard(colored);
            team.setPrefix(split[0]);
            team.setSuffix(split[1]);
        }
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength);
    }

    private static String[] splitForScoreboard(String text) {
        if (text.length() <= MAX_PREFIX) {
            return new String[]{text, ""};
        }
        String prefix = text.substring(0, MAX_PREFIX);
        String suffix = text.substring(MAX_PREFIX);
        if (suffix.length() > MAX_SUFFIX) {
            suffix = suffix.substring(0, MAX_SUFFIX);
        }
        // Carry last color from prefix to suffix
        String lastColor = getLastColor(prefix);
        if (lastColor != null && !lastColor.isEmpty()) {
            suffix = lastColor + suffix;
        }
        return new String[]{prefix, suffix};
    }

    private static String getLastColor(String text) {
        if (text == null || text.isEmpty()) return "";
        int len = text.length();
        StringBuilder result = new StringBuilder();
        for (int i = len - 1; i >= 0; i--) {
            char c = text.charAt(i);
            if (c == ChatColor.COLOR_CHAR && i + 1 < len) {
                char code = text.charAt(i + 1);
                ChatColor color = ChatColor.getByChar(code);
                if (color != null) {
                    result.insert(0, color.toString());
                    if (isColor(color)) {
                        return result.toString();
                    }
                }
            }
        }
        return result.toString();
    }

    private static boolean isColor(ChatColor color) {
        return color != ChatColor.MAGIC && color != ChatColor.BOLD && color != ChatColor.STRIKETHROUGH
                && color != ChatColor.UNDERLINE && color != ChatColor.ITALIC && color != ChatColor.RESET;
    }
}
