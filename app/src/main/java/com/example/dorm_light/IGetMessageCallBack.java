package com.example.dorm_light;

import org.json.JSONException;

public interface IGetMessageCallBack {
    public void setMessage(String message) throws JSONException;
    public void setMQTTstatus(boolean MQTTstatus);
}
