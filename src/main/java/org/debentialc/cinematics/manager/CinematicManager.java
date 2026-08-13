package org.debentialc.cinematics.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.debentialc.cinematics.model.Cinematic;
import org.debentialc.cinematics.model.CinematicEvent;
import org.debentialc.cinematics.model.Waypoint;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CinematicManager {

    private final JavaPlugin plugin;
    private final File cinematicsFolder;
    private final Map<String, Cinematic> cinematics = new HashMap<>();

    public CinematicManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.cinematicsFolder = new File(plugin.getDataFolder(), "cinematics");
        if (!cinematicsFolder.exists()) {
            cinematicsFolder.mkdirs();
        }
    }

    public void loadAll() {
        cinematics.clear();
        File[] files = cinematicsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            String id = file.getName().replace(".yml", "");
            Cinematic cinematic = loadFromFile(id, file);
            if (cinematic != null) {
                cinematics.put(id.toLowerCase(), cinematic);
            }
        }
        plugin.getLogger().info("Cinematics cargadas: " + cinematics.size());
    }

    public void save(Cinematic cinematic) {
        File file = new File(cinematicsFolder, cinematic.getId() + ".yml");
        FileConfiguration config = new YamlConfiguration();

        config.set("id", cinematic.getId());

        List<Map<String, Object>> waypointsList = new ArrayList<>();
        for (Waypoint waypoint : cinematic.getWaypoints()) {
            Map<String, Object> map = new LinkedHashMap<>();
            Location loc = waypoint.getLocation();
            map.put("world", loc.getWorld().getName());
            map.put("x", loc.getX());
            map.put("y", loc.getY());
            map.put("z", loc.getZ());
            map.put("yaw", loc.getYaw());
            map.put("pitch", loc.getPitch());
            map.put("tick", waypoint.getTick());
            map.put("easing", waypoint.getEasing().name());
            waypointsList.add(map);
        }
        config.set("waypoints", waypointsList);

        List<Map<String, Object>> eventsList = new ArrayList<>();
        for (CinematicEvent event : cinematic.getEvents()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("tick", event.getTick());
            map.put("type", event.getType().name());
            map.put("value", event.getValue());
            if (event.getExtra() != null) {
                map.put("extra", event.getExtra());
            }
            eventsList.add(map);
        }
        config.set("events", eventsList);

        try {
            config.save(file);
            cinematics.put(cinematic.getId().toLowerCase(), cinematic);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void delete(String id) {
        Cinematic cinematic = cinematics.remove(id.toLowerCase());
        if (cinematic != null) {
            File file = new File(cinematicsFolder, cinematic.getId() + ".yml");
            if (file.exists()) {
                file.delete();
            }
        }
    }

    public Cinematic getCinematic(String id) {
        return cinematics.get(id.toLowerCase());
    }

    public Collection<Cinematic> getAllCinematics() {
        return new ArrayList<>(cinematics.values());
    }

    private Cinematic loadFromFile(String id, File file) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        Cinematic cinematic = new Cinematic(config.getString("id", id));

        List<Map<?, ?>> waypointsList = config.getMapList("waypoints");
        for (Map<?, ?> map : waypointsList) {
            World world = Bukkit.getWorld(String.valueOf(map.get("world")));
            if (world == null) continue;
            double x = toDouble(map.get("x"));
            double y = toDouble(map.get("y"));
            double z = toDouble(map.get("z"));
            float yaw = toFloat(map.get("yaw"));
            float pitch = toFloat(map.get("pitch"));
            long tick = toLong(map.get("tick"));
            Waypoint.Easing easing;
            try {
                easing = Waypoint.Easing.valueOf(String.valueOf(map.get("easing")).toUpperCase());
            } catch (Exception e) {
                easing = Waypoint.Easing.LINEAR;
            }
            cinematic.addWaypoint(new Waypoint(new Location(world, x, y, z, yaw, pitch), tick, easing));
        }

        List<Map<?, ?>> eventsList = config.getMapList("events");
        for (Map<?, ?> map : eventsList) {
            long tick = toLong(map.get("tick"));
            CinematicEvent.Type type;
            try {
                type = CinematicEvent.Type.valueOf(String.valueOf(map.get("type")).toUpperCase());
            } catch (Exception e) {
                continue;
            }
            String value = map.get("value") != null ? String.valueOf(map.get("value")) : "";
            String extra = map.get("extra") != null ? String.valueOf(map.get("extra")) : null;
            cinematic.addEvent(new CinematicEvent(tick, type, value, extra));
        }

        return cinematic;
    }

    private double toDouble(Object obj) {
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(obj));
        } catch (Exception e) {
            return 0;
        }
    }

    private float toFloat(Object obj) {
        return (float) toDouble(obj);
    }

    private long toLong(Object obj) {
        if (obj instanceof Number) return ((Number) obj).longValue();
        try {
            return Long.parseLong(String.valueOf(obj));
        } catch (Exception e) {
            return 0;
        }
    }
}
