package cn.chper.avalon.form;

import lombok.Data;

@Data
public class SimpleResponse {

    boolean success;
    Object data;

    public SimpleResponse(boolean success, Object data) {
        this.success = success;
        this.data = data;
    }

    public static SimpleResponse ok() { return new SimpleResponse(true, null); }

    public static SimpleResponse fail() { return new SimpleResponse(false, null); }

    public static SimpleResponse ok(Object data) { return new SimpleResponse(true, data); }

    public static SimpleResponse fail(Object data) { return new SimpleResponse(false, data); }

}
