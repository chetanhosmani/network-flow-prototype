package com.networkflow.service;

import java.util.List;

public interface FlowMessageDAO {

    FlowMessage retrieve(String key);

    /**
     * Retrieve all aggregated com.networkflow.service.FlowMessage for a given hour
     */
    List<FlowMessage> retrieve(int hour);

    /**
     * Injest a new data point. If not present creates a new entry
     */
    void ingest(FlowMessage flowMessage);
}
