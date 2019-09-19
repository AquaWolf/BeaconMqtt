package com.bsantalucia.beaconmqtt.json;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

import com.bsantalucia.beaconmqtt.R;
import com.bsantalucia.beaconmqtt.db.log.LogPersistence;

import org.json.JSONException;
import org.json.JSONObject;

public class JSONPayload {
    private static final String TAG = JSONPayload.class.getName();
    private final Context context;

    private final LogPersistence logPersistence;

    public JSONPayload(final Context context) {
        this.context = context;
        logPersistence = new LogPersistence(context);
    }

    public JSONObject getMessagePayload(String uuid, String mac, String major, String minor, double distance) {
        JSONObject jsonObject = getMessagePayload(uuid, mac, major, minor);
        try {
            jsonObject.put("distance", distance);
        } catch (JSONException e) {
            logPersistence.saveNewLog(context.getString(R.string.error_creating_distance_payload, uuid, mac, major, minor, distance), "");
            Log.e(TAG, context.getString(R.string.error_creating_distance_payload, uuid, mac, major, minor, distance), e);
        }
        return jsonObject;
    }

    public JSONObject getMessagePayload(String uuid, String mac, String major, String minor) {
        String androidId = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("uuid", uuid);
            jsonObject.put("mac", mac);
            jsonObject.put("major", major);
            jsonObject.put("minor", minor);
            jsonObject.put("androidId", androidId);
        } catch (JSONException e) {
            logPersistence.saveNewLog(context.getString(R.string.error_creating_payload, uuid, mac, major, minor), "");
            Log.e(TAG, context.getString(R.string.error_creating_payload, uuid, mac, major, minor), e);
        }

        return jsonObject;
    }
}
