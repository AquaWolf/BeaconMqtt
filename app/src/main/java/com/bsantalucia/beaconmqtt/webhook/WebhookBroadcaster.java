package com.bsantalucia.beaconmqtt.webhook;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.bsantalucia.beaconmqtt.MainActivity;
import com.bsantalucia.beaconmqtt.R;
import com.bsantalucia.beaconmqtt.db.log.LogPersistence;
import com.bsantalucia.beaconmqtt.json.JSONPayload;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
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
import static com.bsantalucia.beaconmqtt.settings.SettingsActivity.WEBHOOK_METHOD_KEY;
import static com.bsantalucia.beaconmqtt.settings.SettingsActivity.WEBHOOK_URL_KEY;


enum State {
    DONE,
    ERROR,
}

class Task extends AsyncTask<JSONObject, Void, Void> {
    private final WeakReference<Context> context;
    private final SharedPreferences defaultSharedPreferences;
    private final LogPersistence logPersistence;
    private static final String TAG = Task.class.getName();

    private static final String DEFAULT_CONTENT_TYPE = "application/json";
    private static final String DEFAULT_METHOD = "POST";

    private State state = null;

    public Task(final Context context) {
        this.context = new WeakReference<>(context);
        logPersistence = new LogPersistence(context);
        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    private void publishMessage(final JSONObject payload) {
        Boolean webhookEnabled = defaultSharedPreferences.getBoolean(WEBHOOK_ENABLE_KEY, true);

        if(!webhookEnabled) {
            return;
        }

        HttpURLConnection client = null;
        try {
            URL url = new URL(defaultSharedPreferences.getString(WEBHOOK_URL_KEY, ""));
            client = (HttpURLConnection) url.openConnection();
            client.setRequestMethod(defaultSharedPreferences.getString(WEBHOOK_METHOD_KEY, DEFAULT_METHOD));
            client.setRequestProperty("Content-Type", defaultSharedPreferences.getString(WEBHOOK_CONTENT_TYPE_KEY, DEFAULT_CONTENT_TYPE) + "; utf-8");
            client.setDoOutput(true);
            client.setDoInput(true);

            OutputStream outputStream = client.getOutputStream();
            byte[] input = payload.toString().getBytes("utf-8");
            outputStream.write(input, 0, input.length);

            // TODO: Why is it not sending the request if I remove this?
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    client.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println(inputLine);
            }
            in.close();

            state = State.DONE;
        }
        catch (MalformedURLException e){
            e.printStackTrace();
            logPersistence.saveNewLog(context.get().getString(R.string.webhook_invalid_url), "");
            Log.i(TAG, context.get().getString(R.string.webhook_invalid_url));
            state = State.ERROR;

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            state = State.ERROR;
        } catch (ProtocolException e) {
            e.printStackTrace();
            state = State.ERROR;
        } catch (IOException e) {
            e.printStackTrace();
            state = State.ERROR;
        } finally {
            if(client != null) {
                client.disconnect();
            }
        }
    }

    @Override
    protected Void doInBackground(JSONObject[] payloads) {

        for (int i = 0; i < payloads.length; i++) {
            publishMessage(payloads[i]);
        }

        return null;
    }

    protected void onPostExecute(Void x) {
        if (state == State.DONE) {
            Toast.makeText(context.get(), R.string.webhook_done, Toast.LENGTH_LONG).show();
        }
        if (state == State.ERROR) {
            Toast.makeText(context.get(), R.string.webhook_invalid_url, Toast.LENGTH_LONG).show();
        }
    }
};

public class WebhookBroadcaster {
    private static final String TAG = WebhookBroadcaster.class.getName();
    private final String DEFAULT_ENTER_PAYLOAD;
    private final String DEFAULT_EXIT_PAYLOAD;
    private final String DEFAULT_ENTER_DISTANCE_PAYLOAD;
    private final String DEFAULT_EXIT_DISTANCE_PAYLOAD;

    private final Context context;

    private final SharedPreferences defaultSharedPreferences;
    private final LogPersistence logPersistence;
    private final JSONPayload jsonPayload;

    public WebhookBroadcaster(final Context context) {
        this.context = context;

        DEFAULT_ENTER_PAYLOAD = context.getString(R.string.default_webhook_enter_payload);
        DEFAULT_EXIT_PAYLOAD = context.getString(R.string.default_webhook_exit_payload);
        DEFAULT_ENTER_DISTANCE_PAYLOAD = context.getString(R.string.default_webhook_enter_distance_payload);
        DEFAULT_EXIT_DISTANCE_PAYLOAD = context.getString(R.string.default_webhook_exit_distance_payload);

        logPersistence = new LogPersistence(context);
        jsonPayload = new JSONPayload(context);
        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void publishEnterMessage(String uuid, String mac, String major, String minor) {
        String extraPayload = defaultSharedPreferences.getString(WEBHOOK_ENTER_PAYLOAD_KEY, DEFAULT_ENTER_PAYLOAD);
        Task task = new Task(context);
        task.execute(mergeJson(jsonPayload.getMessagePayload(uuid, mac, major, minor), extraPayload));
    }

    public void publishExitMessage(String uuid, String mac, String major, String minor) {
        String extraPayload = defaultSharedPreferences.getString(WEBHOOK_EXIT_PAYLOAD_KEY, DEFAULT_EXIT_PAYLOAD);
        Task task = new Task(context);
        task.execute(mergeJson(jsonPayload.getMessagePayload(uuid, mac, major, minor), extraPayload));
    }

    public void publishEnterDistanceMessage(String uuid, String mac, String major, String minor, double distance) {
        String extraPayload = defaultSharedPreferences.getString(WEBHOOK_ENTER_DISTANCE_PAYLOAD_KEY, DEFAULT_ENTER_DISTANCE_PAYLOAD);
        Task task = new Task(context);
        task.execute(mergeJson(jsonPayload.getMessagePayload(uuid, mac, major, minor, distance), extraPayload));
    }

    public void publishExitDistanceMessage(String uuid, String mac, String major, String minor, double distance) {
        String extraPayload = defaultSharedPreferences.getString(WEBHOOK_EXIT_DISTANCE_PAYLOAD_KEY, DEFAULT_EXIT_DISTANCE_PAYLOAD);
        Task task = new Task(context);
        task.execute(mergeJson(jsonPayload.getMessagePayload(uuid, mac, major, minor, distance), extraPayload));
    }

    private JSONObject mergeJson(JSONObject payload, String extraPayload) {
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
        return payload;
    }
}
