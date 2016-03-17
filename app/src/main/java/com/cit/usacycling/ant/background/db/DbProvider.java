package com.cit.usacycling.ant.background.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.cit.usacycling.ant.USACyclingApplication;

/**
 * Created by nikolay.nikolov on 09.11.2015
 */
public class DbProvider extends SQLiteOpenHelper {

    private SQLiteDatabase readable;
    private SQLiteDatabase writable;

    public DbProvider(Context context) {
        super(context, DbConstants.DATABASE_NAME, null, 1);
        USACyclingApplication.getObjectGraph().inject(this);
        this.readable = this.getReadableDatabase();
        this.writable = this.getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DbConstants.CREATE_DEVICES_TABLE_QUERY);
        db.execSQL(DbConstants.CREATE_MESSAGES_TABLE_QUERY);
        db.execSQL(DbConstants.CREATE_MQTT_CONFIGURATION_TABLE_QUERY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + DbConstants.DEVICES_TABLE_NAME + ";");
        db.execSQL("DROP TABLE IF EXISTS " + DbConstants.MESSAGES_TABLE_NAME + ";");
        db.execSQL("DROP TABLE IF EXISTS " + DbConstants.MQTT_CONFIG_TABLE_NAME + ";");
        onCreate(db);
    }

    @Override
    protected void finalize() throws Throwable {
        readable.close();
        writable.close();
        super.finalize();
    }

    public SQLiteDatabase getReadable() {
        return readable;
    }

    public SQLiteDatabase getWritable() {
        return writable;
    }
}
