package com.bsantalucia.beaconmqtt;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.bsantalucia.beaconmqtt.db.beacon.BeaconPersistence;
import com.bsantalucia.beaconmqtt.db.beacon.BeaconResult;
import com.bsantalucia.beaconmqtt.db.log.LogPersistence;
import com.bsantalucia.beaconmqtt.db.pid.PidPersistence;
import com.bsantalucia.beaconmqtt.mqtt.MqttBroadcaster;
import com.bsantalucia.beaconmqtt.webhook.WebhookBroadcaster;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static com.bsantalucia.beaconmqtt.settings.SettingsActivity.BEACON_MINIMUM_DISTANCE_KEY;
import static com.bsantalucia.beaconmqtt.settings.SettingsActivity.BEACON_MONITOR_DISTANCE_KEY;
import static com.bsantalucia.beaconmqtt.settings.SettingsActivity.BEACON_NOTIFICATIONS_ENTER_KEY;
import static com.bsantalucia.beaconmqtt.settings.SettingsActivity.BEACON_PERIOD_BETWEEN_SCANS_KEY;
import static com.bsantalucia.beaconmqtt.settings.SettingsActivity.BEACON_SCAN_PERIOD_KEY;
import static com.bsantalucia.beaconmqtt.settings.SettingsActivity.GENERAL_ENABLE_MONITORING;
import static org.altbeacon.beacon.BeaconManager.DEFAULT_BACKGROUND_BETWEEN_SCAN_PERIOD;
import static org.altbeacon.beacon.BeaconManager.DEFAULT_BACKGROUND_SCAN_PERIOD;



public class BeaconApplication extends Application implements BootstrapNotifier {
    private BackgroundPowerSaver backgroundPowerSaver;
    private static final String TAG = BeaconApplication.class.getName();

    private RegionBootstrap regionBootstrap = null; // Needs to be here even if not used (altbeacon requirement)
    private SharedPreferences.OnSharedPreferenceChangeListener listener; // Needs to be here even if not used (altbeacon requirement)
    private BeaconPersistence beaconPersistence = new BeaconPersistence(this);
    private LogPersistence logPersistence = new LogPersistence(this);
    private MqttBroadcaster mqttBroadcaster = null;
    private WebhookBroadcaster webhookBroadcaster = null;
    private BeaconInRangeListener beaconInRangeListener = null;

    private String NOTIFICATION_CHANNEL_ID = "beacon_mqtt_channel_id_01";
    private NotificationChannel notificationChannel;
    private NotificationManager notificationManager;
    private BeaconManager beaconManager;
    private PidPersistence pidPersistence;
    private SharedPreferences defaultSharedPreferences;
    private double minimumDistance;

    private enum MESSAGE_TYPE { ENTER, EXIT, ENTER_DISTANCE, EXIT_DISTANCE };

    private final String pid = UUID.randomUUID().toString();

    @Override
    public void onTerminate() {
        super.onTerminate();
        if(regionBootstrap != null) {
            regionBootstrap.disable();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // this avoid multiple instance bug
        pidPersistence = new PidPersistence(this);
        pidPersistence.savePid(pid);

        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        minimumDistance = Long.parseLong(defaultSharedPreferences.getString(BEACON_MINIMUM_DISTANCE_KEY, "5"));

        setUpBeaconManager();
        backgroundPowerSaver = new BackgroundPowerSaver(this);

        setUpBroadcaster();
        setUpNotification();

        setUpSettingsChangedListener();

        setUpRangeNotifier();
        setUpMonitoring();
    }

    public void restartBeaconSearch() {
        setUpMonitoring();
    }

    private void setUpBroadcaster() {
        if (mqttBroadcaster == null) {
            mqttBroadcaster = new MqttBroadcaster(this);
        }
        if (webhookBroadcaster == null) {
            webhookBroadcaster = new WebhookBroadcaster(this);
        }
    }

    private void setUpNotification() {
        notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_DEFAULT);
        notificationChannel.setDescription(getString(R.string.notification_channel_description));
        notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
        notificationChannel.enableVibration(true);
        notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(notificationChannel);
    }

