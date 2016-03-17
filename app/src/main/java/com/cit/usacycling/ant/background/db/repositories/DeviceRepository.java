package com.cit.usacycling.ant.background.db.repositories;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.cit.usacycling.ant.background.db.DbConstants;
import com.cit.usacycling.ant.background.db.DbProvider;
import com.cit.usacycling.ant.ui.DevicesListItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by nikolay.nikolov on 15.03.2016
 */
public class DeviceRepository implements DeviceRepositoryInterface {

    private DbProvider dbProvider;

    public DeviceRepository(DbProvider dbProvider) {
        this.dbProvider = dbProvider;
    }

    @Override
    public boolean insertDevice(DevicesListItem device, boolean setAsPaired) {
        DevicesListItem dev = getDeviceByNumber(device.getNumber());
        ContentValues contentValues = new ContentValues();
        contentValues.put(DbConstants.DEVICES_COLUMN_NUMBER, device.getNumber());
        contentValues.put(DbConstants.DEVICES_COLUMN_NAME, device.getName());
        contentValues.put(DbConstants.DEVICES_COLUMN_TYPE, device.getType());
        contentValues.put(DbConstants.DEVICES_COLUMN_ACTIVE, 0);
        contentValues.put(DbConstants.DEVICES_COLUMN_STATUS, DbConstants.DISCONNECTED_STATUS);

        try {
            if (dev == null) {
                contentValues.put(DbConstants.DEVICES_COLUMN_PAIRED, setAsPaired ? 1 : 0);
                this.dbProvider.getWritable().insert(DbConstants.DEVICES_TABLE_NAME, null, contentValues);
            } else if (!dev.isPaired()) {
                removeDeviceByNumber(device.getNumber());
                contentValues.put(DbConstants.DEVICES_COLUMN_PAIRED, 1);
                this.dbProvider.getWritable().insert(DbConstants.DEVICES_TABLE_NAME, null, contentValues);
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public HashSet<String> getAllActiveDevices() {
        HashSet<String> result = new HashSet<>();
        Cursor data = null;

        try {
            data = this.dbProvider.getReadable().rawQuery("SELECT * FROM " + DbConstants.DEVICES_TABLE_NAME + " WHERE " + DbConstants.DEVICES_COLUMN_ACTIVE + " LIKE ?", new String[]{String.valueOf(1)});
            if (data.moveToFirst()) {
                do {
                    result.add(data.getString(data.getColumnIndex(DbConstants.DEVICES_COLUMN_NUMBER)));
                } while (data.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (data != null) {
                data.close();
            }
        }

        return result;
    }

    @Override
    public HashSet<DevicesListItem> getAllActiveDevicesDetailed() {
        HashSet<DevicesListItem> result = new HashSet<>();
        Cursor data = null;

        try {
            data = this.dbProvider.getReadable().rawQuery("SELECT * FROM " + DbConstants.DEVICES_TABLE_NAME + " WHERE " + DbConstants.DEVICES_COLUMN_ACTIVE + " LIKE ?", new String[]{String.valueOf(1)});
            if (data.moveToFirst()) {
                do {
                    DevicesListItem item = new DevicesListItem();
                    item.setNumber(data.getString(data.getColumnIndex(DbConstants.DEVICES_COLUMN_NUMBER)));
                    item.setType(data.getString(data.getColumnIndex(DbConstants.DEVICES_COLUMN_TYPE)));
                    result.add(item);
                } while (data.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (data != null) {
                data.close();
            }
        }

        return result;
    }

    @Override
    public void deactivateAllDevices() {
        ContentValues values = new ContentValues();
        values.put(DbConstants.DEVICES_COLUMN_ACTIVE, 0);

        try {
            this.dbProvider.getWritable().update(DbConstants.DEVICES_TABLE_NAME, values, DbConstants.DEVICES_COLUMN_ACTIVE + " LIKE ?", new String[]{String.valueOf(1)});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public ArrayList<String> getAllPairedDeviceIds() {
        ArrayList<String> result = new ArrayList<>();
        Cursor data = null;
        try {
            data = this.dbProvider.getReadable().rawQuery("SELECT * FROM " + DbConstants.DEVICES_TABLE_NAME + " WHERE " + DbConstants.DEVICES_COLUMN_PAIRED + " LIKE ?", new String[]{String.valueOf(1)});
            if (data.moveToFirst()) {
                do {
                    result.add(data.getString(data.getColumnIndex(DbConstants.DEVICES_COLUMN_NUMBER)));

                } while (data.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (data != null) {
                data.close();
            }
        }

        return result;
    }

    @Override
    public ArrayList<DevicesListItem> getAllPairedDevices() {
        ArrayList<DevicesListItem> result = new ArrayList<>();
        Cursor data = null;
        try {
            data = this.dbProvider.getReadable().rawQuery("SELECT * FROM " + DbConstants.DEVICES_TABLE_NAME + " WHERE " + DbConstants.DEVICES_COLUMN_PAIRED + " LIKE ?", new String[]{String.valueOf(1)});
            if (data.moveToFirst()) {
                do {
                    DevicesListItem item =
                            new DevicesListItem(
                                    data.getString(data.getColumnIndex(DbConstants.DEVICES_COLUMN_NAME)),
                                    data.getString(data.getColumnIndex(DbConstants.DEVICES_COLUMN_TYPE)),
                                    data.getString(data.getColumnIndex(DbConstants.DEVICES_COLUMN_NUMBER)),
                                    data.getInt(data.getColumnIndex(DbConstants.DEVICES_COLUMN_PAIRED)) == 1,
                                    data.getString(data.getColumnIndex(DbConstants.DEVICES_COLUMN_STATUS))
                            );

                    result.add(item);

                } while (data.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (data != null) {
                data.close();
            }
        }

        return result;
    }

    @Override
    public DevicesListItem getDeviceByNumber(String number) {
        DevicesListItem device = null;
        Cursor data = null;

        try {
            data = this.dbProvider.getReadable().rawQuery("SELECT * FROM " + DbConstants.DEVICES_TABLE_NAME + " WHERE " + DbConstants.DEVICES_COLUMN_NUMBER + " LIKE ?", new String[]{number});
            if (data.moveToFirst()) {
                device = new DevicesListItem();
                device.setName(data.getString(data.getColumnIndex(DbConstants.DEVICES_COLUMN_NAME)));
                device.setType(data.getString(data.getColumnIndex(DbConstants.DEVICES_COLUMN_TYPE)));
                device.setNumber(data.getString(data.getColumnIndex(DbConstants.DEVICES_COLUMN_NUMBER)));
                device.setIsPaired(data.getInt(data.getColumnIndex(DbConstants.DEVICES_COLUMN_PAIRED)) == 1);
                device.setStatus(data.getString(data.getColumnIndex(DbConstants.DEVICES_COLUMN_STATUS)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (data != null) {
                data.close();
            }
        }

        return device;
    }

    @Override
    public String getDeviceNameByNumber(String number) {
        String result = null;
        Cursor data = null;
        try {
            data = this.dbProvider.getReadable().rawQuery("SELECT * FROM " + DbConstants.DEVICES_TABLE_NAME + " WHERE " + DbConstants.DEVICES_COLUMN_NUMBER + " LIKE ?", new String[]{number});
            result = data.moveToFirst() ? data.getString(data.getColumnIndex(DbConstants.DEVICES_COLUMN_NAME)) : null;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (data != null) {
                data.close();
            }
        }

        return result;
    }

    @Override
    public boolean removeDeviceByNumber(String number) {
        try {
            return this.dbProvider.getWritable().delete(DbConstants.DEVICES_TABLE_NAME, DbConstants.DEVICES_COLUMN_NUMBER + " LIKE ?", new String[]{number}) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean updateDeviceActiveStatus(String deviceNumber, boolean isActive) {
        ContentValues values = new ContentValues();
        values.put(DbConstants.DEVICES_COLUMN_ACTIVE, isActive ? 1 : 0);

        try {
            this.dbProvider.getWritable().update(DbConstants.DEVICES_TABLE_NAME, values, DbConstants.DEVICES_COLUMN_NUMBER + " LIKE ?", new String[]{deviceNumber});
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    public boolean updateDeviceStatus(String deviceNumber, String status) {
        ContentValues values = new ContentValues();
        values.put(DbConstants.DEVICES_COLUMN_STATUS, status);

        try {
            this.dbProvider.getWritable().update(DbConstants.DEVICES_TABLE_NAME, values, DbConstants.DEVICES_COLUMN_NUMBER + " LIKE ?", new String[]{deviceNumber});
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    public boolean unpairDeviceByNumber(String number) {
        ContentValues values = new ContentValues();
        values.put(DbConstants.DEVICES_COLUMN_PAIRED, 0);

        try {
            this.dbProvider.getWritable().update(DbConstants.DEVICES_TABLE_NAME, values, DbConstants.DEVICES_COLUMN_NUMBER + " LIKE ?", new String[]{number});
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    public boolean updateDeviceNameByNumber(DevicesListItem device, String newName) {
        String deviceName = getDeviceNameByNumber(device.getNumber());
        if (deviceName == null) {
            insertDevice(device, false);
        }
        if (newName.equals(deviceName)) {
            return false;
        }

        String validationRegex = "^[A-Za-z0-9 _]*[A-Za-z0-9][A-Za-z0-9 _]*$";

        if (newName.matches(validationRegex)) {
            try {
                ContentValues values = new ContentValues();
                values.put(DbConstants.DEVICES_COLUMN_NAME, newName);
                SQLiteDatabase db = this.dbProvider.getWritable();
                db.update(DbConstants.DEVICES_TABLE_NAME, values, DbConstants.DEVICES_COLUMN_NUMBER + " LIKE ?", new String[]{device.getNumber()});
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public List<DevicesListItem> getAllActivePowerMeters() {
        List<DevicesListItem> result = new ArrayList<>();
        Cursor data = null;

        try {
            data = this.dbProvider.getReadable().rawQuery(
                    "SELECT * FROM " + DbConstants.DEVICES_TABLE_NAME +
                            " WHERE " + DbConstants.DEVICES_COLUMN_ACTIVE +
                            " LIKE ? AND " + DbConstants.DEVICES_COLUMN_TYPE + " LIKE ?",
                    new String[]{String.valueOf(1), "Bike Power Sensors"});
            if (data.moveToFirst()) {
                do {
                    DevicesListItem item = new DevicesListItem();
                    item.setNumber(data.getString(data.getColumnIndex(DbConstants.DEVICES_COLUMN_NUMBER)));
                    item.setName(data.getString(data.getColumnIndex(DbConstants.DEVICES_COLUMN_NAME)));
                    result.add(item);
                } while (data.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (data != null) {
                data.close();
            }
        }

        return result;
    }
}
