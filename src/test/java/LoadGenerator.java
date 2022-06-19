import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoadGenerator {

    private static final String API = "http://localhost:8080/flows";

    private Random random = new Random();

    private static final int TOTAL_THREADS = 100;
    private static final int TOTAL_ITERATIONS = 1000;
    private static final int MESSAGE_PER_CALL = 1000;

    private JsonObject jsonGenerator() {

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("src_app", "src-" + random.nextInt(10));
        jsonObject.addProperty("dest_app", "dest-" + random.nextInt(10));
        jsonObject.addProperty("vpc_id", "vpc-" + random.nextInt(10));
        jsonObject.addProperty("hour", random.nextInt(10));
        jsonObject.addProperty("bytes_tx", 1);
        jsonObject.addProperty("bytes_rx", 1);
        JsonArray jsonArray = new JsonArray();

        return jsonObject;
    }

    public static final MediaType JSON_MEDIA_TYPE
            = MediaType.get("application/json; charset=utf-8");

    ExecutorService executorService = Executors.newFixedThreadPool(TOTAL_THREADS);

    private void testRead(int hour) throws IOException {
        OkHttpClient client = new OkHttpClient();
        String url = API + "?" + FlowMessage.paramHour + "=" + hour;
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            Gson gson = new Gson();
            JsonArray jsonArray = gson.fromJson(response.body().string(), JsonArray.class);
            int bytes_tx = 0;
            int bytes_rx = 0;
            for (JsonElement jsonElement : jsonArray) {
                FlowMessage flowMessage = gson.fromJson(jsonElement, FlowMessage.class);
                bytes_tx += flowMessage.bytes_tx;
                bytes_rx += flowMessage.bytes_rx;
            }

            System.out.println("hour: " + hour + " bytes_tx:" + bytes_tx);
            System.out.println("hour: " + hour + " bytes_rx:" + bytes_rx);
        }
    }

    @Test
    public void testWrite() throws IOException, InterruptedException {
        long currentTimeNanos = System.nanoTime();
        List<WriteRunner> callableList = new ArrayList<>();
        for (int i = 0; i < TOTAL_THREADS; i++) {
            callableList.add(new WriteRunner());
        }
        executorService.invokeAll(callableList);
        for (int i = 0; i < 10; i++) {
            testRead(i);
        }

        long elapsedTime = System.nanoTime() - currentTimeNanos;
        long timeMillis = elapsedTime / 1_000_000;

        System.out.println("Total write HTTP calls made : " + TOTAL_THREADS * TOTAL_ITERATIONS);
        System.out.println("Total flow log entries handled : " + TOTAL_THREADS * TOTAL_ITERATIONS * MESSAGE_PER_CALL);
        System.out.println("Total time taken in milliseconds : " + timeMillis);
    }

    private class WriteRunner implements Callable<Void> {
        OkHttpClient client = new OkHttpClient();

        @Override
        public Void call() throws IOException {
            for (int i = 0; i < 1000; i++) {
                JsonArray jsonArray = new JsonArray();
                for (int j = 0; j < 1000; j++) {
                    jsonArray.add(jsonGenerator());
                }

                RequestBody body = RequestBody.create(jsonArray.toString(), JSON_MEDIA_TYPE);
                Request request = new Request.Builder()
                        .url(API)
                        .post(body)
                        .build();
                Response response = client.newCall(request).execute();
            }
            return null;
        }
    }
}