    @NonNull
    private void setUpBeaconManager() {
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19"));
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("x,s:0-1=feaa,m:2-2=20,d:3-3,d:4-5,d:6-7,d:8-11,d:12-15"));
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("s:0-1=feaa,m:2-2=10,p:3-3:-41,i:4-20v"));
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:0-3=4c000215,i:4-19,i:20-21,i:22-23,p:24-24"));

        Long beaconPeriodBetweenScans = Long.parseLong(defaultSharedPreferences.getString(BEACON_PERIOD_BETWEEN_SCANS_KEY, Long.valueOf(DEFAULT_BACKGROUND_BETWEEN_SCAN_PERIOD).toString()));
        beaconManager.setBackgroundBetweenScanPeriod(beaconPeriodBetweenScans);

        Long beaconScanPeriod = Long.parseLong(defaultSharedPreferences.getString(BEACON_SCAN_PERIOD_KEY, Long.valueOf(DEFAULT_BACKGROUND_SCAN_PERIOD).toString()));
        beaconManager.setBackgroundScanPeriod(beaconScanPeriod);
    }

    private void setUpRangeNotifier() {
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                for (Beacon beacon: beacons) {
                    if(region.getId1() == null) {
                        continue;
                    }
                    double distance = beacon.getDistance();

                    if (distance <= minimumDistance) {
                        didEnterDistance(beacon);
                    } else if (distance > minimumDistance) {
                        didExitDistance(beacon);
                    }
                }
            }
        });
    }

    private void setUpSettingsChangedListener() {
        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (BEACON_PERIOD_BETWEEN_SCANS_KEY.equals(key)) {
                    Long beaconPeriodBetweenScans = Long.parseLong(sharedPreferences.getString(key, Long.valueOf(DEFAULT_BACKGROUND_BETWEEN_SCAN_PERIOD).toString()));
                    beaconManager.setBackgroundBetweenScanPeriod(beaconPeriodBetweenScans);
                } else if (BEACON_SCAN_PERIOD_KEY.equals(key)) {
                    Long beaconScanPeriod = Long.parseLong(sharedPreferences.getString(key, Long.valueOf(DEFAULT_BACKGROUND_SCAN_PERIOD).toString()));
                    beaconManager.setBackgroundScanPeriod(beaconScanPeriod);
                } else if (GENERAL_ENABLE_MONITORING.equals(key)) {
                    setUpMonitoring();
                } else if (BEACON_MINIMUM_DISTANCE_KEY.equals(key)) {
                    minimumDistance = Long.parseLong(defaultSharedPreferences.getString(key, "5"));
                }
            }
        };
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(listener);
    }

    private void setUpMonitoring() {
        boolean enableMonitoring = defaultSharedPreferences.getBoolean(GENERAL_ENABLE_MONITORING, true);
        if (!enableMonitoring && regionBootstrap != null) {
            regionBootstrap.disable();
            regionBootstrap = null;
        } else if (enableMonitoring && regionBootstrap == null) {
            List<BeaconResult> beacons = beaconPersistence.getBeacons();

            List<Region> regions = new ArrayList<>(beacons.size());
            for (BeaconResult beacon : beacons) {
                String id = beacon.getUuid() + beacon.getMac() + beacon.getMajor() + beacon.getMinor();
                try {
                    ArrayList<Identifier> identifiers = new ArrayList(3);
                    identifiers.add(Identifier.parse(beacon.getUuid()));
                    identifiers.add(Identifier.parse(beacon.getMajor()));
                    identifiers.add(Identifier.parse(beacon.getMinor()));

                    Region region = new Region(id,
                            identifiers,
                            beacon.getMac());
                    regions.add(region);
                } catch (IllegalArgumentException e) {
                    String informalName = beacon.getInformalName();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(getString(R.string.not_able_to_start_monitoring_for_beacon_error_1));
                    if (informalName != null && !informalName.isEmpty()) {
                        stringBuilder.append("name: \"").append(informalName).append("\" with ");
                    }
                    stringBuilder.append("uuid: \"").append(beacon.getUuid()).append("mac: \"").append(beacon.getMac()).append("\" major: \"").append(beacon.getMajor()).append("\" minor: \"").append(beacon.getMinor()).append("\"");

                    String errorMessage = stringBuilder.toString();
                    Log.e(TAG, errorMessage, e);

                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            Region region = new Region("backgroundRegion",
                    null, null, null);
            regions.add(region);

            regionBootstrap = new RegionBootstrap(this, regions);
        }
    }

    private  void didEnterDistance(Beacon beacon) {
        boolean isAlreadyInDistance = beaconPersistence.isBeaconInDistance(beacon);

        if (!isAlreadyInDistance) {
            beaconPersistence.saveBeaconInDistance(beacon);
            dispatch(beacon, MESSAGE_TYPE.ENTER_DISTANCE);
        }
    }

    private void didExitDistance(Beacon beacon) {
        boolean isAlreadyInDistance = beaconPersistence.isBeaconInDistance(beacon);

        if (isAlreadyInDistance) {
            beaconPersistence.deleteBeaconInDistance(beacon);
            dispatch(beacon, MESSAGE_TYPE.EXIT_DISTANCE);
        }
    }

    @Override
    public void didEnterRegion(Region region) {
        if(region.getId1() == null) { return; }
        String mac = region.getBluetoothAddress();
        String uuid = region.getId1().toString();
        String major = region.getId2().toString();
        String minor = region.getId3().toString();

        BeaconResult beacon = beaconPersistence.getBeacon(uuid, mac, major, minor);

        if (beacon != null) {
            boolean isAlreadyInRange = beaconPersistence.isBeaconInRange(beacon);

            if(!isAlreadyInRange) {
                Log.i(TAG, getString(R.string.beacon_spotted_notification_message, uuid, mac, major, minor));
                beaconPersistence.saveBeaconInRange(beacon);

                dispatch(region, MESSAGE_TYPE.ENTER);
            } else {
                Log.i(TAG, getString(R.string.beacon_spotted_notification_message, uuid, mac, major, minor) + " " + getString(R.string.beacon_spotted_and_ignored_notification_message));
            }

        }
    }

    @Override
    public void didExitRegion(Region region) {
        if(region.getId1() == null) { return; }
        String mac = region.getBluetoothAddress();
        String uuid = region.getId1().toString();
        String major = region.getId2().toString();
        String minor = region.getId3().toString();

        BeaconResult beacon = beaconPersistence.getBeacon(uuid, mac, major, minor);

        if (beacon != null) {
            boolean isAlreadyInRange = beaconPersistence.isBeaconInRange(beacon);

            if(isAlreadyInRange) {
                Log.i(TAG, getString(R.string.beacon_exit_notification_message, uuid, mac, major, minor));
                beaconPersistence.deleteBeaconInRange(beacon);

                dispatch(region, MESSAGE_TYPE.EXIT);
            } else {
                Log.i(TAG, getString(R.string.beacon_exit_notification_message, uuid, mac, major, minor) + " " + getString(R.string.beacon_exit_and_ignored_notification_message));
            }
        }
    }

    private void toggleDistanceMonitoring(Region region, boolean state) {
        boolean monitorDistance = defaultSharedPreferences.getBoolean(BEACON_MONITOR_DISTANCE_KEY, false);
        if (monitorDistance == true && state == true) {
            try {
                // start ranging for beacons.  This will provide an update once per second with the estimated
                // distance to the beacon in the didRAngeBeaconsInRegion method.
                beaconManager.startRangingBeaconsInRegion(region);
            } catch (RemoteException e) {
            }
        }

        if (state == false) {
            try {
                beaconManager.stopRangingBeaconsInRegion(region);
            } catch (RemoteException e) {
            }
        }
    }

    private void exitOnWrongPid() {
        if (pidPersistence.isSamePid(pid) == false) {
            regionBootstrap.disable();
            regionBootstrap = null;
            System.exit(0);
        }
    }

    private void dispatch(Region region, MESSAGE_TYPE messageType) {
        exitOnWrongPid();

        String mac = region.getBluetoothAddress();
        String uuid = region.getId1().toString();
        String major = region.getId2().toString();
        String minor = region.getId3().toString();

        BeaconResult beacon = beaconPersistence.getBeacon(uuid, mac, major, minor);

        if (messageType == MESSAGE_TYPE.ENTER) {
            broadcastEnter(mac, uuid, major, minor, beacon);
            toggleDistanceMonitoring(region, true);
        }
        if (messageType == MESSAGE_TYPE.EXIT) {
            broadcastExit(mac, uuid, major, minor, beacon);
            toggleDistanceMonitoring(region, false);
        }
    }

    private void dispatch(Beacon beacon, MESSAGE_TYPE messageType) {
        exitOnWrongPid();

        String mac = beacon.getBluetoothAddress();
        String uuid = beacon.getId1().toString();
        String major = beacon.getId2().toString();
        String minor = beacon.getId3().toString();
        double distance = beacon.getDistance();

        if (messageType == MESSAGE_TYPE.ENTER_DISTANCE) {
            broadcastEnterDistance(mac, uuid, major, minor, distance);
        }
        if (messageType == MESSAGE_TYPE.EXIT_DISTANCE) {
            broadcastExitDistance(mac, uuid, major, minor, distance);
        }

    }

    private void broadcastExitDistance(String mac, String uuid, String major, String minor, double distance) {
        String message = getString(R.string.beacon_exit_distance_notification_message, uuid, mac, major, minor, distance);
        Log.i(TAG, message);

        mqttBroadcaster.publishExitDistanceMessage(uuid, mac, major, minor, distance);
        webhookBroadcaster.publishExitDistanceMessage(uuid, mac, major, minor, distance);

        showNotification(getString(R.string.beacon_exit_distance_notification_title), message);

        logPersistence.saveNewLog(message, "");
    }

    private void broadcastEnterDistance(String mac, String uuid, String major, String minor, double distance) {
        String message = getString(R.string.beacon_enter_distance_notification_message, uuid, mac, major, minor, distance);
        Log.i(TAG, message);

        mqttBroadcaster.publishEnterDistanceMessage(uuid, mac, major, minor, distance);
        webhookBroadcaster.publishEnterDistanceMessage(uuid, mac, major, minor, distance);

        showNotification(getString(R.string.beacon_enter_distance_notification_title), message);

        logPersistence.saveNewLog(message, "");
    }

    private void broadcastExit(String mac, String uuid, String major, String minor, BeaconResult beacon) {
        String message = getString(R.string.beacon_exit_notification_message, uuid, mac, major, minor);
        if (beacon.getInformalName() != null) {
            message = "[" + beacon.getInformalName() + "] " + message;
        }
        Log.i(TAG, message);

        mqttBroadcaster.publishExitMessage(uuid, mac, major, minor);
        webhookBroadcaster.publishExitMessage(uuid, mac, major, minor);

        showNotification(getString(R.string.beacon_exit_notification_title), message);

        logPersistence.saveNewLog(message, "");

        beaconInRangeListenerUpdate();
    }

    private void broadcastEnter(String mac, String uuid, String major, String minor, BeaconResult beacon) {
        String message = getString(R.string.beacon_spotted_notification_message, uuid, mac, major, minor);
        if (beacon.getInformalName() != null) {
            message = "[" + beacon.getInformalName() + "] " + message;
        }
        Log.i(TAG, message);

        mqttBroadcaster.publishEnterMessage(uuid, mac, major, minor);
        webhookBroadcaster.publishEnterMessage(uuid, mac, major, minor);

        showNotification(getString(R.string.beacon_spotted_notification_title), message);

        logPersistence.saveNewLog(message, "");

        beaconInRangeListenerUpdate();
    }


    private void beaconInRangeListenerUpdate() {
        if (beaconInRangeListener != null) {
            beaconInRangeListener.beaconsInRangeChanged();
        }
    }

    @Override
    public void didDetermineStateForRegion(int state, Region region) {
        Log.i(TAG, "I have just switched from seeing/not seeing beacons: " + state);
    }

    private void showNotification(String title, String message) {
        boolean showNotification = defaultSharedPreferences.getBoolean(BEACON_NOTIFICATIONS_ENTER_KEY, false);
        if(!showNotification) { return; }

        Intent notifyIntent = new Intent(this, MainActivity.class);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivities(this, 0,
                new Intent[] { notifyIntent }, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build();
        
        notificationManager.notify(1, notification);

        /**Creates an explicit intent for an Activity in your app**/
        Intent resultIntent = new Intent(this , MainActivity.class);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(this,
                0 /* Request code */, resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void setBeaconInRangeListener(BeaconInRangeListener beaconInRangeListener) {
        this.beaconInRangeListener = beaconInRangeListener;
    }

    interface BeaconInRangeListener {
        void beaconsInRangeChanged();
    }
}
