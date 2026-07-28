package org.debentialc.customitems.tools.scripts;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.debentialc.Main;
import org.debentialc.service.CC;
import org.debentialc.service.ServerUtil;
import org.mozilla.javascript.Function;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * API de utilidades para scripts JavaScript
 * Proporciona funciones helpers que simplifican operaciones comunes
 *
 * DISPONIBLE EN SCRIPTS COMO: api.*
 */
public class ScriptAPI {
    private final Random random = new Random();
    private final String scriptId;
    private final Map<String, ScriptEventListener> eventListeners = new HashMap<>();

    public ScriptAPI(String scriptId) {
        this.scriptId = scriptId;
    }

    // ============================================================================
    // JUGADORES
    // ============================================================================

    /**
     * Obtiene un jugador por nombre
     */
    public Player getPlayer(String name) {
        return Bukkit.getPlayer(name);
    }

    /**
     * Obtiene todos los jugadores online
     */
    public Player[] getOnlinePlayers() {
        Player[] online = ServerUtil.getOnlinePlayers();
        Player[] players = new Player[Main.instance.getServer().getMaxPlayers()];
        int i = 0;
        for (Player onlinePlayer : online) {
            players[i] = onlinePlayer;
            i++;
        }
        return players;
    }

    /**
     * Obtiene jugadores cercanos a una ubicación
     */
    public List<Player> getNearbyPlayers(Location location, double radius) {
        List<Player> nearby = new ArrayList<>();
        Player[] players = ServerUtil.getOnlinePlayers();
        for (Player player : players) {
            if (player.getWorld().equals(location.getWorld()) &&
                    player.getLocation().distance(location) <= radius) {
                nearby.add(player);
            }
        }
        return nearby;
    }

    /**
     * Teleporta un jugador a una ubicación
     */
    public void teleport(Player player, Location location) {
        player.teleport(location);
    }

    /**
     * Teleporta un jugador a otro jugador
     */
    public void teleport(Player player, Player target) {
        player.teleport(target.getLocation());
    }

