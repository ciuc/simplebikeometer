package ro.antiprotv.simplebikeometer;

import java.util.ArrayList;
import java.util.LinkedList;

public class TimeLocations {
    private LinkedList<TimeLocation> timeLocations = new LinkedList<>();
    private TrackingService trackingService;

    TimeLocations(TrackingService service) {
        trackingService = service;

    }
    public float getDistance() {
        return distance;
    }

    int distance = 0;

    public void add(TimeLocation timeLocation) {
        timeLocations.addLast(timeLocation);
        if (timeLocations.size() ==3) {
            distance += timeLocations.get(1).getLocation().distanceTo(timeLocation.getLocation());
            trackingService.setDistance(distance);
            timeLocations.removeFirst();
        }
    }

    public TimeLocation getFirst() {
        return timeLocations.peekFirst();
    }

    public TimeLocation getLast() {
        return timeLocations.peekLast();
    }

    public int size(){
        return timeLocations.size();
    }
}
