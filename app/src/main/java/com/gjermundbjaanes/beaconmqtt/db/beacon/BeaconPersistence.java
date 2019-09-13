package com.gjermundbjaanes.beaconmqtt.db.beacon;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.gjermundbjaanes.beaconmqtt.db.DbHelper;

import org.altbeacon.beacon.Beacon;

import java.util.ArrayList;
import java.util.List;

import static com.gjermundbjaanes.beaconmqtt.db.beacon.BeaconContract.BeaconEntry.COLUMN_NAME_INFORMAL_NAME;
import static com.gjermundbjaanes.beaconmqtt.db.beacon.BeaconContract.BeaconEntry.COLUMN_NAME_MAC;
import static com.gjermundbjaanes.beaconmqtt.db.beacon.BeaconContract.BeaconEntry.COLUMN_NAME_MAJOR;
import static com.gjermundbjaanes.beaconmqtt.db.beacon.BeaconContract.BeaconEntry.COLUMN_NAME_MINOR;
import static com.gjermundbjaanes.beaconmqtt.db.beacon.BeaconContract.BeaconEntry.COLUMN_NAME_UUID;
import static com.gjermundbjaanes.beaconmqtt.db.beacon.BeaconContract.BeaconEntry.TABLE_NAME;

public class BeaconPersistence {
    private static final String BEACON_IN_DISTANCE_PRIMARY_KEY_SELECTION = BeaconInDistanceContract.BeaconEntry.COLUMN_NAME_UUID + "=? AND " + BeaconInDistanceContract.BeaconEntry.COLUMN_NAME_MAC + "=? AND " + BeaconInDistanceContract.BeaconEntry.COLUMN_NAME_MAJOR + "=? AND " + BeaconInDistanceContract.BeaconEntry.COLUMN_NAME_MINOR + "=?";
    private static final String BEACON_IN_RANGE_PRIMARY_KEY_SELECTION = BeaconInRangeContract.BeaconEntry.COLUMN_NAME_UUID + "=? AND " + BeaconInRangeContract.BeaconEntry.COLUMN_NAME_MAC + "=? AND " + BeaconInRangeContract.BeaconEntry.COLUMN_NAME_MAJOR + "=? AND " + BeaconInRangeContract.BeaconEntry.COLUMN_NAME_MINOR + "=?";
    private static final String BEACON_PRIMARY_KEY_SELECTION = BeaconContract.BeaconEntry.COLUMN_NAME_UUID + "=? AND " + BeaconContract.BeaconEntry.COLUMN_NAME_MAC + "=? AND " + BeaconContract.BeaconEntry.COLUMN_NAME_MAJOR + "=? AND " + BeaconContract.BeaconEntry.COLUMN_NAME_MINOR + "=?";
    private final DbHelper dbHelper;

    public BeaconPersistence(Context context) {
        dbHelper = new DbHelper(context);
    }