    /**
     * Da un item a un jugador
     */
    public void giveItem(Player player, ItemStack item) {
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItem(player.getLocation(), item);
        } else {
            player.getInventory().addItem(item);
        }
    }

    /**
     * Cura completamente a un jugador
     */
    public void heal(Player player) {
        double maxHealth = ServerUtil.getMaxHealth(player);
        player.setHealth(maxHealth);
        player.setFoodLevel(20);
        player.setFireTicks(0);
        player.getActivePotionEffects().forEach(effect ->
                player.removePotionEffect(effect.getType()));
    }

    /**
     * Da un efecto de poción a un jugador
     */
    public void giveEffect(Player player, String effectName, int duration, int amplifier) {
        PotionEffectType type = PotionEffectType.getByName(effectName.toUpperCase());
        if (type != null) {
            player.addPotionEffect(new PotionEffect(type, duration * 20, amplifier));
        }
    }

    // ============================================================================
    // MENSAJES
    // ============================================================================

    /**
     * Envía un mensaje con colores traducidos
     */
    public void sendMessage(Player player, String message) {
        player.sendMessage(CC.translate(message));
    }

    /**
     * Envía un mensaje a todos los jugadores
     */
    public void broadcast(String message) {
        Bukkit.broadcastMessage(CC.translate(message));
    }

    /**
     * Envía un título al jugador (para 1.7.10 usa chat)
     */
    public void sendTitle(Player player, String title, String subtitle) {
        player.sendMessage(CC.translate("&8&m--------------------"));
        player.sendMessage(CC.translate(title));
        if (subtitle != null && !subtitle.isEmpty()) {
            player.sendMessage(CC.translate(subtitle));
        }
        player.sendMessage(CC.translate("&8&m--------------------"));
    }

    // ============================================================================
    // MUNDO Y ENTIDADES
    // ============================================================================

    /**
     * Obtiene el mundo por nombre
     */
    public World getWorld(String name) {
        return Bukkit.getWorld(name);
    }

    /**
     * Crea una ubicación
     */
    public Location createLocation(World world, double x, double y, double z) {
        return new Location(world, x, y, z);
    }

    /**
     * Crea una ubicación con yaw y pitch
     */
    public Location createLocation(World world, double x, double y, double z, float yaw, float pitch) {
        return new Location(world, x, y, z, yaw, pitch);
    }

    /**
     * Obtiene entidades cercanas a una ubicación
     */
    public List<Entity> getNearbyEntities(Location location, double radius) {
        List<Entity> entities = new ArrayList<>();
        for (Entity entity : location.getWorld().getEntities()) {
            if (entity.getLocation().distance(location) <= radius) {
                entities.add(entity);
            }
        }
        return entities;
    }

    /**
     * Genera un rayo en una ubicación
     */
    public void strikeLightning(Location location) {
        location.getWorld().strikeLightning(location);
    }

    /**
     * Genera un rayo visual (sin daño)
     */
    public void strikeLightningEffect(Location location) {
        location.getWorld().strikeLightningEffect(location);
    }

    /**
     * Crea una explosión
     */
    public void createExplosion(Location location, float power, boolean setFire, boolean breakBlocks) {
        location.getWorld().createExplosion(location, power);
    }

    // ============================================================================
    // SONIDOS Y EFECTOS
    // ============================================================================

    /**
     * Reproduce un sonido para un jugador
     */
    public void playSound(Player player, String sound, float volume, float pitch) {
        try {
            Sound soundEnum = Sound.valueOf(sound.toUpperCase());
            player.playSound(player.getLocation(), soundEnum, volume, pitch);
        } catch (Exception e) {
            // Sonido inválido
        }
    }

    /**
     * Reproduce un sonido en una ubicación para todos
     */
    public void playSoundAt(Location location, String sound, float volume, float pitch) {
        try {
            Sound soundEnum = Sound.valueOf(sound.toUpperCase());
            location.getWorld().playSound(location, soundEnum, volume, pitch);
        } catch (Exception e) {
            // Sonido inválido
        }
    }

    /**
     * Crea un efecto de partículas (limitado en 1.7.10)
     */
    public void playEffect(Location location, String effect) {
        try {
            Effect effectEnum = Effect.valueOf(effect.toUpperCase());
            location.getWorld().playEffect(location, effectEnum, 1);
        } catch (Exception e) {
            // Efecto inválido
        }
    }

    // ============================================================================
    // COMANDOS
    // ============================================================================

    /**
     * Ejecuta un comando como consola
     */
    public void executeCommand(String command) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    /**
     * Ejecuta un comando como un jugador
     */
    public void executeCommandAsPlayer(Player player, String command) {
        player.performCommand(command);
    }

    // ============================================================================
    // UTILIDADES
    // ============================================================================

    /**
     * Genera un número aleatorio entre min y max
     */
    public int random(int min, int max) {
        return random.nextInt((max - min) + 1) + min;
    }

    /**
     * Genera un número decimal aleatorio entre 0 y 1
     */
    public double randomDouble() {
        return random.nextDouble();
    }

    /**
     * Calcula la distancia entre dos ubicaciones
     */
    public double distance(Location loc1, Location loc2) {
        return loc1.distance(loc2);
    }

    /**
     * Obtiene la dirección hacia donde mira el jugador
     */
    public Vector getDirection(Player player) {
        return player.getLocation().getDirection();
    }

    /**
     * Empuja una entidad en una dirección
     */
    public void push(Entity entity, Vector direction, double force) {
        Vector velocity = direction.normalize().multiply(force);
        entity.setVelocity(velocity);
    }

    /**
     * Empuja una entidad hacia arriba
     */
    public void launch(Entity entity, double force) {
        entity.setVelocity(new Vector(0, force, 0));
    }

    /**
     * Atrae una entidad hacia una ubicación
     */
    public void attract(Entity entity, Location target, double force) {
        Vector direction = target.toVector().subtract(entity.getLocation().toVector());
        entity.setVelocity(direction.normalize().multiply(force));
    }

    // ============================================================================
    // JUGADOR - INFORMACIÓN
    // ============================================================================

    /**
     * Obtiene la vida del jugador
     */
    public double getHealth(Player player) {
        return ServerUtil.getHealth(player);
    }

    /**
     * Obtiene la vida máxima del jugador
     */
    public double getMaxHealth(Player player) {
        return ServerUtil.getMaxHealth(player);
    }

    /**
     * Establece la vida del jugador
     */
    public void setHealth(Player player, double health) {
        double maxHealth = ServerUtil.getMaxHealth(player);
        player.setHealth(Math.min(health, maxHealth));
    }

    /**
     * Obtiene el nivel de comida del jugador
     */
    public int getFoodLevel(Player player) {
        return player.getFoodLevel();
    }

    /**
     * Establece el nivel de comida del jugador
     */
    public void setFoodLevel(Player player, int level) {
        player.setFoodLevel(Math.min(Math.max(level, 0), 20));
    }

    /**
     * Obtiene el nivel de experiencia del jugador
     */
    public int getLevel(Player player) {
        return player.getLevel();
    }

    /**
     * Establece el nivel de experiencia del jugador
     */
    public void setLevel(Player player, int level) {
        player.setLevel(level);
    }

    /**
     * Da experiencia al jugador
     */
    public void giveExp(Player player, int amount) {
        player.giveExp(amount);
    }

    /**
     * Verifica si el jugador está agachado
     */
    public boolean isSneaking(Player player) {
        return player.isSneaking();
    }

    /**
     * Verifica si el jugador está corriendo
     */
    public boolean isSprinting(Player player) {
        return player.isSprinting();
    }

    /**
     * Verifica si el jugador está volando
     */
    public boolean isFlying(Player player) {
        return player.isFlying();
    }

    /**
     * Verifica si el jugador está en el suelo
     */
    public boolean isOnGround(Player player) {
        return player.isOnGround();
    }


    /**
     * Crea un ItemStack
     */
    public ItemStack createItem(int materialId, int amount, short data) {
        return new ItemStack(materialId, amount, data);
    }

    /**
     * Crea un ItemStack simple
     */
    public ItemStack createItem(int materialId, int amount) {
        return new ItemStack(materialId, amount);
    }

    /**
     * Remueve items del inventario del jugador
     */
    public void removeItem(Player player, int materialId, int amount) {
        player.getInventory().removeItem(new ItemStack(materialId, amount));
    }

    /**
     * Verifica si el jugador tiene un item
     */
    public boolean hasItem(Player player, int materialId, int amount) {
        return player.getInventory().contains(materialId, amount);
    }

    /**
     * Limpia el inventario del jugador
     */
    public void clearInventory(Player player) {
        player.getInventory().clear();
    }

    // ============================================================================
    // PERMISOS Y OPERADORES
    // ============================================================================

    /**
     * Verifica si el jugador tiene un permiso
     */
    public boolean hasPermission(Player player, String permission) {
        return player.hasPermission(permission);
    }

    /**
     * Verifica si el jugador es operador
     */
    public boolean isOp(Player player) {
        return player.isOp();
    }

    // ============================================================================
    // TIEMPO Y CLIMA
    // ============================================================================

    /**
     * Obtiene el tiempo del mundo
     */
    public long getTime(World world) {
        return world.getTime();
    }

    /**
     * Establece el tiempo del mundo
     */
    public void setTime(World world, long time) {
        world.setTime(time);
    }

    /**
     * Establece el clima del mundo
     */
    public void setWeather(World world, boolean storm) {
        world.setStorm(storm);
    }

    /**
     * Establece si hay truenos
     */
    public void setThundering(World world, boolean thunder) {
        world.setThundering(thunder);
    }


    /**
     * Guarda un dato personalizado en un jugador (solo durante la sesión)
     */
    public void setPlayerData(Player player, String key, Object value) {
        player.setMetadata(key, new org.bukkit.metadata.FixedMetadataValue(
                org.debentialc.Main.instance, value));
    }

    /**
     * Obtiene un dato personalizado de un jugador
     */
    public Object getPlayerData(Player player, String key) {
        if (player.hasMetadata(key)) {
            return player.getMetadata(key).get(0).value();
        }
        return null;
    }

    /**
     * Remueve un dato personalizado de un jugador
     */
    public void removePlayerData(Player player, String key) {
        player.removeMetadata(key, org.debentialc.Main.instance);
    }

    // ============================================================================
    // DEBUGGING
    // ============================================================================

    /**
     * Imprime en la consola del servidor
     */
    public void log(String message) {
        Bukkit.getLogger().info("[Script] " + message);
    }

    /**
     * Imprime un warning en la consola
     */
    public void warn(String message) {
        Bukkit.getLogger().warning("[Script] " + message);
    }

    /**
     * Imprime un error en la consola
     */
    public void error(String message) {
        Bukkit.getLogger().severe("[Script] " + message);
    }


    /**
     * Registra un listener para un evento de Bukkit
     *
     * Uso:
     *   api.on("PlayerJoinEvent", function(event) {
     *       api.broadcast("§e" + event.getPlayer().getName() + " §ase unió!");
     *   });
     *
     * Eventos comunes:
     *   - PlayerJoinEvent, PlayerQuitEvent
     *   - PlayerInteractEvent, PlayerMoveEvent
     *   - BlockBreakEvent, BlockPlaceEvent
     *   - EntityDamageEvent, EntityDeathEvent
     *   - InventoryClickEvent, InventoryCloseEvent
     *
     * @param eventName Nombre del evento (sin paquete o con paquete completo)
     * @param callback Función JavaScript que se ejecutará cuando ocurra el evento
     */
    public void on(String eventName, Function callback) {
        on(eventName, callback, EventPriority.NORMAL);
    }

    /**
     * Registra un listener con prioridad específica
     *
     * Prioridades disponibles:
     *   - LOWEST (se ejecuta primero)
     *   - LOW
     *   - NORMAL (por defecto)
     *   - HIGH
     *   - HIGHEST (se ejecuta último)
     *   - MONITOR (solo lectura, no modificar el evento)
     *
     * @param eventName Nombre del evento
     * @param callback Función callback
     * @param priority Prioridad como String ("NORMAL", "HIGH", etc.)
     */
    public void on(String eventName, Function callback, String priority) {
        EventPriority eventPriority;
        try {
            eventPriority = EventPriority.valueOf(priority.toUpperCase());
        } catch (IllegalArgumentException e) {
            warn("Prioridad inválida '" + priority + "', usando NORMAL");
            eventPriority = EventPriority.NORMAL;
        }
        on(eventName, callback, eventPriority);
    }

    /**
     * Registra un listener con prioridad específica
     */
    private void on(String eventName, Function callback, EventPriority priority) {
        try {
            // Verificar si ya existe un listener para este evento
            if (eventListeners.containsKey(eventName)) {
                warn("Ya existe un listener para " + eventName + " en este script. Será reemplazado.");
                off(eventName);
            }

            ScriptEventListener listener = new ScriptEventListener(
                    scriptId,
                    eventName,
                    callback,
                    priority
            );

            eventListeners.put(eventName, listener);
            log("Evento registrado: " + eventName + " (prioridad: " + priority + ")");

        } catch (ClassNotFoundException e) {
            error("No se encontró el evento: " + eventName);
            error("Verifica que el nombre del evento sea correcto.");
        } catch (Exception e) {
            error("Error registrando evento " + eventName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Desregistra un listener de evento
     *
     * @param eventName Nombre del evento a desregistrar
     */
    public void off(String eventName) {
        ScriptEventListener listener = eventListeners.remove(eventName);
        if (listener != null) {
            listener.unregister();
            log("Evento desregistrado: " + eventName);
        } else {
            warn("No se encontró listener para: " + eventName);
        }
    }

    /**
     * Desregistra todos los listeners de este script
     */
    public void offAll() {
        if (eventListeners.isEmpty()) {
            return;
        }

        log("Desregistrando " + eventListeners.size() + " listeners de eventos...");

        for (ScriptEventListener listener : eventListeners.values()) {
            listener.unregister();
        }

        eventListeners.clear();
        log("Todos los listeners desregistrados.");
    }

    /**
     * Obtiene la lista de eventos registrados por este script
     */
    public List<String> getRegisteredEvents() {
        return new ArrayList<>(eventListeners.keySet());
    }

    /**
     * Verifica si un evento está registrado
     */
    public boolean isEventRegistered(String eventName) {
        return eventListeners.containsKey(eventName);
    }
}