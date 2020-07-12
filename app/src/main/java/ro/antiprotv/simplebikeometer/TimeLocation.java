package ro.antiprotv.simplebikeometer;

import android.location.Location;

class TimeLocation {
    public Long getTime() {
        return time;
    }

    public Location getLocation() {
        return location;
    }

    Long time;
    Location location;
    TimeLocation(Long time, Location location) {
        this.time = time;
        this.location = location;
    }

}
