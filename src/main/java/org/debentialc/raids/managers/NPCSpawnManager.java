package org.debentialc.raids.managers;

import noppes.npcs.api.AbstractNpcAPI;
import noppes.npcs.api.IWorld;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.scripted.NpcAPI;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
import org.debentialc.raids.models.SpawnPoint;
import org.debentialc.raids.models.Wave;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NPCSpawnManager {

    private static final Map<String, Set<Integer>> spawnedNpcIds = new ConcurrentHashMap<>();
    private static final Map<String, List<ICustomNpc>> spawnedNpcRefs = new ConcurrentHashMap<>();

    public static boolean spawnWaveNpcs(Wave wave, String waveId) {
        if (wave == null || wave.getSpawnPoints().isEmpty()) {
            return false;
        }

        Set<Integer> npcIds = new HashSet<>();
        List<ICustomNpc> npcRefs = new ArrayList<>();
        int totalSpawned = 0;

        for (SpawnPoint spawnPoint : wave.getSpawnPoints()) {
            Location loc = spawnPoint.getLocation();
            if (loc == null || loc.getWorld() == null) {
                System.out.println("[Raids] ADVERTENCIA: SpawnPoint sin ubicación válida para NPC: "
                        + spawnPoint.getNpcName());
                continue;
            }

            for (int i = 0; i < spawnPoint.getQuantity(); i++) {
                ICustomNpc npc = spawnNpc(loc, spawnPoint.getNpcName(), spawnPoint.getNpcTab());
                if (npc != null) {
                    int entityId = npc.getEntityId();
                    npcIds.add(entityId);
                    npcRefs.add(npc);
                    totalSpawned++;
                }
            }
        }

        spawnedNpcIds.put(waveId, npcIds);
        spawnedNpcRefs.put(waveId, npcRefs);

        System.out.println("[Raids] Spawneados " + totalSpawned + " NPCs para waveId: " + waveId);
        return totalSpawned > 0;
    }

    private static ICustomNpc spawnNpc(Location location, String npcName, int npcTab) {
        try {
            World world = location.getWorld();
            if (world == null) {
                System.out.println("[Raids] ERROR: mundo nulo para spawn de '" + npcName + "'");
                return null;
            }

            AbstractNpcAPI api = NpcAPI.Instance();
            if (api == null) {
                System.out.println("[Raids] ERROR: AbstractNpcAPI.Instance() retornó null");
                return null;
            }

            double x = location.getX();
            double y = location.getY();
            double z = location.getZ();
            int bx = location.getBlockX();
            int bz = location.getBlockZ();

            // Asegurar que el chunk esté cargado antes de spawnear
            Chunk chunk = world.getChunkAt(bx >> 4, bz >> 4);
            if (!chunk.isLoaded()) {
                chunk.load();
                System.out.println(String.format(
                        "[Raids] Chunk cargado para spawn en %d,%d", bx >> 4, bz >> 4
                ));
            }

            System.out.println(String.format(
                    "[Raids] Spawneando NPC '%s' (tab=%d) en %.2f,%.2f,%.2f (bloque %d,%d,%d) mundo=%s",
                    npcName, npcTab, x, y, z, bx, location.getBlockY(), bz,
                    world.getName()
            ));

            // Usar el ID de dimensión real del mundo NMS, no el environment ID de Bukkit.
            // Esto es necesario para mundos creados por Multiverse, que comparten Environment.NORMAL
            // pero tienen dimension IDs distintos.
            int dimensionId = ((CraftWorld) world).getHandle().dimension;
            System.out.println(String.format(
                    "[Raids] Mundo=%s dimensionId=%d environmentId=%d",
                    world.getName(), dimensionId, world.getEnvironment().getId()
            ));

            IWorld iWorld = api.getIWorld(dimensionId);

            // Método 1: Usar ICloneHandler.spawn, igual que /kam clone spawn.
            // Este lee el clon del ServerCloneController global y funciona en cualquier mundo.
            ICustomNpc npc = null;
            try {
                IEntity entity = api.getClones().spawn(
                        x,
                        y,
                        z,
                        npcTab,
                        npcName,
                        iWorld
                );
                if (entity instanceof ICustomNpc) {
                    npc = (ICustomNpc) entity;
                }
            } catch (Exception e) {
                System.out.println("[Raids] ICloneHandler.spawn falló: " + e.getMessage());
            }

            // Fallback al método original por si acaso
            if (npc == null) {
                System.out.println("[Raids] Fallback a spawnClone");
                IEntity entity = iWorld.spawnClone(bx, location.getBlockY(), bz, npcTab, npcName);
                if (entity instanceof ICustomNpc) {
                    npc = (ICustomNpc) entity;
                }
            }

            if (npc != null) {
                npc.setName(npcName);

                // Forzar posición exacta (double) en entidad Bukkit
                for (Entity e : world.getEntities()) {
                    if (e.getEntityId() == npc.getEntityId()) {
                        e.teleport(location);
                        break;
                    }
                }

                System.out.println(String.format(
                        "[Raids] OK: NPC '%s' spawneado con entityId=%d", npcName, npc.getEntityId()
                ));
                return npc;
            } else {
                System.out.println("[Raids] ERROR: No se pudo spawnear el NPC '" + npcName
                        + "' tab=" + npcTab + " en " + bx + "," + location.getBlockY() + "," + bz
                        + " (mundo=" + world.getName() + ")");
            }

        } catch (Exception e) {
            System.out.println("[Raids] ERROR spawneando NPC '" + npcName + "': " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    public static boolean isNpcFromWave(int entityId, String waveId) {
        Set<Integer> ids = spawnedNpcIds.get(waveId);
        if (ids == null) {
            return false;
        }
        return ids.contains(entityId);
    }

    public static String getWaveIdForNpc(int entityId) {
        for (Map.Entry<String, Set<Integer>> entry : spawnedNpcIds.entrySet()) {
            if (entry.getValue().contains(entityId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static boolean markNpcDead(int entityId, String waveId) {
        Set<Integer> ids = spawnedNpcIds.get(waveId);
        if (ids == null) {
            return false;
        }

        return ids.remove(entityId);
    }

    public static void despawnWaveNpcs(String waveId) {
        List<ICustomNpc> npcs = spawnedNpcRefs.get(waveId);
        if (npcs == null) {
            return;
        }

        for (ICustomNpc npc : npcs) {
            try {
                if (npc != null && npc.isAlive()) {
                    npc.despawn();
                }
            } catch (Exception e) {
            }
        }

        spawnedNpcRefs.remove(waveId);
        spawnedNpcIds.remove(waveId);
    }

    public static void despawnAllNpcs() {
        for (String waveId : new ArrayList<>(spawnedNpcRefs.keySet())) {
            despawnWaveNpcs(waveId);
        }
    }

    public static int getAliveNpcsCount(String waveId) {
        Set<Integer> ids = spawnedNpcIds.get(waveId);
        if (ids == null) {
            return 0;
        }
        return ids.size();
    }

    public static boolean areAllNpcsDead(String waveId) {
        return getAliveNpcsCount(waveId) == 0;
    }

    public static void clearWaveTracking(String waveId) {
        spawnedNpcIds.remove(waveId);
        spawnedNpcRefs.remove(waveId);
    }

    public static String getDebugInfo(String waveId) {
        Set<Integer> ids = spawnedNpcIds.get(waveId);
        if (ids == null) {
            return "Wave " + waveId + ": No tracking data";
        }

        return String.format("Wave %s: %d NPCs vivos - IDs: %s",
                waveId, ids.size(), ids.toString());
    }

    public static Set<String> getActiveWaves() {
        return new HashSet<>(spawnedNpcIds.keySet());
    }
}