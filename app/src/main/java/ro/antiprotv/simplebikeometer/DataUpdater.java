package ro.antiprotv.simplebikeometer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.Date;

/**
 * Thread to manage the clock (update the clock and move it)
 */
class DataUpdater extends Thread implements LocationListener {
    private final TextView mSpeedView;
    private final TextView mTimerView;
    private final TextView mAverageSpeedView;
    private final TextView tripDistanceView;
    LocationManager locationManager;
    boolean moving;
    double speed = 0;
    TrackingService trackingService = new TrackingService();
    long startTime = new Date().getTime();
    private TimeLocations timeLocations = new TimeLocations(trackingService);
    //We create this ui handler to update the data
    //We need this in order to not block the UI
    private final Handler uiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        Log.d("uiHandler", new Integer(timeLocations.size()).toString() );

            if (timeLocations.size() == 2) {

                long timeDiff = new Date().getTime() - timeLocations.getLast().getTime();
                mTimerView.setText(MeterActivity.formatTime(trackingService.getTime()));
                if (timeDiff < 2000) {
                    mSpeedView.setText(String.format("%.2f", timeLocations.getLast().getLocation().getSpeed() / 1000 * 3600));
                    tripDistanceView.setText(String.format("%.2f", trackingService.getDistance()));
                    mAverageSpeedView.setText(String.format("%.2f", trackingService.getTripAverageSpeed()));
                    moving = true;
                } else {
                    mSpeedView.setText("0.00");
                    moving = false;
                }
            } else {
                mSpeedView.setText("0.00");
                moving = false;
            }

        }
    };
    private boolean semaphore = true;
    //Threading stuff
    private Handler threadHandler = null;

    @SuppressLint("MissingPermission")
    DataUpdater(Context ctx, LocationManager locationManager) {
        AppCompatActivity activity = (AppCompatActivity) ctx;
        this.mSpeedView = activity.findViewById(R.id.speed);
        this.mTimerView = activity.findViewById(R.id.timer);
        this.tripDistanceView = activity.findViewById(R.id.trip_distance);
        this.mAverageSpeedView = activity.findViewById(R.id.average_speed);
        mSpeedView.setText("0.00");

        this.locationManager = locationManager;

        if (hasPermision()) {

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 1f, this);// new LocationChangeManager(this));
            //Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        } else {
            Toast.makeText(ctx, "Please provide location permissions, otherwise I can't function...", Toast.LENGTH_LONG);
        }

        this.start();
    }

    public boolean isMoving() {
        return moving;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    private boolean hasPermision() {
        if (ActivityCompat.checkSelfPermission(mSpeedView.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(mSpeedView.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return true;
    }

    public Handler getThreadHandler() {
        return threadHandler;
    }

    public void setThreadHandler(Handler threadHandler) {
        this.threadHandler = threadHandler;
    }


    @Override
    public void run() {
        Looper.prepare();

        threadHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                while (semaphore) {
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                    }
                    if (isMoving()) {
                        trackingService.startTimer();
                    } else {
                        trackingService.stopTimer();
                    }
                    uiHandler.sendEmptyMessage(0);
                }
            }
        };
        Looper.loop();
    }


    public void setSemaphore(boolean semaphore) {
        this.semaphore = semaphore;
    }

    @Override
    public void onLocationChanged(@NonNull final Location location) {
        Log.d("locationChange", location.toString());
        if (location != null) {
            timeLocations.add(new TimeLocation(new Date().getTime(), location));
        } else {
            speed = 0;
        }
    }
}
