package com.bsantalucia.beaconmqtt.webhook;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.bsantalucia.beaconmqtt.R;
import com.bsantalucia.beaconmqtt.db.log.LogPersistence;
import com.bsantalucia.beaconmqtt.json.JSONPayload;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.bsantalucia.beaconmqtt.settings.SettingsActivity.WEBHOOK_CONTENT_TYPE_KEY;
import static com.bsantalucia.beaconmqtt.settings.SettingsActivity.WEBHOOK_ENABLE_KEY;
import static com.bsantalucia.beaconmqtt.settings.SettingsActivity.WEBHOOK_ENTER_DISTANCE_PAYLOAD_KEY;
import static com.bsantalucia.beaconmqtt.settings.SettingsActivity.WEBHOOK_ENTER_PAYLOAD_KEY;
import static com.bsantalucia.beaconmqtt.settings.SettingsActivity.WEBHOOK_EXIT_DISTANCE_PAYLOAD_KEY;
import static com.bsantalucia.beaconmqtt.settings.SettingsActivity.WEBHOOK_EXIT_PAYLOAD_KEY;

public class WebhookBroadcaster {
    private static final String TAG = WebhookBroadcaster.class.getName();
    private static final String DEFAULT_ENTER_PAYLOAD = "{}";
    private static final String DEFAULT_EXIT_PAYLOAD = "{}";
    private static final String DEFAULT_ENTER_DISTANCE_PAYLOAD = "{}";
    private static final String DEFAULT_EXIT_DISTANCE_PAYLOAD = "{}";
    private static final String DEFAULT_CONTENT_TYPE = "application/json";
    private static final String DEFAULT_METHOD = "POST";

    private final Context context;

    private final SharedPreferences defaultSharedPreferences;
    private final LogPersistence logPersistence;
    private final JSONPayload jsonPayload;

    public WebhookBroadcaster(final Context context) {
        this.context = context;
        logPersistence = new LogPersistence(context);
        jsonPayload = new JSONPayload(context);
        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void publishEnterMessage(String uuid, String mac, String major, String minor) {
        String extraPayload = defaultSharedPreferences.getString(WEBHOOK_ENTER_PAYLOAD_KEY, DEFAULT_ENTER_PAYLOAD);
        publishMessage(jsonPayload.getMessagePayload(uuid, mac, major, minor), extraPayload);
    }

    public void publishExitMessage(String uuid, String mac, String major, String minor) {
        String extraPayload = defaultSharedPreferences.getString(WEBHOOK_EXIT_PAYLOAD_KEY, DEFAULT_EXIT_PAYLOAD);
        publishMessage(jsonPayload.getMessagePayload(uuid, mac, major, minor), extraPayload);
    }

    public void publishEnterDistanceMessage(String uuid, String mac, String major, String minor, double distance) {
        String extraPayload = defaultSharedPreferences.getString(WEBHOOK_ENTER_DISTANCE_PAYLOAD_KEY, DEFAULT_ENTER_DISTANCE_PAYLOAD);
        publishMessage(jsonPayload.getMessagePayload(uuid, mac, major, minor, distance), extraPayload);
    }

    public void publishExitDistanceMessage(String uuid, String mac, String major, String minor, double distance) {
        String extraPayload = defaultSharedPreferences.getString(WEBHOOK_EXIT_DISTANCE_PAYLOAD_KEY, DEFAULT_EXIT_DISTANCE_PAYLOAD);
        publishMessage(jsonPayload.getMessagePayload(uuid, mac, major, minor, distance), extraPayload);
    }

    private void publishMessage(final JSONObject payload, final String extraPayload) {
        Boolean webhookEnabled = defaultSharedPreferences.getBoolean(WEBHOOK_ENABLE_KEY, true);

        if(!webhookEnabled) {
            return;
        }

        JSONObject extra = null;
        try {
            extra = new JSONObject(extraPayload);
        } catch (JSONException e) {
            logPersistence.saveNewLog(context.getString(R.string.webhook_invalid_payload), "");
            Toast.makeText(context, R.string.webhook_invalid_payload, Toast.LENGTH_LONG).show();
            Log.i(TAG, context.getString(R.string.webhook_invalid_payload));
        }
        if(extra != null) {
            Iterator<String> keys = extra.keys();

            while(keys.hasNext()) {
                String key = keys.next();
                try {
                    payload.put(key, extra.get(key));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        HttpURLConnection client = null;
        try {
            URL url = new URL(defaultSharedPreferences.getString(WEBHOOK_CONTENT_TYPE_KEY, ""));
            client = (HttpURLConnection) url.openConnection();
            client.setRequestMethod(defaultSharedPreferences.getString(WEBHOOK_CONTENT_TYPE_KEY, DEFAULT_METHOD));
            client.setRequestProperty("Content-Type", defaultSharedPreferences.getString(WEBHOOK_CONTENT_TYPE_KEY, DEFAULT_CONTENT_TYPE) + "; utf-8");
            client.setDoOutput(true);

            try(OutputStream outputStream = client.getOutputStream()) {
                byte[] input = payload.toString().getBytes("utf-8");
                outputStream.write(input, 0, input.length);
            }
        }
        catch (MalformedURLException e){
            e.printStackTrace();
            logPersistence.saveNewLog(context.getString(R.string.webhook_invalid_url), "");
            Toast.makeText(context, R.string.webhook_invalid_url, Toast.LENGTH_LONG).show();
            Log.i(TAG, context.getString(R.string.webhook_invalid_url));

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(client != null) {
                client.disconnect();
            }
        }
    }

    private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
        StringBuilder feedback = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()){
            if (first)
                first = false;
            else
                feedback.append("&");

            feedback.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            feedback.append("=");
            feedback.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return feedback.toString();
    }
}
