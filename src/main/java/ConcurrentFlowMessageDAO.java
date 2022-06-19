import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ConcurrentFlowMessageDAO implements FlowMessageDAO {

    ConcurrentHashMap<String, FlowMessageAggregate> messageStore = new ConcurrentHashMap<>();

    ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> hourStore = new ConcurrentHashMap<>();

    @Override
    public FlowMessage retrieve(String key) {
        FlowMessageAggregate aggregate = messageStore.get(key);
        if (aggregate == null) {
            return null;
        }
        return aggregate.generateFlowMessage();
    }

    @Override
    public List<FlowMessage> retrieve(int hour) {
        String hourStr = String.valueOf(hour);
        ConcurrentLinkedQueue<String> keyList = hourStore.get(hourStr);
        if (keyList == null) {
            return Collections.emptyList();
        }

        List<FlowMessage> flowMessages = new ArrayList<>();
        for (String key : keyList) {
            flowMessages.add(retrieve(key));
        }

        return flowMessages;
    }


    @Override
    public void ingest(FlowMessage flowMessage) {
        String key = flowMessage.generateKey();
        /*
         * If present then simply aggregate data
         */
        FlowMessageAggregate aggregate = messageStore.get(key);
        if (aggregate != null) {
            aggregate.addDatapoint(flowMessage);
            return;
        }

        /*
         * If not present it's still possible the item got added in parallel to this function execution.
         * Check the return value of putIfAbsent. If non-null the put didn't succeed. Aggregate existing value.
         */

        aggregate = new FlowMessageAggregate(flowMessage);
        aggregate.addDatapoint(flowMessage);
        FlowMessageAggregate existingAggregate = messageStore.putIfAbsent(flowMessage.generateKey(), aggregate);
        if (existingAggregate != null) {
            existingAggregate.addDatapoint(flowMessage);
            return;
        }


        /*
         * Do the same for keys grouped by hour
         */
        String hourStr = String.valueOf(flowMessage.hour);
        ConcurrentLinkedQueue<String> keyList = hourStore.get(hourStr);
        if (keyList != null) {
            keyList.add(key);
            return;
        }

        keyList = new ConcurrentLinkedQueue<>();
        keyList.add(key);
        ConcurrentLinkedQueue<String> existingKeyList = hourStore.putIfAbsent(hourStr, keyList);
        if (existingKeyList != null) {
            existingKeyList.add(key);
        }
    }

}
