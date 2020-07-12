package ro.antiprotv.simplebikeometer;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;




class SettingsManager implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final MeterActivity clockActivity;

    private final SharedPreferences prefs;

    public SettingsManager(MeterActivity meterActivity) {
        this.clockActivity = meterActivity;
        prefs = PreferenceManager.getDefaultSharedPreferences(meterActivity);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        //BUTTONS
        int buttonIndex = -1;


    }


}
