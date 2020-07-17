/*
  Copyright Cristian "ciuc" Starasciuc 2016
  <p/>
  Licensed under the Apache license 2.0
  <p/>
  cristi.ciuc@gmail.com
 */
package ro.antiprotv.simplebikeometer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.LocationManager;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

//import timber.log.Timber;

/**
 * Main Activity. Just displays the clock and buttons
 */
public class MeterActivity extends AppCompatActivity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;
    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private static final String TAG_STATE = "MeterActivity | State: %s";
    private final Handler mHideHandler = new Handler();
    private final java.util.List<String> mUrls = new ArrayList<>();
    String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
    int permissionRequestCode = 1;
    ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
    private SharedPreferences prefs;
    private DataUpdater speedUpdater;
    private LocationManager locationManager;
    private android.view.View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(android.view.View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final android.view.View.OnTouchListener mDelayHideTouchListener = new android.view.View.OnTouchListener() {
        @Override
        public boolean onTouch(android.view.View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }

    };
    private RelativeLayout mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                    | android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // UTILITY
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static String formatTime(int time) {
        int hours = time / 3600;
        int minutes = (time - hours * 3600 )/ 60;
        int seconds = time - minutes * 60;

        return String.format("%d:%s:%s", hours, padWithZeros(minutes), padWithZeros(seconds));
    }

    private static String padWithZeros(int number) {
        if (new Integer(number).toString().length() == 1) {
            return "0" + new Integer(number);
        }
        return new Integer(number).toString();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED &&
                grantResults[1] != PackageManager.PERMISSION_GRANTED) {
            android.app.AlertDialog.Builder dialogBuilder = new android.app.AlertDialog.Builder(this);
            dialogBuilder.setTitle("Permissions needed");
            dialogBuilder.setPositiveButton(R.string.dialog_button_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            dialogBuilder.show();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // State methods
    ///////////////////////////////////////////////////////////////////////////
    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Timber.d(TAG_STATE, "onCreate");
        //SOME INITIALIZATIONS
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, perms, permissionRequestCode);
        }

        //init the location manager
        if (locationManager == null) {
            locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        }

        int currentOrientation = getResources().getConfiguration().orientation;
        //if reverse enabled we load the reverse layout
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            setContentView(R.layout.activity_main_portrait);
        } else if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
            setContentView(R.layout.activity_main_portrait);
        } else {
            setContentView(R.layout.activity_main_portrait);
        }

        /*
        LEAVE THIS HERE, SO IF WE ENABLE TIMBER FOR DEBUGGING, WE JUST UNCOMMENT
         */
        //Set up Timber
        //if (BuildConfig.DEBUG) {
        //    Timber.plant(new Timber.DebugTree());
        //}
        mVisible = true;
        mControlsView = findViewById(R.id.mainLayout);
        mContentView = findViewById(R.id.fullscreen_content);

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View view) {
                toggle();
            }
        });

        displayDialogsOnOpen();

        //Thread - clock
        //Thread for communicating with the ui handler
        //We start it here , and we sendMessage to the threadHandler in onStart (we might have a race and get threadHandler null if we try it here)
        speedUpdater = new DataUpdater(this, locationManager);

        preferenceChangeListener = new SettingsManager(this);

    }


    @Override
    protected void onPostCreate(android.os.Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        //Timber.d(TAG_STATE, "onPostCreate");
        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    @Override
    protected void onStart() {
        //Timber.d(TAG_STATE, "onStart");
        super.onStart();
        if (!speedUpdater.getThreadHandler().hasMessages(0)) {
            speedUpdater.getThreadHandler().sendEmptyMessage(0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Timber.d(TAG_STATE, "onResume");
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //Timber.d(TAG_STATE, "onPause");
    }

    @Override
    protected void onStop() {
        //Timber.d(TAG_STATE, "onStop");
        super.onStop();
        speedUpdater.setSemaphore(false);
    }


    @Override
    protected void onDestroy() {
        //Timber.d(TAG_STATE, "onDestroy");

        speedUpdater.setThreadHandler(null);
        speedUpdater = null;
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        super.onDestroy();
    }

    @Override
    protected void onRestart() {
        //Timber.d(TAG_STATE, "onRestart");
        super.onRestart();
        speedUpdater.setSemaphore(true);
    }


    private void displayDialogsOnOpen() {
        if (prefs.getBoolean("FIRST_TIME", true)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Check out the \"About\" section to see what's new and to find out what you can do with this thing.")
                    .setTitle("Thanks for using this app!").setPositiveButton(R.string.dialog_button_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    prefs.edit().putBoolean("FIRST_TIME", false).apply();
                }
            });
            AlertDialog dialog = builder.create();

            dialog.show();
        }

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // MENU
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        MenuInflater inflater = getMenuInflater();
        //inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        return true;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Delaying removal of nav bar (android studio default stuff)
    ///////////////////////////////////////////////////////////////////////////
    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    public void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(android.view.View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    private class OnHelpClickListener implements android.view.View.OnClickListener {
        @Override
        public void onClick(android.view.View view) {
            android.app.AlertDialog.Builder dialogBuilder = new android.app.AlertDialog.Builder(view.getContext());
            dialogBuilder.setTitle("Tips & Tricks");
            dialogBuilder.setView(LayoutInflater.from(view.getContext()).inflate(R.layout.dialog_main_help, null));
            dialogBuilder.setPositiveButton(R.string.dialog_button_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            dialogBuilder.show();
        }
    }


}
