package org.debentialc.raids.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CooldownManager - Gestor de cooldowns con persistencia
 * Los cooldowns se guardan y cargan automáticamente
 */
public class CooldownManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String COOLDOWNS_FILE = "plugins/Debentialc/data/raids/cooldowns.json";

    // Estructura: player_uuid -> (raid_id -> cooldown_end_time)
    private static final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    private static String normalizeRaidId(String raidId) {
        return raidId != null ? raidId.toLowerCase() : null;
    }

    /**
     * Establece un cooldown para un jugador en una raid
     */
    public static void setCooldown(UUID playerId, String raidId, long durationSeconds) {
        if (playerId == null || raidId == null) {
            return;
        }
        long endTime = System.currentTimeMillis() + (durationSeconds * 1000);
        String key = normalizeRaidId(raidId);

        cooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .put(key, endTime);

        System.out.println("[Raids] Cooldown establecido: " + playerId + " - " + key +
                " - Duración: " + durationSeconds + "s");

        // Auto-guardar después de establecer cooldown
        saveCooldowns();
    }

    /**
     * Verifica si un jugador tiene cooldown en una raid
     */
    public static boolean hasCooldown(UUID playerId, String raidId) {
        if (playerId == null || raidId == null) {
            return false;
        }
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) {
            return false;
        }

        String key = normalizeRaidId(raidId);
        Object endTimeObj = playerCooldowns.get(key);
        if (!(endTimeObj instanceof Number)) {
            return false;
        }
        long endTime = ((Number) endTimeObj).longValue();

        // Verificar si el cooldown aún está activo
        if (System.currentTimeMillis() >= endTime) {
            // Cooldown expiró, removerlo
            playerCooldowns.remove(key);
            saveCooldowns();
            return false;
        }

        return true;
    }

    /**
     * Obtiene los segundos restantes de un cooldown
     */
    public static long getCooldownRemaining(UUID playerId, String raidId) {
        if (playerId == null || raidId == null) {
            return 0;
        }
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) {
            return 0;
        }

        String key = normalizeRaidId(raidId);
        Object endTimeObj = playerCooldowns.get(key);
        if (!(endTimeObj instanceof Number)) {
            return 0;
        }
        long endTime = ((Number) endTimeObj).longValue();

        long remaining = (endTime - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    /**
     * Obtiene el cooldown formateado en texto legible
     */
    public static String getCooldownFormattedTime(UUID playerId, String raidId) {
        long remaining = getCooldownRemaining(playerId, raidId);

        if (remaining <= 0) {
            return "No hay cooldown";
        }

        long hours = remaining / 3600;
        long minutes = (remaining % 3600) / 60;
        long seconds = remaining % 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    /**
     * Limpia un cooldown específico
     */
    public static void clearCooldown(UUID playerId, String raidId) {
        if (playerId == null || raidId == null) {
            return;
        }
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns != null) {
            playerCooldowns.remove(normalizeRaidId(raidId));
            saveCooldowns();
            System.out.println("[Raids] Cooldown removido: " + playerId + " - " + raidId);
        }
    }

    /**
     * Limpia todos los cooldowns de un jugador
     */
    public static void clearAllPlayerCooldowns(UUID playerId) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns != null) {
            playerCooldowns.clear();
            saveCooldowns();
            System.out.println("[Raids] Todos los cooldowns de " + playerId + " han sido removidos");
        }
    }

    /**
     * Limpia todos los cooldowns
     */
    public static void clearAllCooldowns() {
        cooldowns.clear();
        saveCooldowns();
        System.out.println("[Raids] Todos los cooldowns han sido removidos");
    }

    /**
     * Obtiene todos los cooldowns activos de un jugador
     */
    public static Map<String, Long> getPlayerCooldowns(UUID playerId) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) {
            return new HashMap<>();
        }

        // Crear una copia y remover los expirados
        Map<String, Long> active = new HashMap<>();
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : playerCooldowns.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Number && now < ((Number) value).longValue()) {
                active.put(entry.getKey(), ((Number) value).longValue());
            }
        }

        return active;
    }

    /**
     * Guarda todos los cooldowns en archivo JSON
     */
    public static void saveCooldowns() {
        try {
            File file = new File(COOLDOWNS_FILE);
            file.getParentFile().mkdirs();

            // Convertir UUID a String para serialización
            Map<String, Map<String, Long>> serializable = new HashMap<>();
            for (Map.Entry<UUID, Map<String, Long>> entry : cooldowns.entrySet()) {
                serializable.put(entry.getKey().toString(), entry.getValue());
            }

            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(serializable, writer);
            }

            System.out.println("[Raids] Cooldowns guardados: " + cooldowns.size() + " jugadores");
        } catch (Exception e) {
            System.err.println("[Raids] Error al guardar cooldowns: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Carga todos los cooldowns desde archivo JSON
     */
    public static void loadCooldowns() {
        try {
            File file = new File(COOLDOWNS_FILE);
            if (!file.exists()) {
                System.out.println("[Raids] No hay archivo de cooldowns previo");
                return;
            }

            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<String, Map<String, Long>>>(){}.getType();
                Map<String, Map<String, Long>> loaded = GSON.fromJson(reader, type);

                if (loaded != null) {
                    cooldowns.clear();
                    // Convertir String a UUID
                    for (Map.Entry<String, Map<String, Long>> entry : loaded.entrySet()) {
                        try {
                            UUID playerId = UUID.fromString(entry.getKey());
                            cooldowns.put(playerId, new ConcurrentHashMap<>(entry.getValue()));
                        } catch (IllegalArgumentException e) {
                            System.err.println("[Raids] UUID inválido en cooldowns: " + entry.getKey());
                        }
                    }
                }
            }

            // Limpiar cooldowns expirados
            cleanupExpiredCooldowns();

            System.out.println("[Raids] Cooldowns cargados: " + cooldowns.size() + " jugadores");
        } catch (Exception e) {
            System.err.println("[Raids] Error al cargar cooldowns: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Limpia cooldowns expirados
     */
    private static void cleanupExpiredCooldowns() {
        long now = System.currentTimeMillis();
        AtomicInteger removed = new AtomicInteger();

        for (Map.Entry<UUID, Map<String, Long>> playerEntry : cooldowns.entrySet()) {
            Map<String, Long> playerCooldowns = playerEntry.getValue();
            playerCooldowns.entrySet().removeIf(entry -> {
                if (now >= entry.getValue()) {
                    removed.getAndIncrement();
                    return true;
                }
                return false;
            });
        }

        // Remover jugadores sin cooldowns
        cooldowns.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        if (removed.get() > 0) {
            System.out.println("[Raids] Cooldowns expirados limpiados: " + removed);
            saveCooldowns();
        }
    }

    /**
     * Obtiene un resumen de los cooldowns de un jugador
     */
    public static String getCooldownSummary(UUID playerId) {
        Map<String, Long> playerCooldowns = getPlayerCooldowns(playerId);

        if (playerCooldowns.isEmpty()) {
            return "§aSin cooldowns activos";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("§6=== Cooldowns Activos ===\n");

        for (Map.Entry<String, Long> entry : playerCooldowns.entrySet()) {
            String raidId = entry.getKey();
            String timeFormatted = getCooldownFormattedTime(playerId, raidId);
            sb.append("§e").append(raidId).append(": §f").append(timeFormatted).append("\n");
        }

        return sb.toString();
    }

    /**
     * Verifica si un jugador tiene al menos un cooldown
     */
    public static boolean hasAnyCooldown(UUID playerId) {
        return !getPlayerCooldowns(playerId).isEmpty();
    }

    /**
     * Obtiene el total de cooldowns activos de un jugador
     */
    public static int getTotalActiveCooldowns(UUID playerId) {
        return getPlayerCooldowns(playerId).size();
    }

    /**
     * Obtiene el total de todos los cooldowns en el servidor
     */
    public static int getTotalCooldowns() {
        int total = 0;
        for (Map<String, Long> playerCooldowns : cooldowns.values()) {
            total += playerCooldowns.size();
        }
        return total;
    }
}