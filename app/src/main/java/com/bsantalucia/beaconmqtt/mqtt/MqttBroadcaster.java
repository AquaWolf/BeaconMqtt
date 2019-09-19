
package com.bsantalucia.beaconmqtt.mqtt;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.bsantalucia.beaconmqtt.R;
import com.bsantalucia.beaconmqtt.db.log.LogPersistence;
import com.bsantalucia.beaconmqtt.json.JSONPayload;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import static com.bsantalucia.beaconmqtt.settings.SettingsActivity.GENERAL_LOG_KEY;
import static com.bsantalucia.beaconmqtt.settings.SettingsActivity.MQTT_ENABLE_KEY;
import static com.bsantalucia.beaconmqtt.settings.SettingsActivity.MQTT_ENTER_DISTANCE_TOPIC_KEY;
import static com.bsantalucia.beaconmqtt.settings.SettingsActivity.MQTT_ENTER_TOPIC_KEY;
import static com.bsantalucia.beaconmqtt.settings.SettingsActivity.MQTT_EXIT_DISTANCE_TOPIC_KEY;
import static com.bsantalucia.beaconmqtt.settings.SettingsActivity.MQTT_EXIT_TOPIC_KEY;
import static com.bsantalucia.beaconmqtt.settings.SettingsActivity.MQTT_PORT_KEY;
import static com.bsantalucia.beaconmqtt.settings.SettingsActivity.MQTT_SERVER_KEY;
import static com.bsantalucia.beaconmqtt.settings.SettingsActivity.MQTT_USER_KEY;
import static com.bsantalucia.beaconmqtt.settings.SettingsActivity.MQTT_PASS_KEY;


interface ConnectCallback {
    void run(MqttAndroidClient mqttAndroidClient);
}

public class MqttBroadcaster {

    private static final String TAG = MqttBroadcaster.class.getName();
    private static final String CLIENT_ID = "AndroidMqttBeacon";
    private static final String DEFAULT_ENTER_TOPIC = "beacon/enter";
    private static final String DEFAULT_EXIT_TOPIC = "beacon/exit";
    private static final String DEFAULT_ENTER_DISTANCE_TOPIC = "beacon/enter/distance";
    private static final String DEFAULT_EXIT_DISTANCE_TOPIC = "beacon/exit/distance";
    private final Context context;
    private final JSONPayload jsonPayload;

    private SharedPreferences.OnSharedPreferenceChangeListener listener;
    private final SharedPreferences defaultSharedPreferences;
    private final LogPersistence logPersistence;

    public MqttBroadcaster(final Context context) {
        this.context = context;
        logPersistence = new LogPersistence(context);
        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        jsonPayload = new JSONPayload(context);
        registerSettingsChangeListener();
    }

    public void publishEnterMessage(String uuid, String mac, String major, String minor) {
        String preferenceEnterTopic = defaultSharedPreferences.getString(MQTT_ENTER_TOPIC_KEY, DEFAULT_ENTER_TOPIC);
        publishMessage(jsonPayload.getMessagePayload(uuid, mac, major, minor), preferenceEnterTopic);
    }

    public void publishExitMessage(String uuid, String mac, String major, String minor) {
        String preferenceExitTopic = defaultSharedPreferences.getString(MQTT_EXIT_TOPIC_KEY, DEFAULT_EXIT_TOPIC);
        publishMessage(jsonPayload.getMessagePayload(uuid, mac, major, minor), preferenceExitTopic);
    }

    public void publishEnterDistanceMessage(String uuid, String mac, String major, String minor, double distance) {
        String preferenceDistanceTopic = defaultSharedPreferences.getString(MQTT_ENTER_DISTANCE_TOPIC_KEY, DEFAULT_ENTER_DISTANCE_TOPIC);
        publishMessage(jsonPayload.getMessagePayload(uuid, mac, major, minor, distance), preferenceDistanceTopic);
    }

    public void publishExitDistanceMessage(String uuid, String mac, String major, String minor, double distance) {
        String preferenceDistanceTopic = defaultSharedPreferences.getString(MQTT_EXIT_DISTANCE_TOPIC_KEY, DEFAULT_EXIT_DISTANCE_TOPIC);
        publishMessage(jsonPayload.getMessagePayload(uuid, mac, major, minor, distance), preferenceDistanceTopic);
    }

