package org.debentialc.cinematics.model;

public class CinematicEvent {

    private final long tick;
    private final Type type;
    private final String value;
    private final String extra;

    public CinematicEvent(long tick, Type type, String value) {
        this(tick, type, value, null);
    }

    public CinematicEvent(long tick, Type type, String value, String extra) {
        this.tick = tick;
        this.type = type;
        this.value = value;
        this.extra = extra;
    }

    public long getTick() {
        return tick;
    }

    public Type getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public String getExtra() {
        return extra;
    }

    public enum Type {
        MESSAGE,
        TITLE,
        SUBTITLE,
        SOUND,
        COMMAND,
        PARTICLE,
        EFFECT,
        TELEPORT,
        FREEZE,
        UNFREEZE
    }
}
