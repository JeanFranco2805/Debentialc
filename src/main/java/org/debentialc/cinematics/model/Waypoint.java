package org.debentialc.cinematics.model;

import org.bukkit.Location;

public class Waypoint {

    private final Location location;
    private final long tick;
    private final Easing easing;

    public Waypoint(Location location, long tick) {
        this(location, tick, Easing.LINEAR);
    }

    public Waypoint(Location location, long tick, Easing easing) {
        this.location = location.clone();
        this.tick = tick;
        this.easing = easing != null ? easing : Easing.LINEAR;
    }

    public Location getLocation() {
        return location.clone();
    }

    public long getTick() {
        return tick;
    }

    public Easing getEasing() {
        return easing;
    }

    public enum Easing {
        LINEAR,
        EASE_IN,
        EASE_OUT,
        EASE_IN_OUT
    }
}