    private void registerSettingsChangeListener() {
        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (MQTT_SERVER_KEY.equals(key) || MQTT_PORT_KEY.equals(key)) {
                    connectToMqttServer();
                }
            }
        };
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(listener);
    }

    private void connectToMqttServer() {
        connectToMqttServer(new ConnectCallback() {
            @Override
            public void run(MqttAndroidClient mqttAndroidClient) {

            }
        });
    }

    private void connectToMqttServer(final ConnectCallback callback) {
        String mqttServer = defaultSharedPreferences.getString(MQTT_SERVER_KEY, null);
        String mqttPort = defaultSharedPreferences.getString(MQTT_PORT_KEY, null);
        String mqttUser = defaultSharedPreferences.getString(MQTT_USER_KEY, null);
        String mqttPassword = defaultSharedPreferences.getString(MQTT_PASS_KEY, null);
        Boolean mqttEnabled = defaultSharedPreferences.getBoolean(MQTT_ENABLE_KEY, true);

        final MqttAndroidClient mqttAndroidClient;

        if(!mqttEnabled) {
            return;
        }

        if (mqttServer != null && mqttPort != null) {
            final String serverUri = "tcp://" + mqttServer + ":" + mqttPort;

            Log.d("Mqtt connect", "Connecting...");


            mqttAndroidClient = new MqttAndroidClient(context, serverUri, CLIENT_ID);

            MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
            mqttConnectOptions.setAutomaticReconnect(true);
            mqttConnectOptions.setCleanSession(false);

            if (TextUtils.isEmpty(mqttUser)||TextUtils.isEmpty(mqttPassword)) {
                Log.d("Mqtt connect", "Login without user/pass");
            } else {
                Log.d("Mqtt connect", "Login with user/pass");
                mqttConnectOptions.setUserName(mqttUser);
                mqttConnectOptions.setPassword(mqttPassword.toCharArray());
            }
            Toast.makeText(context, R.string.connecting_to_mqtt_server, Toast.LENGTH_SHORT).show();
            try {
                mqttAndroidClient.connect(mqttConnectOptions, context, new IMqttActionListener() {

                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Toast.makeText(context, R.string.connection_successful, Toast.LENGTH_SHORT).show();
                        Log.d("Mqtt", "Connection successful");
                        DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                        disconnectedBufferOptions.setBufferEnabled(true);
                        disconnectedBufferOptions.setBufferSize(100);
                        disconnectedBufferOptions.setPersistBuffer(false);
                        disconnectedBufferOptions.setDeleteOldestMessages(false);
                        mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                        callback.run(mqttAndroidClient);
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        logPersistence.saveNewLog(context.getString(R.string.failed_to_connect_mqtt_server, serverUri), "");
                        Toast.makeText(context, context.getString(R.string.failed_to_connect_mqtt_server, serverUri), Toast.LENGTH_LONG).show();
                        Log.e(TAG, context.getString(R.string.failed_to_connect_mqtt_server, serverUri), exception);
                    }
                });
            } catch (MqttException ex){
                ex.printStackTrace();
            }

        } else {
            logPersistence.saveNewLog(context.getString(R.string.mqtt_missing_server_or_port), "");
            Toast.makeText(context, R.string.mqtt_missing_server_or_port, Toast.LENGTH_LONG).show();
            Log.i(TAG, context.getString(R.string.mqtt_missing_server_or_port));
        }
    }

    private void publishMessage(final JSONObject payload, final String topic) {
        connectToMqttServer(new ConnectCallback() {
            @Override
            public void run(MqttAndroidClient mqttAndroidClient) {
                MqttMessage mqttMessage = new MqttMessage();
                mqttMessage.setPayload(payload.toString().getBytes());
                try {
                    mqttAndroidClient.publish(topic, mqttMessage);
                } catch (MqttException ex){
                    ex.printStackTrace();
                }
                boolean logEvent = defaultSharedPreferences.getBoolean(GENERAL_LOG_KEY, false);
                if (logEvent) {
                    String logMessage = context.getString(R.string.published_mqtt_message_to_topic, mqttMessage, topic);
                    logPersistence.saveNewLog(logMessage, "");
                }
            }
        });
    }
}
