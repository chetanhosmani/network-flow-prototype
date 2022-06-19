import java.util.concurrent.atomic.AtomicInteger;

public class FlowMessageAggregate {

    private final String srcApp;
    private final String destApp;
    private final String vpcId;
    private final int hour;

    private final AtomicInteger atomicBytesTx = new AtomicInteger();
    private final AtomicInteger atomicBytesRx = new AtomicInteger();

    public FlowMessageAggregate(FlowMessage flowMessage) {
        this(flowMessage.src_app, flowMessage.dest_app, flowMessage.vpc_id, flowMessage.hour);
    }

    public FlowMessageAggregate(String srcApp, String destApp, String vpcId, int hour) {
        this.srcApp = srcApp;
        this.destApp = destApp;
        this.vpcId = vpcId;
        this.hour = hour;
    }

    public void addDatapoint(FlowMessage flowMessage) {
        atomicBytesTx.addAndGet(flowMessage.bytes_tx);
        atomicBytesRx.addAndGet(flowMessage.bytes_rx);
    }

    public FlowMessage generateFlowMessage() {
        FlowMessage flowMessage = new FlowMessage();
        flowMessage.src_app = srcApp;
        flowMessage.dest_app = destApp;
        flowMessage.vpc_id = vpcId;
        flowMessage.hour = hour;
        flowMessage.bytes_tx = atomicBytesTx.get();
        flowMessage.bytes_rx = atomicBytesRx.get();
        return flowMessage;
    }
}
