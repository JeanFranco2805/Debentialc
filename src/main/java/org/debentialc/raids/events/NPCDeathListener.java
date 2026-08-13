package org.debentialc.raids.events;

import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.event.INpcEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.debentialc.Main;
import org.debentialc.raids.effects.RaidEffects;
import org.debentialc.raids.managers.NPCSpawnManager;
import org.debentialc.raids.managers.RaidSessionManager;
import org.debentialc.raids.managers.RaidSoundManager;
import org.debentialc.raids.managers.RaidTitleManager;
import org.debentialc.raids.models.RaidSession;
import org.debentialc.raids.models.SpawnPoint;
import org.debentialc.raids.models.Wave;
import org.debentialc.raids.models.WaveStatus;
import org.debentialc.service.CC;

import java.util.*;

public class NPCDeathListener implements Listener {

    private static final Set<String> rewardsGiven = new HashSet<>();
    private static final Set<String> activeCountdowns = new HashSet<>();
    private static final Map<String, Integer> countdownTasks = new HashMap<>();
    public void onNpcDie(INpcEvent.DiedEvent event) {
        try {


            int entityId = event.getNpc().getEntityId();
            String waveId = NPCSpawnManager.getWaveIdForNpc(entityId);

            if (waveId == null) {
                return;
            }

            Player killer = null;
            try {
                UUID killerUuid = UUID.fromString(event.getSource().getUniqueID());
                killer = Bukkit.getPlayer(killerUuid);
            } catch (Exception e) {
            }

            RaidSession session = getSessionByWaveId(waveId);
            if (session == null) {
                return;
            }

            handleNpcDeath(entityId, waveId, session, killer);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onNpcDamage(INpcEvent.DamagedEvent event) {
        try {
            int entityId = event.getNpc().getEntityId();
            Player attacker = null;
            IEntity source = event.getSource();
            if (source != null) {
                try {
                    UUID uuid = UUID.fromString(source.getUniqueID());
                    attacker = Bukkit.getPlayer(uuid);
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (!canDamageRaidNpc(entityId, attacker)) {
                if (attacker != null) {
                    attacker.sendMessage(CC.translate("&cNo puedes dañar a este NPC de raid."));
                }
                event.setDamage(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        try {
            int entityId = event.getEntity().getEntityId();
            org.bukkit.entity.Entity damager = event.getDamager();
            Player attacker = null;
            if (damager instanceof Player) {
                attacker = (Player) damager;
            } else if (damager instanceof Projectile) {
                Projectile projectile = (Projectile) damager;
                if (projectile.getShooter() instanceof Player) {
                    attacker = (Player) projectile.getShooter();
                }
            }
            if (!canDamageRaidNpc(entityId, attacker)) {
                event.setCancelled(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean canDamageRaidNpc(int entityId, Player attacker) {
        String waveId = NPCSpawnManager.getWaveIdForNpc(entityId);
        // Solo aplica a NPCs spawneados por el sistema de raids
        if (waveId == null) {
            return true;
        }

        RaidSession session = getSessionByWaveId(waveId);
        if (session == null) {
            // Raid ya no está activa: no permitir daño a NPCs huérfanos
            return false;
        }

        if (attacker == null) {
            return false;
        }

        return session.getActivePlayers().contains(attacker.getUniqueId());
    }

    private RaidSession getSessionByWaveId(String waveId) {
        String sessionId = waveId.substring(0, waveId.lastIndexOf("_wave_"));
        return RaidSessionManager.getSessionById(sessionId);
    }

    private void handleNpcDeath(int entityId, String waveId, RaidSession session, Player killer) {
        Wave wave = session.getCurrentWave();
        if (wave == null) {
            return;
        }

        boolean wasTracked = NPCSpawnManager.markNpcDead(entityId, waveId);
        if (!wasTracked) {
            return;
        }

        playDeathEffects(session, killer);
        int remaining = NPCSpawnManager.getAliveNpcsCount(waveId);

        if (remaining == 0) {
            completeWave(session, waveId);
        } else {
            updateWaveProgress(session, remaining);
        }
    }

    private void playDeathEffects(RaidSession session, Player killer) {
        for (UUID playerId : session.getActivePlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                RaidSoundManager.playDamageSound(player);
            }
        }

        if (killer != null) {
            String waveId = getWaveId(session);
            int remaining = NPCSpawnManager.getAliveNpcsCount(waveId);
            String message = String.format("§e⚔ §f+1 enemigo §7(%d restantes)", remaining);
            killer.sendMessage(message);
        }
    }

    private void completeWave(RaidSession session, String waveId) {
        int waveNumber = session.getCurrentWaveIndex() + 1;
        int totalWaves = session.getRaid().getTotalWaves();

        // Guard: prevent processing the same wave completion twice
        if (rewardsGiven.contains(waveId)) {
            return;
        }
        rewardsGiven.add(waveId);

        List<Player> players = getActivePlayers(session);
        boolean isLastWave = !session.hasNextWave();

        // --- Sounds only (no messages from effects/title managers) ---
        for (Player player : players) {
            RaidSoundManager.playWaveCompleteSound(player);
        }

        // Visual effect at player locations (no messages inside RaidEffects)
        for (Player player : players) {
            player.getLocation().getWorld().playEffect(
                    player.getLocation(), org.bukkit.Effect.MOBSPAWNER_FLAMES, 0);
        }

        // Execute rewards
        executeWaveRewardsOnce(session);

        NPCSpawnManager.clearWaveTracking(waveId);

        if (isLastWave) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(Main.instance, () -> {
                completeRaidWithEffects(session);
                rewardsGiven.remove(waveId);
            }, 60L);
        } else {
            startWaveCountdown(session, waveId);
        }
    }

    private void startWaveCountdown(RaidSession session, String previousWaveId) {
        String countdownKey = session.getSessionId() + "_countdown";

        if (activeCountdowns.contains(countdownKey)) {
            return;
        }
        activeCountdowns.add(countdownKey);

        List<Player> players = getActivePlayers(session);
        int nextWaveNumber = session.getCurrentWaveIndex() + 2;

        for (Player player : players) {
            player.sendMessage("");
            player.sendMessage("§6⏳ §fPreparándose para oleada §6" + nextWaveNumber + "§f...");
            player.sendMessage("");
        }

        final int[] countdown = {10};
        final boolean[] hasStartedWave = {false};

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(Main.instance, new Runnable() {
            @Override
            public void run() {
                if (hasStartedWave[0]) {
                    return;
                }

                List<Player> currentPlayers = getActivePlayers(session);

                if (countdown[0] > 0) {
                    String countdownMsg = getCountdownMessage(countdown[0]);

                    for (Player player : currentPlayers) {
                        player.sendMessage(countdownMsg);

                        if (countdown[0] <= 5) {
                            RaidSoundManager.playAlertSound(player);
                        }
                    }

                    countdown[0]--;
                } else {
                    hasStartedWave[0] = true;

                    for (Player player : currentPlayers) {
                        player.sendMessage("");
                        player.sendMessage("§c§l⚔ ¡OLEADA " + nextWaveNumber + " INICIADA! ⚔");
                        player.sendMessage("");
                        RaidSoundManager.playWaveStartSound(player);
                    }

                    session.moveToNextWave();
                    Wave nextWave = session.getCurrentWave();

                    if (nextWave != null) {
                        for (SpawnPoint sp : nextWave.getSpawnPoints()) {
                            sp.resetAliveCount();
                        }

                        nextWave.setStatus(WaveStatus.ACTIVE);

                        String newWaveId = getWaveId(session);
                        boolean spawned = NPCSpawnManager.spawnWaveNpcs(nextWave, newWaveId);

                        if (spawned) {
                            // Sounds only - no duplicate message block
                            for (Player player : currentPlayers) {
                                RaidSoundManager.playWaveStartSound(player);
                            }
                        }
                    }

                    activeCountdowns.remove(countdownKey);
                    rewardsGiven.remove(previousWaveId);

                    Integer storedTaskId = countdownTasks.remove(countdownKey);
                    if (storedTaskId != null) {
                        Bukkit.getScheduler().cancelTask(storedTaskId);
                    }
                }
            }
        }, 20L, 20L);

        countdownTasks.put(countdownKey, taskId);
    }

    private String getCountdownMessage(int seconds) {
        if (seconds > 5) {
            return String.format("§e⏳ §fSiguiente oleada en §e%d §fsegundos...", seconds);
        } else if (seconds > 1) {
            return String.format("§6⏳ §f¡Oleada en §6%d§f!", seconds);
        } else {
            return "§c⏳ §f¡§c1§f!";
        }
    }

    private void completeRaidWithEffects(RaidSession session) {
        String raidName = session.getRaid().getDisplayName();
        List<Player> players = getActivePlayers(session);

        // Sounds only
        for (Player player : players) {
            RaidSoundManager.playVictorySound(player);
        }

        long duration = session.getDurationSeconds();
        long minutes = duration / 60;
        long seconds = duration % 60;

        // Single clean centered victory message
        String survivorText = players.size() == 1 ? "sobreviviente" : "sobrevivientes";
        String border = repeatString("▬", 53);
        String line1 = center("&6&l✦ RAID COMPLETADA ✦", true);
        String line2 = center("&7Raid: &f" + raidName);
        String line3 = center(String.format("&7Oleadas: &f%d/%d &7completadas", session.getRaid().getTotalWaves(), session.getRaid().getTotalWaves()));
        String line4 = center(String.format("&7Tiempo: &f%dm %ds", minutes, seconds));
        String line5 = center(String.format("&7Jugadores: &f%d &7%s", players.size(), survivorText));
        for (Player player : players) {
            player.sendMessage("");
            player.sendMessage(CC.translate("&8&l" + border));
            player.sendMessage(CC.translate(line1));
            player.sendMessage(CC.translate(line2));
            player.sendMessage(CC.translate(line3));
            player.sendMessage(CC.translate(line4));
            player.sendMessage(CC.translate(line5));
            player.sendMessage(CC.translate("&8&l" + border));
            player.sendMessage("");
        }

        Bukkit.getScheduler().scheduleSyncDelayedTask(Main.instance, () -> {
            for (Player player : players) {
                player.getWorld().playEffect(player.getLocation(), org.bukkit.Effect.MOBSPAWNER_FLAMES, 0);
                RaidSoundManager.playBuffSound(player);
            }
        }, 40L);

        RaidSessionManager.completeRaid(session);
    }

    private void updateWaveProgress(RaidSession session, int enemiesRemaining) {
        Wave wave = session.getCurrentWave();

        if (wave == null) {
            return;
        }

        int totalEnemies = wave.getTotalEnemies();
        int enemiesKilled = totalEnemies - enemiesRemaining;
        int waveNumber = session.getCurrentWaveIndex() + 1;
        int totalWaves = session.getRaid().getTotalWaves();

        int waveProgress = (enemiesKilled * 100) / Math.max(1, totalEnemies);
        String progressBar = createProgressBar(waveProgress, 20);

        for (UUID playerId : session.getActivePlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                String progressMessage = String.format(
                        "§6Oleada %d/%d §8| §7Enemigos: %s §f%d/%d",
                        waveNumber, totalWaves, progressBar, enemiesKilled, totalEnemies
                );

                player.sendMessage(progressMessage);

                if (enemiesRemaining <= 5 && enemiesRemaining > 0) {
                    if (enemiesRemaining == 5 || enemiesRemaining == 3 || enemiesRemaining == 1) {
                        player.sendMessage(String.format("§c⚠ §f¡Solo quedan §c%d §fenemigos!", enemiesRemaining));
                        RaidSoundManager.playAttackSound(player);
                    }
                }
            }
        }
    }

    private void executeWaveRewardsOnce(RaidSession session) {
        Wave wave = session.getCurrentWave();
        if (wave == null || !wave.hasRewards()) {
            return;
        }

        for (org.debentialc.raids.models.WaveReward reward : wave.getRewards()) {
            boolean shouldGive = reward.shouldExecute();

            if (shouldGive) {
                for (UUID playerId : session.getActivePlayers()) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null) {
                        String command = reward.getCommand()
                                .replace("@p", player.getName())
                                .replace("@s", player.getName())
                                .replace("{player}", player.getName());

                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

                        RaidSoundManager.playBuffSound(player);
                    }
                }
            }
        }
    }

    private String getRewardDisplayName(String command) {
        if (command.length() > 40) {
            return command.substring(0, 37) + "...";
        }

        if (command.startsWith("give")) {
            return "§a✓ §fRecompensa otorgada";
        } else if (command.startsWith("eco") || command.startsWith("money")) {
            return "§6✓ §fDinero otorgado";
        } else if (command.startsWith("xp")) {
            return "§b✓ §fExperiencia otorgada";
        }

        return "§e✓ §f/" + command;
    }

    private List<Player> getActivePlayers(RaidSession session) {
        List<Player> players = new ArrayList<>();
        for (UUID playerId : session.getActivePlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                players.add(player);
            }
        }
        return players;
    }

    private String createProgressBar(int percentage, int length) {
        int filled = (percentage * length) / 100;

        StringBuilder bar = new StringBuilder();
        bar.append("§a");
        for (int i = 0; i < filled; i++) {
            bar.append("█");
        }

        bar.append("§7");
        for (int i = filled; i < length; i++) {
            bar.append("█");
        }

        return bar.toString();
    }

    private String repeatString(String str, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    private String center(String text) {
        return center(text, false);
    }

    private String center(String text, boolean bold) {
        int chatWidth = 300;
        int spaceWidth = 4;
        int textWidth = pixelWidth(text, bold);
        int spaces = Math.max(0, (chatWidth - textWidth) / (2 * spaceWidth));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < spaces; i++) {
            sb.append(' ');
        }
        sb.append(text);
        return sb.toString();
    }

    private int pixelWidth(String text) {
        return pixelWidth(text, false);
    }

    private int pixelWidth(String text, boolean bold) {
        int width = 0;
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            // Saltar códigos de color § o & seguidos de un código de formato
            if (c == '§' || c == '&') {
                if (i + 1 < chars.length) {
                    i++;
                }
                continue;
            }
            if (c >= 'A' && c <= 'Z') {
                width += 6;
            } else if (c >= 'a' && c <= 'z') {
                width += 5;
            } else if (c >= '0' && c <= '9') {
                width += 6;
            } else if (c == ' ') {
                width += 4;
            } else if (c == ':' || c == '/' || c == '.' || c == '-') {
                width += 4;
            } else if (c == '✦' || c == '★' || c == '✓' || c == '✗') {
                width += 6;
            } else {
                width += 5;
            }
            if (bold) {
                width += 1;
            }
        }
        return width;
    }

    private String getWaveId(RaidSession session) {
        return session.getSessionId() + "_wave_" + session.getCurrentWaveIndex();
    }

    public static void clearSessionTracking(String sessionId) {
        rewardsGiven.removeIf(id -> id.startsWith(sessionId));
        activeCountdowns.removeIf(id -> id.startsWith(sessionId));

        List<String> keysToRemove = new ArrayList<>();
        for (String key : countdownTasks.keySet()) {
            if (key.startsWith(sessionId)) {
                Integer taskId = countdownTasks.get(key);
                if (taskId != null) {
                    Bukkit.getScheduler().cancelTask(taskId);
                }
                keysToRemove.add(key);
            }
        }
        for (String key : keysToRemove) {
            countdownTasks.remove(key);
        }
    }
}