import org.intellij.lang.annotations.Flow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NetworkFlowHandler {

    List<FlowMessage> flowMessages = new ArrayList<>();
    public List<FlowMessage> readFlows(int hour, String paginationToken) {
        return new ArrayList<>(flowMessages);
    }

    public void writeFlows(List<FlowMessage> inputflowMessages) {
        flowMessages.addAll(inputflowMessages);
    }
}
