package com.bsantalucia.beaconmqtt.settings;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;
import android.widget.Toast;

import com.bsantalucia.beaconmqtt.R;

import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {

    public static final String GENERAL_LOG_KEY = "general_create_log";
    public static final String GENERAL_ENABLE_MONITORING = "general_enable_monitoring";

    public static final String MQTT_SERVER_KEY = "mqtt_server";
    public static final String MQTT_PORT_KEY = "mqtt_port";
    public static final String MQTT_USER_KEY = "mqtt_user";
    public static final String MQTT_PASS_KEY = "mqtt_pass";

    public static final String MQTT_ENTER_TOPIC_KEY = "mqtt_enter_topic";
    public static final String MQTT_EXIT_TOPIC_KEY = "mqtt_exit_topic";
    public static final String MQTT_ENTER_DISTANCE_TOPIC_KEY = "mqtt_enter_distance_topic";
    public static final String MQTT_EXIT_DISTANCE_TOPIC_KEY = "mqtt_exit_distance_topic";
    public static final String MQTT_ENABLE_KEY = "mqtt_enable";

    public static final String BEACON_MONITOR_DISTANCE_KEY = "beacon_monitor_distance";
    public static final String BEACON_MINIMUM_DISTANCE_KEY = "beacon_minimum_distance";
    public static final String BEACON_NOTIFICATIONS_ENTER_KEY = "beacon_notifications_enter";
    public static final String BEACON_NOTIFICATIONS_EXIT_KEY = "beacon_notifications_exit";
    public static final String BEACON_NOTIFICATIONS_ENTER_DISTANCE_KEY = "beacon_notifications_enter_distance";
    public static final String BEACON_NOTIFICATIONS_EXIT_DISTANCE_KEY = "beacon_notifications_exit_distance";
    public static final String BEACON_PERIOD_BETWEEN_SCANS_KEY = "beacon_period_between_scans";
    public static final String BEACON_SCAN_PERIOD_KEY = "beacon_scan_period";

    public static final String WEBHOOK_ENTER_PAYLOAD_KEY = "webhook_enter_payload";
    public static final String WEBHOOK_EXIT_PAYLOAD_KEY = "webhook_exit_payload";
    public static final String WEBHOOK_ENTER_DISTANCE_PAYLOAD_KEY = "webhook_enter_distance_payload";
    public static final String WEBHOOK_EXIT_DISTANCE_PAYLOAD_KEY = "webhook_exit_distance_payload";
    public static final String WEBHOOK_ENABLE_KEY = "webhook_enable";
    public static final String WEBHOOK_CONTENT_TYPE_KEY = "webhook_content_type";
    public static final String WEBHOOK_METHOD_KEY = "webhook_method";
    public static final String WEBHOOK_URL_KEY = "webhook_url";

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);
            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                NavUtils.navigateUpFromSameTask(this);
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || MqttPreferenceFragment.class.getName().equals(fragmentName)
                || WebhookPreferenceFragment.class.getName().equals(fragmentName)
                || BeaconPreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows webhook preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class WebhookPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_mqtt);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.

            bindPreferenceSummaryToValue(findPreference(WEBHOOK_ENTER_PAYLOAD_KEY));
            bindPreferenceSummaryToValue(findPreference(WEBHOOK_EXIT_PAYLOAD_KEY));
            bindPreferenceSummaryToValue(findPreference(WEBHOOK_ENTER_DISTANCE_PAYLOAD_KEY));
            bindPreferenceSummaryToValue(findPreference(WEBHOOK_EXIT_DISTANCE_PAYLOAD_KEY));
            bindPreferenceSummaryToValue(findPreference(WEBHOOK_CONTENT_TYPE_KEY));
            bindPreferenceSummaryToValue(findPreference(WEBHOOK_METHOD_KEY));
            bindPreferenceSummaryToValue(findPreference(WEBHOOK_URL_KEY));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows mqtt preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class MqttPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_mqtt);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference(MQTT_SERVER_KEY));
            bindPreferenceSummaryToValue(findPreference(MQTT_PORT_KEY));
            bindPreferenceSummaryToValue(findPreference(MQTT_USER_KEY));
            //bindPreferenceSummaryToValue(findPreference(MQTT_PASS_KEY)); // do not bind password, we want it to be hidden
            bindPreferenceSummaryToValue(findPreference(MQTT_ENTER_TOPIC_KEY));
            bindPreferenceSummaryToValue(findPreference(MQTT_EXIT_TOPIC_KEY));
            bindPreferenceSummaryToValue(findPreference(MQTT_ENTER_DISTANCE_TOPIC_KEY));
            bindPreferenceSummaryToValue(findPreference(MQTT_EXIT_DISTANCE_TOPIC_KEY));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows beacon preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class BeaconPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_beacon);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference(BEACON_PERIOD_BETWEEN_SCANS_KEY));
            bindPreferenceSummaryToValue(findPreference(BEACON_SCAN_PERIOD_KEY));
            bindPreferenceSummaryToValue(findPreference(BEACON_MINIMUM_DISTANCE_KEY));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            findPreference(GENERAL_LOG_KEY).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if ((boolean) newValue) {
                        Toast.makeText(preference.getContext(), "The log can use a lot of disk space, so it's advised to turn it off after use.", Toast.LENGTH_LONG).show();
                    }

                    return true;
                }
            });
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
}
