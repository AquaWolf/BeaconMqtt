package com.bsantalucia.beaconmqtt.db.beacon;

import android.provider.BaseColumns;

public class BeaconInDistanceContract {

    private BeaconInDistanceContract() {}

    public static class BeaconEntry implements BaseColumns {
        public static final String TABLE_NAME = "beacon_in_distance";

        public static final String COLUMN_NAME_MAC = "mac";
        public static final String COLUMN_NAME_UUID = "uuid";
        public static final String COLUMN_NAME_MINOR = "minor";
        public static final String COLUMN_NAME_MAJOR = "major";
    }
}
