package com.networkflow.service;

public class FlowMessage {

    public static final String paramHour = "hour";

    String src_app;
    String dest_app;
    String vpc_id;
    int hour;

    int bytes_tx;
    int bytes_rx;

    public String generateKey() {
        return src_app + dest_app + vpc_id + hour;
    }
}
