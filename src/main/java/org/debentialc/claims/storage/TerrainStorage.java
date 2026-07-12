package org.debentialc.claims.storage;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.debentialc.Main;
import org.debentialc.claims.managers.TerrainManager;
import org.debentialc.claims.models.Terrain;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

public class TerrainStorage {

    private final File folder;

    /**
     * Terrenos que tenían worldName guardado pero el mundo no estaba cargado
     * en el momento de loadAll(). Se reintenta resolver en el primer tick.
     */
    private static final List<PendingOrigin> pendingOrigins = new ArrayList<PendingOrigin>();

    private static class PendingOrigin {
        final String terrainId;
        final String worldName;
        final int x, y, z;

        PendingOrigin(String terrainId, String worldName, int x, int y, int z) {
            this.terrainId = terrainId;
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public TerrainStorage() {
        folder = new File(Main.instance.getDataFolder(), "claims" + File.separator + "terrains");
        folder.mkdirs();
    }

    public void saveTerrain(Terrain terrain) {
        File file = new File(folder, terrain.getId() + ".dat");
        try {
            Properties props = new Properties();
            props.setProperty("id", terrain.getId());
            props.setProperty("chunks", String.valueOf(terrain.getChunks()));
            props.setProperty("price", String.valueOf(terrain.getPrice()));
            props.setProperty("committed", String.valueOf(terrain.isCommitted()));

            if (terrain.getOwner() != null) {
                props.setProperty("owner", terrain.getOwner().toString());
                props.setProperty("ownerName", terrain.getOwnerName() != null ? terrain.getOwnerName() : "");
            }

            if (terrain.getOrigin() != null) {
                Location loc = terrain.getOrigin();
                props.setProperty("world", loc.getWorld().getName());
                props.setProperty("x", String.valueOf(loc.getBlockX()));
                props.setProperty("y", String.valueOf(loc.getBlockY()));
                props.setProperty("z", String.valueOf(loc.getBlockZ()));
            }

            StringBuilder membersStr = new StringBuilder();
            for (Map.Entry<UUID, List<Terrain.MemberRole>> entry : terrain.getMembers().entrySet()) {
                if (membersStr.length() > 0) membersStr.append(";");
                membersStr.append(entry.getKey().toString()).append(":");
                StringBuilder rolesStr = new StringBuilder();
                for (Terrain.MemberRole role : entry.getValue()) {
                    if (rolesStr.length() > 0) rolesStr.append(",");
                    rolesStr.append(role.name());
                }
                membersStr.append(rolesStr.toString());
            }
            if (membersStr.length() > 0) {
                props.setProperty("members", membersStr.toString());
            }

            FileOutputStream fos = new FileOutputStream(file);
            props.store(fos, null);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Map<String, Terrain> loadAll() {
        Map<String, Terrain> terrains = new HashMap<String, Terrain>();
        File[] files = folder.listFiles();
        if (files == null) return terrains;

        for (File file : files) {
            if (!file.getName().endsWith(".dat")) continue;
            try {
                Properties props = new Properties();
                FileInputStream fis = new FileInputStream(file);
                props.load(fis);
                fis.close();

                String id = props.getProperty("id");
                if (id == null || id.isEmpty()) continue;

                int chunks = Integer.parseInt(props.getProperty("chunks", "1"));
                Terrain terrain = new Terrain(id, chunks);
                terrain.setPrice(Double.parseDouble(props.getProperty("price", "0")));
                terrain.setCommitted(Boolean.parseBoolean(props.getProperty("committed", "false")));

                String ownerStr = props.getProperty("owner");
                if (ownerStr != null && !ownerStr.isEmpty()) {
                    terrain.setOwner(UUID.fromString(ownerStr));
                    terrain.setOwnerName(props.getProperty("ownerName", ""));
                }

                String worldName = props.getProperty("world");
                if (worldName != null && !worldName.isEmpty()) {
                    int x = Integer.parseInt(props.getProperty("x", "0"));
                    int y = Integer.parseInt(props.getProperty("y", "64"));
                    int z = Integer.parseInt(props.getProperty("z", "0"));

                    World world = Bukkit.getWorld(worldName);
                    if (world != null) {
                        terrain.setOrigin(new Location(world, x, y, z));
                    } else {
                        // El mundo no está cargado aún — guardar para resolver luego
                        pendingOrigins.add(new PendingOrigin(id, worldName, x, y, z));
                        Main.instance.getLogger().warning(
                                "[Claims] Mundo '" + worldName + "' no disponible al cargar terreno '"
                                        + id + "'. Se reintentará en el siguiente tick.");
                    }
                }

                String membersStr = props.getProperty("members");
                if (membersStr != null && !membersStr.isEmpty()) {
                    for (String memberEntry : membersStr.split(";")) {
                        String[] parts = memberEntry.split(":");
                        if (parts.length < 2) continue;
                        UUID uuid = UUID.fromString(parts[0]);
                        for (String roleStr : parts[1].split(",")) {
                            try {
                                Terrain.MemberRole role = Terrain.MemberRole.valueOf(roleStr.trim());
                                terrain.addMemberRole(uuid, null, role);
                            } catch (Exception ignore) {}
                        }
                    }
                }

                terrains.put(id, terrain);
            } catch (Exception e) {
                Main.instance.getLogger().warning("[Claims] Error cargando terreno desde " + file.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Programar resolución de mundos pendientes en el siguiente tick
        if (!pendingOrigins.isEmpty()) {
            Bukkit.getScheduler().runTaskLater(Main.instance, new Runnable() {
                public void run() {
                    resolvePendingOrigins();
                }
            }, 1L);
        }

        return terrains;
    }

    /**
     * Intenta resolver los orígenes pendientes cuyos mundos no estaban cargados.
     * Se llama automáticamente 1 tick después del inicio del servidor.
     */
    private void resolvePendingOrigins() {
        List<PendingOrigin> stillPending = new ArrayList<PendingOrigin>();

        for (PendingOrigin po : pendingOrigins) {
            World world = Bukkit.getWorld(po.worldName);
            if (world != null) {
                Terrain terrain = TerrainManager.getInstance().getTerrain(po.terrainId);
                if (terrain != null) {
                    terrain.setOrigin(new Location(world, po.x, po.y, po.z));
                    Main.instance.getLogger().info(
                            "[Claims] Origen del terreno '" + po.terrainId + "' resuelto correctamente.");
                }
            } else {
                stillPending.add(po);
                Main.instance.getLogger().warning(
                        "[Claims] El mundo '" + po.worldName + "' sigue sin estar disponible para el terreno '"
                                + po.terrainId + "'. El terreno quedará sin posición hasta el próximo reinicio.");
            }
        }

        pendingOrigins.clear();
        pendingOrigins.addAll(stillPending);
    }

    public void deleteTerrain(String id) {
        File file = new File(folder, id + ".dat");
        if (file.exists()) file.delete();
    }
}