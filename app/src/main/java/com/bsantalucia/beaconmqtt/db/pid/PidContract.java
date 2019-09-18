package com.bsantalucia.beaconmqtt.db.pid;

import android.provider.BaseColumns;

public class PidContract {

    private PidContract() {}

    public static class PidEntry implements BaseColumns {
        public static final String TABLE_NAME = "pid";

        public static final String COLUMN_NAME_PID = "pid";
    }
}
