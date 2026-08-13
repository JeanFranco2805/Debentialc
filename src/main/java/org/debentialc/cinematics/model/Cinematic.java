package org.debentialc.cinematics.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Cinematic {

    private final String id;
    private final List<Waypoint> waypoints = new ArrayList<>();
    private final List<CinematicEvent> events = new ArrayList<>();

    public Cinematic(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void addWaypoint(Waypoint waypoint) {
        waypoints.add(waypoint);
        sortWaypoints();
    }

    public void addEvent(CinematicEvent event) {
        events.add(event);
        sortEvents();
    }

    public List<Waypoint> getWaypoints() {
        return new ArrayList<>(waypoints);
    }

    public List<CinematicEvent> getEvents() {
        return new ArrayList<>(events);
    }

    public long getDurationTicks() {
        if (waypoints.isEmpty()) return 0;
        return waypoints.get(waypoints.size() - 1).getTick();
    }

    private void sortWaypoints() {
        Collections.sort(waypoints, new Comparator<Waypoint>() {
            @Override
            public int compare(Waypoint o1, Waypoint o2) {
                return Long.compare(o1.getTick(), o2.getTick());
            }
        });
    }

    private void sortEvents() {
        Collections.sort(events, new Comparator<CinematicEvent>() {
            @Override
            public int compare(CinematicEvent o1, CinematicEvent o2) {
                return Long.compare(o1.getTick(), o2.getTick());
            }
        });
    }
}
