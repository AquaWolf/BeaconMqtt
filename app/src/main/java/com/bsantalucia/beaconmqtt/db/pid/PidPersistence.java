package com.bsantalucia.beaconmqtt.db.pid;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.bsantalucia.beaconmqtt.db.DbHelper;
import com.bsantalucia.beaconmqtt.db.beacon.BeaconContract;
import com.bsantalucia.beaconmqtt.db.beacon.BeaconInDistanceContract;
import com.bsantalucia.beaconmqtt.db.beacon.BeaconInRangeContract;
import com.bsantalucia.beaconmqtt.db.beacon.BeaconResult;

import org.altbeacon.beacon.Beacon;

import java.util.ArrayList;
import java.util.List;

public class PidPersistence {
    private static final String PID_PRIMARY_KEY_SELECTION = PidContract.PidEntry.COLUMN_NAME_PID + "=?";
    private final DbHelper dbHelper;

    public PidPersistence(Context context) {
        dbHelper = new DbHelper(context);
    }

    public void savePid(String pid) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            ContentValues values = new ContentValues();

            values.put(PidContract.PidEntry.COLUMN_NAME_PID, pid);

            db.insert(PidContract.PidEntry.TABLE_NAME, null, values);
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    public boolean isSamePid(String pid) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try {
            String[] columns = {
                    PidContract.PidEntry.COLUMN_NAME_PID
            };

            Cursor cursor = db.query(PidContract.PidEntry.TABLE_NAME, columns, PID_PRIMARY_KEY_SELECTION, new String[] {pid}, null, null, null);

            return cursor != null && cursor.getCount() > 0;

        } finally {
            if (db != null) {
                db.close();
            }
        }
    }
}
