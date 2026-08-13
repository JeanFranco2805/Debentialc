package org.debentialc.scoreboard.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.debentialc.scoreboard.config.ScoreboardConfig;
import org.debentialc.scoreboard.manager.ScoreboardManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ScoreboardListener implements Listener {

    private final ScoreboardManager manager;
    private final Set<String> ourSubcommands = new HashSet<>(Arrays.asList(
            "toggle", "on", "off", "reload", "help", "activar", "desactivar", "recargar", "ayuda"
    ));

    public ScoreboardListener(ScoreboardManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!ScoreboardConfig.isEnabled()) return;
        manager.createScoreboard(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        manager.removeScoreboard(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (!ScoreboardConfig.isEnabled()) return;
        manager.createScoreboard(event.getPlayer());
    }

    /**
     * Intercepta /scoreboard para los subcomandos propios sin anular el comando vanilla de Minecraft.
     * Los subcomandos que no sean de nuestro scoreboard (objectives, players, teams, etc.) pasan normal.
     */
    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (!message.toLowerCase().startsWith("/scoreboard")) return;

        String[] args = message.split(" ");
        String subcommand = args.length >= 2 ? args[1].toLowerCase() : "";

        // Sin argumentos o subcomando propio => lo redirigimos a nuestro comando /sb
        if (subcommand.isEmpty() || ourSubcommands.contains(subcommand)) {
            event.setCancelled(true);
            String redirect = "/sb" + (args.length >= 2 ? " " + join(args, 1) : "");
            final Player player = event.getPlayer();
            Bukkit.getScheduler().runTask(org.debentialc.Main.instance, new Runnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        Bukkit.dispatchCommand(player, redirect.substring(1));
                    }
                }
            });
        }
    }

    private String join(String[] args, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(args[i]);
        }
        return sb.toString();
    }
}
