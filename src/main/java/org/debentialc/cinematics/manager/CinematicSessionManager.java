package org.debentialc.cinematics.manager;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CinematicSessionManager {

    private static final Map<UUID, CinematicPlayer> activePlayers = new HashMap<>();

    public static void play(Player player, CinematicPlayer cinematicPlayer) {
        stop(player);
        activePlayers.put(player.getUniqueId(), cinematicPlayer);
        cinematicPlayer.play();
    }

    public static void stop(Player player) {
        CinematicPlayer cinematicPlayer = activePlayers.remove(player.getUniqueId());
        if (cinematicPlayer != null) {
            cinematicPlayer.stop();
        }
    }

    public static void removePlayer(Player player) {
        activePlayers.remove(player.getUniqueId());
    }

    public static boolean isPlaying(Player player) {
        return activePlayers.containsKey(player.getUniqueId());
    }

    public static CinematicPlayer getPlayer(Player player) {
        return activePlayers.get(player.getUniqueId());
    }
}
