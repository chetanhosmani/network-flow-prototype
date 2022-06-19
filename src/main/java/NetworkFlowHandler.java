import java.util.List;

public class NetworkFlowHandler {

    FlowMessageDAO flowMessageDAO = new ConcurrentFlowMessageDAO();
    public List<FlowMessage> readFlows(int hour) {
        return flowMessageDAO.retrieve(hour);
    }

    public void writeFlows(List<FlowMessage> inputFlowMessages) {
        for (FlowMessage flowMessage : inputFlowMessages) {
            flowMessageDAO.ingest(flowMessage);
        }
    }
}
