package org.debentialc.raids.managers;

import org.debentialc.raids.models.Raid;
import org.debentialc.raids.models.RaidStatus;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RaidManager - Gestor principal de raids
 * Responsable de crear, obtener, editar y eliminar raids
 */
public class RaidManager {

    private static final Map<String, Raid> raids = new ConcurrentHashMap<>();
    private static int raidCounter = 0;

    /**
     * Crea una nueva raid con ID automático
     */
    public static Raid createRaid(String raidName) {
        String raidId = "raid_" + (++raidCounter);
        Raid raid = new Raid(raidId, raidName);
        raids.put(raidId, raid);

        System.out.println("[Raids] Nueva raid creada: " + raidId + " - " + raidName);
        return raid;
    }

    /**
     * Crea una nueva raid con ID personalizado
     */
    public static Raid createRaidWithId(String raidId, String raidName) {
        if (raidId == null || raidId.isEmpty() || raids.containsKey(raidId)) {
            return null;
        }
        Raid raid = new Raid(raidId, raidName);
        raids.put(raidId, raid);
        System.out.println("[Raids] Nueva raid creada: " + raidId);
        return raid;
    }

    /**
     * Cambia el ID de una raid existente
     */
    public static boolean changeRaidId(String oldId, String newId) {
        if (oldId == null || newId == null || oldId.equalsIgnoreCase(newId)) return false;
        if (!raids.containsKey(oldId)) return false;
        if (raids.containsKey(newId)) return false;

        Raid raid = raids.remove(oldId);
        raid.setRaidId(newId);
        raids.put(newId, raid);
        RaidStorageManager.deleteRaidFiles(oldId);
        RaidStorageManager.saveAllRaids();
        System.out.println("[Raids] ID cambiado: " + oldId + " -> " + newId);
        return true;
    }

    /**
     * Obtiene una raid por ID
     */
    public static Raid getRaidById(String raidId) {
        return raids.get(raidId);
    }

    /**
     * Obtiene una raid por nombre (busca la primera coincidencia)
     */
    public static Raid getRaidByName(String raidName) {
        return raids.values().stream()
                .filter(r -> r.getRaidName().equalsIgnoreCase(raidName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Obtiene todas las raids
     */
    public static List<Raid> getAllRaids() {
        return new ArrayList<>(raids.values());
    }

    /**
     * Obtiene todas las raids habilitadas
     */
    public static List<Raid> getEnabledRaids() {
        List<Raid> enabled = new ArrayList<>();
        for (Raid raid : raids.values()) {
            if (raid.isEnabled() && raid.isConfigured()) {
                enabled.add(raid);
            }
        }
        return enabled;
    }

    /**
     * Edita una raid existente
     */
    public static void updateRaid(Raid raid) {
        if (raid != null) {
            raids.put(raid.getRaidId(), raid);
            System.out.println("[Raids] Raid actualizada: " + raid.getRaidId());
        }
    }

    /**
     * Elimina una raid
     */
    public static boolean deleteRaid(String raidId) {
        if (raids.remove(raidId) != null) {
            RaidStorageManager.deleteRaidFiles(raidId);
            RaidStorageManager.saveAllRaids();
            System.out.println("[Raids] Raid eliminada: " + raidId);
            return true;
        }
        return false;
    }

    /**
     * Verifica si una raid existe
     */
    public static boolean raidExists(String raidId) {
        return raids.containsKey(raidId);
    }

    /**
     * Obtiene la cantidad total de raids
     */
    public static int getTotalRaids() {
        return raids.size();
    }

    /**
     * Obtiene la cantidad de raids habilitadas
     */
    public static int getTotalEnabledRaids() {
        return (int) raids.values().stream()
                .filter(Raid::isEnabled)
                .count();
    }

    /**
     * Habilita una raid
     */
    public static void enableRaid(String raidId) {
        Raid raid = raids.get(raidId);
        if (raid != null) {
            raid.setEnabled(true);
            System.out.println("[Raids] Raid habilitada: " + raidId);
        }
    }

    /**
     * Deshabilita una raid
     */
    public static void disableRaid(String raidId) {
        Raid raid = raids.get(raidId);
        if (raid != null) {
            raid.setEnabled(false);
            System.out.println("[Raids] Raid deshabilitada: " + raidId);
        }
    }

    /**
     * Obtiene información de una raid
     */
    public static String getRaidInfo(String raidId) {
        Raid raid = raids.get(raidId);
        if (raid == null) {
            return "Raid no encontrada";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("§6=== Información de Raid ===\n");
        sb.append("§eID: §f").append(raid.getRaidId()).append("\n");
        sb.append("§eNombre: §f").append(raid.getDisplayName()).append("\n");
        sb.append("§eDescripción: §f").append(raid.getDescription()).append("\n");
        sb.append("§eOleadas: §f").append(raid.getTotalWaves()).append("\n");
        sb.append("§eJugadores: §f").append(raid.getMinPlayers()).append("-").append(raid.getMaxPlayers()).append("\n");
        sb.append("§eCooldown: §f").append(raid.getCooldownSeconds()).append("s\n");
        sb.append("§eEstado: §f").append(raid.getStatus().getDisplayName()).append("\n");
        sb.append("§eConfigured: §f").append(raid.isConfigured() ? "✓" : "✗");

        return sb.toString();
    }

    /**
     * Obtiene un listado de raids
     */
    public static String getRaidsList() {
        List<Raid> raidsList = getAllRaids();

        if (raidsList.isEmpty()) {
            return "§cNo hay raids creadas";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("§6=== Raids Disponibles ===\n");

        for (int i = 0; i < raidsList.size(); i++) {
            Raid raid = raidsList.get(i);
            String status = raid.isEnabled() ? "§a✓" : "§c✗";
            String configured = raid.isConfigured() ? "§a✓" : "§c✗";

            sb.append(String.format("§e[%d] §f%s §7(%s) §7- Oleadas: §f%d §7- Configurada: %s\n",
                    i + 1,
                    raid.getDisplayName(),
                    raid.getRaidId(),
                    raid.getTotalWaves(),
                    configured
            ));
        }

        return sb.toString();
    }

    /**
     * Verifica que una raid esté lista para jugar
     */
    public static boolean isRaidReadyToPlay(String raidId) {
        Raid raid = raids.get(raidId);
        if (raid == null) {
            return false;
        }

        return raid.isEnabled() &&
                raid.isConfigured() &&
                raid.getTotalWaves() > 0;
    }

    /**
     * Limpia todas las raids (útil para reload)
     */
    public static void clearAllRaids() {
        raids.clear();
        raidCounter = 0;
        System.out.println("[Raids] Todas las raids han sido limpiadas");
    }

    /**
     * Recalcula el contador de raids para evitar IDs duplicados tras reinicio.
     * Debe llamarse después de cargar las raids desde disco.
     */
    public static void recalculateRaidCounter() {
        int max = 0;
        for (String id : raids.keySet()) {
            if (id.startsWith("raid_")) {
                try {
                    int num = Integer.parseInt(id.substring(5));
                    if (num > max) {
                        max = num;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        raidCounter = max;
        System.out.println("[Raids] Contador de raids recalculado: " + raidCounter);
    }

    /**
     * Obtiene todos los IDs de raids
     */
    public static Set<String> getAllRaidIds() {
        return new HashSet<>(raids.keySet());
    }
}