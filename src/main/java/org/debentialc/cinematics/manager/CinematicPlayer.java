package org.debentialc.cinematics.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.debentialc.cinematics.model.Cinematic;
import org.debentialc.cinematics.model.CinematicEvent;
import org.debentialc.cinematics.model.Waypoint;
import org.debentialc.service.CC;

import java.util.*;

public class CinematicPlayer {

    private final Cinematic cinematic;
    private final Player player;
    private final List<Waypoint> waypoints;
    private final List<CinematicEvent> events;
    private final Set<CinematicEvent> triggeredEvents = new HashSet<>();
    private final Location startLocation;
    private BukkitRunnable task;
    private long currentTick = 0;
    private boolean running = false;

    // Estado previo del jugador
    private boolean prevAllowFlight;
    private boolean prevFlying;
    private float prevWalkSpeed;
    private float prevFlySpeed;

    public CinematicPlayer(Cinematic cinematic, Player player) {
        this.cinematic = cinematic;
        this.player = player;
        this.waypoints = cinematic.getWaypoints();
        this.events = cinematic.getEvents();
        this.startLocation = player.getLocation().clone();
    }

    public void play() {
        if (running) return;
        if (waypoints.isEmpty()) {
            player.sendMessage(CC.translate("&7[&cCinematic&7] &fLa cinematica no tiene puntos de ruta."));
            return;
        }
        running = true;
        currentTick = 0;
        triggeredEvents.clear();

        savePlayerState();
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setWalkSpeed(0.0f);
        player.setFlySpeed(0.0f);

        player.sendMessage(CC.translate("&7[&aCinematic&7] &fReproduciendo '&7" + cinematic.getId() + "&f'..."));

        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    stop();
                    return;
                }
                if (currentTick > cinematic.getDurationTicks()) {
                    stop();
                    return;
                }

                Location loc = calculatePosition(currentTick);
                if (loc != null) {
                    player.teleport(loc);
                }

                processEvents(currentTick);

