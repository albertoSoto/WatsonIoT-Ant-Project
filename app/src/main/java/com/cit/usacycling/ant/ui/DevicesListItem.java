package com.cit.usacycling.ant.ui;

/**
 * Created by nikolay.nikolov on 2.11.2015
 */
public class DevicesListItem {
    private String name;
    private String type;
    private String number;
    private boolean isPaired;
    private String status;

    public DevicesListItem() {
    }

    public DevicesListItem(String name, String type, String number, boolean isPaired, String status) {
        setNumber(number);
        setType(type);
        setName(name);
        setIsPaired(isPaired);
        setStatus(status);
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return this.status;
    }

    public void setIsPaired(boolean isPaired) {
        this.isPaired = isPaired;
    }

    public boolean isPaired() {
        return isPaired;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }
}
