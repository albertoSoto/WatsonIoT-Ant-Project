package com.cit.usacycling.ant.global;

/**
 * Created by nikolay.nikolov on 4.2.2016
 */
public enum ResponseCode {

    OK(200),
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    INTERNAL_ERROR(500);

    private final int code;

    ResponseCode(int code) {
        this.code = code;
    }

    public boolean compare(int statusCode) {
        return this.code == statusCode;
    }
}