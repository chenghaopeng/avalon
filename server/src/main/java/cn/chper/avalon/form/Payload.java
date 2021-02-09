package cn.chper.avalon.form;

import com.alibaba.fastjson.JSONObject;

public class Payload {
    
    public String type;

    public Object data;

    public Payload(String type, Object data) {
        this.type = type;
        this.data = data;
    }

    public Payload(String json) {
        JSONObject jsonObject = JSONObject.parseObject(json);
        this.type = jsonObject.getString("type");
        this.data = jsonObject.get("data");
    }

    public String toString() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", this.type);
        jsonObject.put("data", this.data);
        return jsonObject.toJSONString();
    }

    public static String newPayload(String type, Object data) {
        return new Payload(type, data).toString();
    }

}
