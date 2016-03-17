package com.cit.usacycling.ant.background.websocket;

/**
 * Created by nikolay.nikolov on 29.1.2016
 */
public class ServiceResponse {
    private String id;
    private Integer code;
    private Object content;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "ServiceResponse [id=" + id + ", code=" + code + ", content="
                + content + "]";
    }
}