                currentTick++;
            }
        };
        task.runTaskTimer(org.debentialc.Main.instance, 0L, 1L);
    }

    public void stop() {
        running = false;
        if (task != null) {
            task.cancel();
            task = null;
        }
        restorePlayerState();
        CinematicSessionManager.removePlayer(player);
        player.sendMessage(CC.translate("&7[&aCinematic&7] &fCinematica finalizada."));
    }

    public boolean isRunning() {
        return running;
    }

    public Cinematic getCinematic() {
        return cinematic;
    }

    public Player getPlayer() {
        return player;
    }

    private void savePlayerState() {
        prevAllowFlight = player.getAllowFlight();
        prevFlying = player.isFlying();
        prevWalkSpeed = player.getWalkSpeed();
        prevFlySpeed = player.getFlySpeed();
    }

    private void restorePlayerState() {
        player.setAllowFlight(prevAllowFlight);
        player.setFlying(prevFlying);
        player.setWalkSpeed(prevWalkSpeed);
        player.setFlySpeed(prevFlySpeed);
    }

    private Location calculatePosition(long tick) {
        if (waypoints.size() == 1) {
            return waypoints.get(0).getLocation();
        }

        long duration = cinematic.getDurationTicks();
        if (tick <= 0) return waypoints.get(0).getLocation();
        if (tick >= duration) return waypoints.get(waypoints.size() - 1).getLocation();

        int segment = 0;
        for (int i = 0; i < waypoints.size() - 1; i++) {
            Waypoint a = waypoints.get(i);
            Waypoint b = waypoints.get(i + 1);
            if (tick >= a.getTick() && tick <= b.getTick()) {
                segment = i;
                break;
            }
        }

        long segmentDuration = waypoints.get(segment + 1).getTick() - waypoints.get(segment).getTick();
        if (segmentDuration <= 0) return waypoints.get(segment + 1).getLocation();

        double t = (double) (tick - waypoints.get(segment).getTick()) / segmentDuration;
        t = applyEasing(t, waypoints.get(segment).getEasing());

        Location p0 = waypoints.get(Math.max(0, segment - 1)).getLocation();
        Location p1 = waypoints.get(segment).getLocation();
        Location p2 = waypoints.get(segment + 1).getLocation();
        Location p3 = waypoints.get(Math.min(waypoints.size() - 1, segment + 2)).getLocation();

        double x = catmullRom(p0.getX(), p1.getX(), p2.getX(), p3.getX(), t);
        double y = catmullRom(p0.getY(), p1.getY(), p2.getY(), p3.getY(), t);
        double z = catmullRom(p0.getZ(), p1.getZ(), p2.getZ(), p3.getZ(), t);
        float yaw = catmullRomAngle(p0.getYaw(), p1.getYaw(), p2.getYaw(), p3.getYaw(), t);
        float pitch = clampPitch((float) catmullRom(p0.getPitch(), p1.getPitch(), p2.getPitch(), p3.getPitch(), t));

        return new Location(p1.getWorld(), x, y, z, yaw, pitch);
    }

    private double catmullRom(double p0, double p1, double p2, double p3, double t) {
        return 0.5 * (
                (2 * p1) +
                (-p0 + p2) * t +
                (2 * p0 - 5 * p1 + 4 * p2 - p3) * t * t +
                (-p0 + 3 * p1 - 3 * p2 + p3) * t * t * t
        );
    }

    private float catmullRomAngle(float a0, float a1, float a2, float a3, double t) {
        double n0 = normalizeAngle(a0 - a1);
        double n1 = 0;
        double n2 = normalizeAngle(a2 - a1);
        double n3 = normalizeAngle(a3 - a1);
        double result = catmullRom(n0, n1, n2, n3, t);
        return (float) normalizeAngle(a1 + result);
    }

    private double normalizeAngle(double angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }

    private float clampPitch(float pitch) {
        if (pitch > 90) return 90;
        if (pitch < -90) return -90;
        return pitch;
    }

    private double applyEasing(double t, Waypoint.Easing easing) {
        switch (easing) {
            case EASE_IN:
                return t * t;
            case EASE_OUT:
                return 1 - (1 - t) * (1 - t);
            case EASE_IN_OUT:
                return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
            case LINEAR:
            default:
                return t;
        }
    }

    private void processEvents(long tick) {
        for (CinematicEvent event : events) {
            if (event.getTick() == tick && !triggeredEvents.contains(event)) {
                triggeredEvents.add(event);
                executeEvent(event);
            }
        }
    }

    private void executeEvent(CinematicEvent event) {
        switch (event.getType()) {
            case MESSAGE:
                player.sendMessage(CC.translate(event.getValue()));
                break;
            case TITLE:
                sendTitle(event.getValue(), event.getExtra());
                break;
            case SUBTITLE:
                if (event.getValue() != null && !event.getValue().isEmpty()) {
                    player.sendMessage(CC.translate("&7" + event.getValue()));
                }
                break;
            case SOUND:
                try {
                    Sound sound = Sound.valueOf(event.getValue().toUpperCase());
                    player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                } catch (Exception ignored) {
                }
                break;
            case COMMAND:
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), event.getValue().replace("%player%", player.getName()));
                break;
            case PARTICLE:
                try {
                    player.getWorld().playEffect(player.getLocation(), org.bukkit.Effect.valueOf(event.getValue().toUpperCase()), 0);
                } catch (Exception ignored) {
                }
                break;
            case EFFECT:
                try {
                    org.bukkit.potion.PotionEffectType type = org.bukkit.potion.PotionEffectType.getByName(event.getValue().toUpperCase());
                    if (type != null) {
                        player.addPotionEffect(new org.bukkit.potion.PotionEffect(type, 100, 1));
                    }
                } catch (Exception ignored) {
                }
                break;
            case TELEPORT:
                // Reservado
                break;
            case FREEZE:
            case UNFREEZE:
                break;
        }
    }

    private void sendTitle(String title, String subtitle) {
        if (title == null || title.isEmpty()) return;
        player.sendMessage(CC.translate(""));
        player.sendMessage(CC.translate(""));
        player.sendMessage(CC.translate("&f&l" + title));
        if (subtitle != null && !subtitle.isEmpty()) {
            player.sendMessage(CC.translate("&7" + subtitle));
        }
        player.sendMessage(CC.translate(""));
    }
}
