package ro.antiprotv.simplebikeometer;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class LocationChangeManager implements LocationListener {
    DataUpdater speedUpdater;
    Map<Long, Location> locations = new HashMap<>();
    LocationChangeManager(DataUpdater speedUpdater) {
        this.speedUpdater = speedUpdater;
    }
    @Override
    public void onLocationChanged(@NonNull Location location) {
        Log.v("location", "onLocationChanged " + location.getSpeed());
        if (location != null) {
            locations.put(new Date().getTime(), location);
        } else {
            speedUpdater.setSpeed(0);
        }

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.v("location", "onStatusChanged " + status);
    }


}