    public List<BeaconResult> getBeacons() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try {
            String[] columns = {
                    BeaconContract.BeaconEntry.COLUMN_NAME_UUID,
                    BeaconContract.BeaconEntry.COLUMN_NAME_MAC,
                    BeaconContract.BeaconEntry.COLUMN_NAME_MAJOR,
                    BeaconContract.BeaconEntry.COLUMN_NAME_MINOR,
                    BeaconContract.BeaconEntry.COLUMN_NAME_INFORMAL_NAME,
            };

            Cursor cursor = db.query(BeaconContract.BeaconEntry.TABLE_NAME, columns, null, null, null, null, null);

            List<BeaconResult> beacons = new ArrayList<>();
            while(cursor.moveToNext()) {
                String uuid = cursor.getString(cursor.getColumnIndex(BeaconContract.BeaconEntry.COLUMN_NAME_UUID));
                String mac = cursor.getString(cursor.getColumnIndex(BeaconContract.BeaconEntry.COLUMN_NAME_MAC));
                String major = cursor.getString(cursor.getColumnIndex(BeaconContract.BeaconEntry.COLUMN_NAME_MAJOR));
                String minor = cursor.getString(cursor.getColumnIndex(BeaconContract.BeaconEntry.COLUMN_NAME_MINOR));
                String informalName = cursor.getString(cursor.getColumnIndex(BeaconContract.BeaconEntry.COLUMN_NAME_INFORMAL_NAME));
                beacons.add(new BeaconResult(uuid, mac, major, minor, informalName));
            }
            cursor.close();

            return beacons;
        } finally {
            if (db != null) {
                db.close();
            }
        }

    }

    public void saveBeacon(Beacon beacon, String informalBeaconName) {
        saveBeacon(beacon.getId1().toString(), beacon.getBluetoothAddress() , beacon.getId2().toString(), beacon.getId3().toString(), informalBeaconName);
    }

    public void saveBeacon(String uuid, String mac, String major, String minor, String informalBeaconName) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            ContentValues values = new ContentValues();

            values.put(BeaconContract.BeaconEntry.COLUMN_NAME_UUID, uuid);
            values.put(BeaconContract.BeaconEntry.COLUMN_NAME_MAC, mac);
            values.put(BeaconContract.BeaconEntry.COLUMN_NAME_MAJOR, major);
            values.put(BeaconContract.BeaconEntry.COLUMN_NAME_MINOR, minor);
            values.put(BeaconContract.BeaconEntry.COLUMN_NAME_INFORMAL_NAME, informalBeaconName);

            db.insert(BeaconContract.BeaconEntry.TABLE_NAME, null, values);
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    public void saveBeaconInRange(String uuid, String mac, String major, String minor) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            ContentValues values = new ContentValues();

            values.put(BeaconInRangeContract.BeaconEntry.COLUMN_NAME_UUID, uuid);
            values.put(BeaconInRangeContract.BeaconEntry.COLUMN_NAME_MAC, mac);
            values.put(BeaconInRangeContract.BeaconEntry.COLUMN_NAME_MAJOR, major);
            values.put(BeaconInRangeContract.BeaconEntry.COLUMN_NAME_MINOR, minor);

            db.insert(BeaconInRangeContract.BeaconEntry.TABLE_NAME, null, values);
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    public void saveBeaconInRange(Beacon beacon) {
        saveBeaconInRange(beacon.getId1().toString(),beacon.getBluetoothAddress(), beacon.getId2().toString(), beacon.getId3().toString());
    }

    public void saveBeaconInRange(BeaconResult beaconResult) {
        saveBeaconInRange(beaconResult.getUuid(), beaconResult.getMac(), beaconResult.getMajor(), beaconResult.getMinor());
    }

    public void saveBeaconInDistance(Beacon beacon) {
        saveBeaconInDistance(beacon.getId1().toString(),beacon.getBluetoothAddress(), beacon.getId2().toString(), beacon.getId3().toString());
    }

    public void saveBeaconInDistance(BeaconResult beaconResult) {
        saveBeaconInDistance(beaconResult.getUuid(), beaconResult.getMac(), beaconResult.getMajor(), beaconResult.getMinor());
    }

    public void saveBeaconInDistance(String uuid, String mac, String major, String minor) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            ContentValues values = new ContentValues();

            values.put(BeaconInDistanceContract.BeaconEntry.COLUMN_NAME_UUID, uuid);
            values.put(BeaconInDistanceContract.BeaconEntry.COLUMN_NAME_MAC, mac);
            values.put(BeaconInDistanceContract.BeaconEntry.COLUMN_NAME_MAJOR, major);
            values.put(BeaconInDistanceContract.BeaconEntry.COLUMN_NAME_MINOR, minor);

            db.insert(BeaconInDistanceContract.BeaconEntry.TABLE_NAME, null, values);
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    public BeaconResult getBeacon(String uuid, String mac, String major, String minor) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try {
            String[] columns = {
                    BeaconContract.BeaconEntry.COLUMN_NAME_UUID,
                    BeaconContract.BeaconEntry.COLUMN_NAME_MAC,
                    BeaconContract.BeaconEntry.COLUMN_NAME_MAJOR,
                    BeaconContract.BeaconEntry.COLUMN_NAME_MINOR,
                    BeaconContract.BeaconEntry.COLUMN_NAME_INFORMAL_NAME,
            };

            Cursor cursor = db.query(BeaconContract.BeaconEntry.TABLE_NAME, columns, BEACON_PRIMARY_KEY_SELECTION, new String[] {uuid, mac, major, minor}, null, null, null);

            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                String informalName = cursor.getString(cursor.getColumnIndex(BeaconContract.BeaconEntry.COLUMN_NAME_INFORMAL_NAME));
                cursor.close();

                return new BeaconResult(uuid, mac, major, minor, informalName);
            }

        } finally {
            if (db != null) {
                db.close();
            }
        }

        return null;
    }

    public boolean isBeaconInRange(BeaconResult beaconResult) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String uuid = beaconResult.getUuid();
        String mac = beaconResult.getMac();
        String major = beaconResult.getMajor();
        String minor = beaconResult.getMinor();

        try {
            String[] columns = {
                    BeaconInRangeContract.BeaconEntry.COLUMN_NAME_UUID,
                    BeaconInRangeContract.BeaconEntry.COLUMN_NAME_MAC,
                    BeaconInRangeContract.BeaconEntry.COLUMN_NAME_MAJOR,
                    BeaconInRangeContract.BeaconEntry.COLUMN_NAME_MINOR,
            };

            Cursor cursor = db.query(BeaconInRangeContract.BeaconEntry.TABLE_NAME, columns, BEACON_IN_RANGE_PRIMARY_KEY_SELECTION, new String[] {uuid, mac, major, minor}, null, null, null);

            return cursor != null && cursor.getCount() > 0;

        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    public boolean isBeaconInDistance(Beacon beacon) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String uuid = beacon.getId1().toString();
        String mac = beacon.getBluetoothAddress();
        String major = beacon.getId2().toString();
        String minor = beacon.getId3().toString();

        try {
            String[] columns = {
                    BeaconInDistanceContract.BeaconEntry.COLUMN_NAME_UUID,
                    BeaconInDistanceContract.BeaconEntry.COLUMN_NAME_MAC,
                    BeaconInDistanceContract.BeaconEntry.COLUMN_NAME_MAJOR,
                    BeaconInDistanceContract.BeaconEntry.COLUMN_NAME_MINOR,
            };

            Cursor cursor = db.query(BeaconInDistanceContract.BeaconEntry.TABLE_NAME, columns, BEACON_IN_DISTANCE_PRIMARY_KEY_SELECTION, new String[] {uuid, mac, major, minor}, null, null, null);

            return cursor != null && cursor.getCount() > 0;

        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    public boolean deleteBeacon(BeaconResult beaconResult) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            int numberOfRowsAffected = db.delete(BeaconContract.BeaconEntry.TABLE_NAME, BEACON_PRIMARY_KEY_SELECTION, new String[] {beaconResult.getUuid(), beaconResult.getMac(), beaconResult.getMajor(), beaconResult.getMinor()});
            return numberOfRowsAffected != 0;
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    public boolean deleteBeaconInRange(BeaconResult beaconResult) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            int numberOfRowsAffected = db.delete(BeaconInRangeContract.BeaconEntry.TABLE_NAME, BEACON_IN_RANGE_PRIMARY_KEY_SELECTION, new String[] {beaconResult.getUuid(), beaconResult.getMac(), beaconResult.getMajor(), beaconResult.getMinor()});
            return numberOfRowsAffected != 0;
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    public boolean deleteBeaconInDistance(Beacon beacon) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            int numberOfRowsAffected = db.delete(BeaconInDistanceContract.BeaconEntry.TABLE_NAME, BEACON_IN_DISTANCE_PRIMARY_KEY_SELECTION, new String[] {beacon.getId1().toString(), beacon.getBluetoothAddress(), beacon.getId2().toString(), beacon.getId3().toString()});
            return numberOfRowsAffected != 0;
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }
}
