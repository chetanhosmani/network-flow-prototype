import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.javalin.Javalin;
import io.javalin.core.JavalinConfig;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.plugin.json.JsonMapper;
import org.intellij.lang.annotations.Flow;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

public class NetworkFlowService {

    private final Gson gson;
    private final NetworkFlowHandler handler;


    public NetworkFlowService() {
        gson = new GsonBuilder().create();
        handler = new NetworkFlowHandler();

    }

    private void setConfig(JavalinConfig config) {
        JsonMapper gsonMapper = new JsonMapper() {
            @NotNull
            @Override
            public String toJsonString(@NotNull Object obj) {
                return gson.toJson(obj);
            }

            @NotNull
            @Override
            public <T> T fromJsonString(@NotNull String json, @NotNull Class<T> targetClass) {
                return gson.fromJson(json, targetClass);
            }
        };
        config.jsonMapper(gsonMapper);
    }

    private void start() {
        Javalin app = Javalin.create(config -> setConfig(config)).start(8080);
        app.get("/flows", ctx -> handleRead(ctx));
        app.post("/flows", ctx -> handleWrite(ctx));
        app.exception(Exception.class, (e, ctx) -> ctx.status(400));
    }

    public static void main(String[] args) {
        NetworkFlowService service = new NetworkFlowService();
        service.start();
    }


    private void handleRead(Context ctx) {
        String hourStr = ctx.queryParam(FlowMessage.paramHour);
        if (hourStr == null) {
            throw new BadRequestResponse();
        }
        int hour;
        try {
            hour = Integer.parseInt(hourStr);
        } catch (NumberFormatException e) {
            throw new BadRequestResponse();
        }

        List<FlowMessage> messages = handler.readFlows(hour);
        ctx.json(messages);
    }

    private void handleWrite(Context ctx) {
        String payload = ctx.body();
        Type listType = new TypeToken<List<FlowMessage>>() {
        }.getType();

        List<FlowMessage> flowMessages = gson.fromJson(payload, listType);
        handler.writeFlows(flowMessages);
    }

}
