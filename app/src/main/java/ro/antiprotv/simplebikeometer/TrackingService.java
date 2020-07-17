package ro.antiprotv.simplebikeometer;

import android.location.Location;
import android.util.Log;

public class TrackingService {
    int time;
    boolean moving;

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public double getDistance() {
        return distance / 1000;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    double distance;

    private Thread timerThread = new Thread() {
        @Override
        public void run() {
            while(true) {
                if (moving) {
                    ++time;
                }
                try {
                    sleep(1000);
                } catch (InterruptedException e) {

                }

            }
        }
    };

    TrackingService() {
        timerThread.start();
    }
    public void startTimer() {
        Log.d("timer", "start");
        moving = true;
    }

    public void stopTimer() {
        Log.d("timer", "stop");
        moving = false;
    }

    public void addDistance(Location locationA, Location locationB) {
        this.distance += locationA.distanceTo(locationB);
    }

    public double getTripAverageSpeed() {
        if (time != 0) {
            return distance / time /1000*3600;
        }
        return 0;
    }
}
