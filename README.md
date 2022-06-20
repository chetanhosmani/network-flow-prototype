# network-flow-prototype

Network flow data point management

## Build Instructions

To run this project Java 8 and Gradle are needed.
If Gradle is not available the mega jar can still be executed to run the application without building it through gradle.

To run the load test and the service together, you need to use the mega jar instructions below.

### Gradle instructions

To bring up the service

```
gradle build
gradle run
```

### Mega Jar instructions

A mega jar is bundled if needed. This has all the dependencies inside it and can be run on its own, as long as Java is
available.

To run the service

```
java -jar build/libs/network-flow-prototype-1.0-SNAPSHOT-all.jar
```

To run the load test (after starting up the service)

```
java -cp build/libs/network-flow-prototype-1.0-SNAPSHOT-all.jar com.networkflow.service.LoadGenerator
```

## Example Calls

```
curl -i -X POST "localhost:8080/flows" -H 'Content-type: application/json' -d '[{"src_app":"foo-1","dest_app":"bar-1","vpc_id":"vpc-1","hour":2,"bytes_tx":100,"bytes_rx":200},{"src_app":"foo-2","dest_app":"bar-1","vpc_id":"vpc-2","hour":3,"bytes_tx":10,"bytes_rx":20}]'
```

```
HTTP/1.1 200 OK
Date: Mon, 20 Jun 2022 07:53:27 GMT
Content-Type: text/plain
Content-Length: 0
```

```
curl -s "localhost:8080/flows?hour=2" | jq
```

```
[
  {
    "src_app": "foo-1",
    "dest_app": "bar-1",
    "vpc_id": "vpc-1",
    "hour": 2,
    "bytes_tx": 100,
    "bytes_rx": 200
  }
]

```

## Salient features

### Java + Gradle

Service is built in Java. Java was chosen primarily due to the speed of development. For this API use case, memory being
a constraint makes Java a poor choice. However, the concurrency primitives in Java makes it easy to build something
quickly.
Gradle is used to manage dependencies and build targets.

### Javalin

Service uses Javalin to quickly develop a REST endpoint. Concurrency in Javalin is simple - multiple threads invoke
multiple handlers. As long as the handler can handle concurrency Javalin is happy to allow multiple concurrent requests.

### Concurrency

**Service is designed to be highly concurrent without the use of any locks.** Java AtomicInteger and
ConcurrentLinkedQueue utilize CAS (Compare and Swap). CAS while not technically a lock can still cause contention among
threads. However, it does not suffer from context switches which can expensive if the concurrency is high.

A ConcurrentHashMap is also used. This allows the lookups to be fully concurrent with no locking. The writes also never
lock the entire Map. In this service there's never actually a need to write to the same entry twice. Since data is
aggregated in the same flow object, once a flow object is inserted, no more writes to the map are needed. Since lookups
are contention free the entire system remains mostly contention free.

## Results

The com.networkflow.service.LoadGenerator was written to make simple write HTTP calls with 1000 flow log messages in
each call. Calls were
distributed on 100 threads with 1000 iterations in each. The results showed that 100 million flow log entries were
handled in about 50s.

Another major factor is the cardinality of the data. For this test 10 source applications, 10 destination applications,
10
VPCs and 10 hours were used. Resulting in a total cardinality of 10000. The cardinality has less impact on the
performance and direct impact on the memory.

The results from a LoadGenerator execution are pasted below

```
hour: 0 bytes_tx:39971536
hour: 0 bytes_rx:79943072
hour: 1 bytes_tx:39996452
hour: 1 bytes_rx:79992904
hour: 2 bytes_tx:40009632
hour: 2 bytes_rx:80019264
hour: 3 bytes_tx:39999856
hour: 3 bytes_rx:79999712
hour: 4 bytes_tx:39983948
hour: 4 bytes_rx:79967896
hour: 5 bytes_tx:39994148
hour: 5 bytes_rx:79988296
hour: 6 bytes_tx:40028324
hour: 6 bytes_rx:80056648
hour: 7 bytes_tx:40000224
hour: 7 bytes_rx:80000448
hour: 8 bytes_tx:40000984
hour: 8 bytes_rx:80001968
hour: 9 bytes_tx:40014896
hour: 9 bytes_rx:80029792
Total write HTTP calls made : 100000
Total flow log entries handled : 100000000
Total bytes_tx expected 400000000 Total bytes_tx actual 400000000
Total bytes_rx expected 800000000 Total bytes_rx actual 800000000
Total time taken in milliseconds : 47131
```

## Scalability

The service is a very simple in-memory implementation and is pretty much limited by the CPU and memory of the hardware.

- Javalin server thread count is at 250. If each request was handled in 10 ms, the frontend of the service can handle
  250,000 requests per second. However, this was not utilized by the com.networkflow.service.LoadGenerator.
- The other bottleneck is around AtomicInteger. This resource is a point of contention. For in-memory a LongAdder can
  provide much better performance. In a database partitioning the key similar to a LongAdder will allow it to support
  much higher throughput. Again for this test this bottleneck was not the point of contention.
- ConcurrentHashMap might not scale with very large cardinality of keys. Java memory management can cause fragmentation
  especially if the entries for a single hour are located in various memory regions. When using a more durable data
  store like a database or a block device, data can be arranged better by grouping same hour data together. The current
  solution fragments data in an extremely large ConcurrentHashMap's array.
- Currently, the service runs on a single host and is limited by the resources of the hardware. Building a distributed
  service will allow it to be horizontally scalable.
- Service currently uses Gson to serialize/deserialize data. Since the data is simple and immutable there's no need for
  serializing multiple times (Javalin JSON layer, POJOs, etc). For a less resource intensive and faster service,
  protobuf would allow direct aggregation of the request data. This would reduce the latency and CPU consumption.
- Support for long-lived connections or websockets to allow for agents to keep connections open.

