package com.cit.usacycling.ant.background.db;

/**
 * Created by nikolay.nikolov on 26.11.2015
 */
public class Message {
    private int id;
    private String message;
    private int sent;

    public Message(int id, String message, int sent) {
        this.id = id;
        this.message = message;
        this.sent = sent;
    }

    public int getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public int getSent() {
        return sent;
    }
}
