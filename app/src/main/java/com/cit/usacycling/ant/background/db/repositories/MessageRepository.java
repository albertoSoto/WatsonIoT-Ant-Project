package com.cit.usacycling.ant.background.db.repositories;

import android.content.ContentValues;
import android.database.Cursor;

import com.cit.usacycling.ant.background.db.DbConstants;
import com.cit.usacycling.ant.background.db.DbProvider;
import com.cit.usacycling.ant.background.db.Message;

import java.util.HashSet;

/**
 * Created by nikolay.nikolov on 15.03.2016
 */
public class MessageRepository implements MessageRepositoryInterface {

    private DbProvider dbProvider;

    public MessageRepository(DbProvider dbProvider) {
        this.dbProvider = dbProvider;
    }

    @Override
    public boolean insertMessage(String message) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DbConstants.MESSAGES_COLUMN_PAYLOAD, message);
        contentValues.put(DbConstants.MESSAGES_COLUMN_SENT_STATUS, 0);

        try {
            dbProvider.getWritable().insert(DbConstants.MESSAGES_TABLE_NAME, null, contentValues);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean markMessageAsSent(int messageId) {
        ContentValues values = new ContentValues();
        values.put("sent", 1);
        try {
            this.dbProvider.getWritable().update(DbConstants.MESSAGES_TABLE_NAME, values, DbConstants.DEVICES_COLUMN_NUMBER + " LIKE ?", new String[]{String.valueOf(messageId)});
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Message getMessageById(int messageId) {
        Message msg = null;
        Cursor data = null;

        try {
            data = this.dbProvider.getReadable().rawQuery("SELECT * FROM " + DbConstants.MESSAGES_TABLE_NAME + " WHERE " + DbConstants.DEVICES_COLUMN_NUMBER + " LIKE ?", new String[]{String.valueOf(messageId)});
            if (data.moveToFirst()) {
                do {
                    int id = data.getInt(data.getColumnIndex(DbConstants.DEVICES_COLUMN_NUMBER));
                    String payload = data.getString(data.getColumnIndex(DbConstants.MESSAGES_COLUMN_PAYLOAD));
                    int sent = data.getInt(data.getColumnIndex(DbConstants.MESSAGES_COLUMN_SENT_STATUS));
                    msg = new Message(id, payload, sent);
                } while (data.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (data != null) {
                data.close();
            }
        }

        return msg;
    }

    @Override
    public HashSet<Message> getMessagesToSend() {
        HashSet<Message> messages = new HashSet<>();
        Cursor data = null;

        try {
            data = this.dbProvider.getReadable().rawQuery("SELECT * FROM " + DbConstants.MESSAGES_TABLE_NAME + " WHERE " + "sent" + " LIKE ?", new String[]{String.valueOf(0)});
            if (data.moveToFirst()) {
                do {
                    int id = data.getInt(data.getColumnIndex(DbConstants.DEVICES_COLUMN_NUMBER));
                    String payload = data.getString(data.getColumnIndex(DbConstants.MESSAGES_COLUMN_PAYLOAD));
                    messages.add(new Message(id, payload, 0));
                } while (data.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (data != null) {
                data.close();
            }
        }

        return messages;
    }

    @Override
    public boolean clearAllUnsentMessages() {
        try {
            return this.dbProvider.getWritable().delete(DbConstants.MESSAGES_TABLE_NAME, DbConstants.DEVICES_COLUMN_NUMBER + ">=0", null) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
