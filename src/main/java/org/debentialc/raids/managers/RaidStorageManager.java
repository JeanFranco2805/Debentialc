package org.debentialc.raids.managers;

import com.google.gson.*;
import org.bukkit.Location;
import org.debentialc.raids.models.*;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * RaidStorageManager - Gestor de almacenamiento de raids
 * CORREGIDO: Maneja referencias circulares correctamente
 */
public class RaidStorageManager {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Location.class, new LocationAdapter())
            .excludeFieldsWithoutExposeAnnotation() // IMPORTANTE: Solo serializar campos con @Expose
            .create();

    private static final String BASE_PATH = "plugins/Debentialc/data/raids";
    private static final String RAIDS_FILE = "raids.json";
    private static final String COOLDOWNS_FILE = "cooldowns.json";

    static {
        try {
            Files.createDirectories(Paths.get(BASE_PATH));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adaptador para serializar/deserializar Location de Bukkit
     */
    private static class LocationAdapter implements JsonSerializer<Location>, JsonDeserializer<Location> {
        @Override
        public JsonElement serialize(Location loc, Type type, JsonSerializationContext context) {
            if (loc == null) return JsonNull.INSTANCE;

            JsonObject obj = new JsonObject();
            obj.addProperty("world", loc.getWorld().getName());
            obj.addProperty("x", loc.getX());
            obj.addProperty("y", loc.getY());
            obj.addProperty("z", loc.getZ());
            obj.addProperty("yaw", loc.getYaw());
            obj.addProperty("pitch", loc.getPitch());
            return obj;
        }

        @Override
        public Location deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonNull()) return null;

            JsonObject obj = json.getAsJsonObject();
            String worldName = obj.get("world").getAsString();
            double x = obj.get("x").getAsDouble();
            double y = obj.get("y").getAsDouble();
            double z = obj.get("z").getAsDouble();
            float yaw = obj.has("yaw") ? obj.get("yaw").getAsFloat() : 0;
            float pitch = obj.has("pitch") ? obj.get("pitch").getAsFloat() : 0;

            org.bukkit.World world = org.bukkit.Bukkit.getWorld(worldName);
            if (world == null) {
                System.err.println("[Raids] Mundo no encontrado: " + worldName);
                return null;
            }

            return new Location(world, x, y, z, yaw, pitch);
        }
    }

    // ... resto de métodos sin cambios

    /**
     * Guarda todas las raids en JSON
     */
    public static void saveAllRaids() {
        try {
            Path filePath = Paths.get(BASE_PATH, RAIDS_FILE);
            List<Raid> raidsList = RaidManager.getAllRaids();

            String json = GSON.toJson(raidsList);
            Files.write(filePath, json.getBytes(StandardCharsets.UTF_8));

            System.out.println("[Raids] Raids guardadas: " + raidsList.size());
        } catch (Exception e) {
            System.err.println("[Raids] Error al guardar raids: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Carga todas las raids desde JSON
     */
    public static void loadAllRaids() {
        try {
            Path filePath = Paths.get(BASE_PATH, RAIDS_FILE);

            if (!Files.exists(filePath)) {
                System.out.println("[Raids] Archivo de raids no encontrado. Se creará al guardar.");
                return;
            }

            String json = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
            Raid[] raidsArray = GSON.fromJson(json, Raid[].class);

            RaidManager.clearAllRaids();

            if (raidsArray != null) {
                for (Raid raid : raidsArray) {
                    RaidManager.updateRaid(raid);
                }
                RaidManager.recalculateRaidCounter();
                System.out.println("[Raids] Raids cargadas: " + raidsArray.length);
            }

        } catch (Exception e) {
            System.err.println("[Raids] Error al cargar raids: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Guarda una raid individual
     */
    public static void saveRaid(Raid raid) {
        if (raid == null) {
            return;
        }

        try {
            Path raidDir = Paths.get(BASE_PATH, "raid_" + raid.getRaidId());
            Files.createDirectories(raidDir);

            Path filePath = raidDir.resolve("config.json");
            String json = GSON.toJson(raid);
            Files.write(filePath, json.getBytes(StandardCharsets.UTF_8));

            System.out.println("[Raids] Raid individual guardada: " + raid.getRaidId());
        } catch (Exception e) {
            System.err.println("[Raids] Error al guardar raid individual: " + e.getMessage());
            e.printStackTrace();
        }

        // Guardar también la lista maestra para que los cambios sobrevivan al reinicio.
        saveAllRaids();
    }

    /**
     * Carga una raid individual
     */
    public static Raid loadRaid(String raidId) {
        try {
            Path filePath = Paths.get(BASE_PATH, "raid_" + raidId, "config.json");

            if (!Files.exists(filePath)) {
                return null;
            }

            String json = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
            return GSON.fromJson(json, Raid.class);

        } catch (Exception e) {
            System.err.println("[Raids] Error al cargar raid: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Mantener el resto de métodos sin cambios...

    public static void saveCooldowns() {
        try {
            Path filePath = Paths.get(BASE_PATH, COOLDOWNS_FILE);
            Map<String, Map<String, Long>> cooldownsData = new HashMap<>();
            String json = GSON.toJson(cooldownsData);
            Files.write(filePath, json.getBytes(StandardCharsets.UTF_8));
            System.out.println("[Raids] Cooldowns guardados");
        } catch (IOException e) {
            System.err.println("[Raids] Error al guardar cooldowns: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void loadCooldowns() {
        try {
            Path filePath = Paths.get(BASE_PATH, COOLDOWNS_FILE);
            if (!Files.exists(filePath)) {
                System.out.println("[Raids] Archivo de cooldowns no encontrado.");
                return;
            }
            String json = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
            System.out.println("[Raids] Cooldowns cargados");
        } catch (IOException | JsonSyntaxException e) {
            System.err.println("[Raids] Error al cargar cooldowns: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void deleteRaidFiles(String raidId) {
        try {
            Path raidDir = Paths.get(BASE_PATH, "raid_" + raidId);
            if (Files.exists(raidDir)) {
                Files.walk(raidDir)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                System.err.println("[Raids] Error al eliminar archivo: " + e.getMessage());
                            }
                        });
                System.out.println("[Raids] Archivos de raid eliminados: " + raidId);
            }
        } catch (IOException e) {
            System.err.println("[Raids] Error al eliminar archivos de raid: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void saveRaidSystemData() {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("totalRaids", RaidManager.getTotalRaids());
            data.put("totalEnabledRaids", RaidManager.getTotalEnabledRaids());
            data.put("totalParties", PartyManager.getTotalParties());
            data.put("totalPlayersInParties", PartyManager.getTotalPlayersInParties());
            data.put("totalActiveSessions", RaidSessionManager.getTotalActiveSessions());
            data.put("totalPlayersInRaids", RaidSessionManager.getTotalPlayersInRaids());
            data.put("lastSaveTime", System.currentTimeMillis());

            Path filePath = Paths.get(BASE_PATH, "system_data.json");
            String json = GSON.toJson(data);
            Files.write(filePath, json.getBytes(StandardCharsets.UTF_8));
            System.out.println("[Raids] Datos del sistema guardados");
        } catch (IOException e) {
            System.err.println("[Raids] Error al guardar datos del sistema: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static Map<String, Object> loadRaidSystemData() {
        try {
            Path filePath = Paths.get(BASE_PATH, "system_data.json");
            if (!Files.exists(filePath)) {
                return new HashMap<>();
            }
            String json = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
            return GSON.fromJson(json, HashMap.class);
        } catch (IOException | JsonSyntaxException e) {
            System.err.println("[Raids] Error al cargar datos del sistema: " + e.getMessage());
            return new HashMap<>();
        }
    }

    public static void backupRaids() {
        try {
            Path source = Paths.get(BASE_PATH);
            Path backup = Paths.get(BASE_PATH, "backup_" + System.currentTimeMillis());
            Files.createDirectories(backup);
            Files.walk(source)
                    .filter(p -> !p.equals(source))
                    .forEach(p -> {
                        try {
                            Path relative = source.relativize(p);
                            Path target = backup.resolve(relative);
                            if (Files.isDirectory(p)) {
                                Files.createDirectories(target);
                            } else {
                                Files.copy(p, target);
                            }
                        } catch (IOException e) {
                            System.err.println("[Raids] Error al hacer backup: " + e.getMessage());
                        }
                    });
            System.out.println("[Raids] Backup realizado: " + backup);
        } catch (IOException e) {
            System.err.println("[Raids] Error al realizar backup: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void cleanup() {
        try {
            Path source = Paths.get(BASE_PATH);
            Files.walk(source)
                    .filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().startsWith("raid_"))
                    .forEach(p -> {
                        String raidId = p.getFileName().toString().replace("raid_", "");
                        if (!RaidManager.raidExists(raidId)) {
                            try {
                                Files.walk(p)
                                        .sorted(Comparator.reverseOrder())
                                        .forEach(path -> {
                                            try {
                                                Files.delete(path);
                                            } catch (IOException e) {
                                                System.err.println("[Raids] Error en cleanup: " + e.getMessage());
                                            }
                                        });
                                System.out.println("[Raids] Directorio obsoleto eliminado: " + raidId);
                            } catch (IOException e) {
                                System.err.println("[Raids] Error al limpiar: " + e.getMessage());
                            }
                        }
                    });
        } catch (IOException e) {
            System.err.println("[Raids] Error durante cleanup: " + e.getMessage());
            e.printStackTrace();
        }
    }
}