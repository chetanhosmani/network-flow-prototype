# network-flow-prototype

Network flow data point management

## Build Instructions

## Salient features

### Gradle

Service uses Gradle to manage dependencies and build targets.

### Javalin

Service uses Javalin to quickly develop a REST endpoint. Concurrency in Javalin is simple - multiple threads invoke
multiple handlers. As long as the handler can handle concurrency Javalin is happy to allow multiple concurrent requests.

### Concurrency

**Service is designed to be highly concurrent without the use of any locks.** Java utilizes CAS (Compare and Swap) in
AtomicInteger as well as ConcurrentLinkedQueue. CAS while not technically a lock can still have contention but at the
lowest instruction set level.

A ConcurrentHashMap is also used. This allows the lookups to be fully concurrent with no locking. The writes also never
lock the entire Map. In this service there's never a need to write to the same entry twice. Once written, lookup and
update allows the path to remain contention free.

## Results

The LoadGenerator was written to make simple write HTTP calls with 1000 flow log messages in each call. Calls were
distributed on 100 threads with 1000 iterations in each. The results showed that 100 million flow log entries were
handled in about 50s.

Another major factor is the cardinality of the data. For this test 10 source applications, 10 destination applications,
10
VPCs and 10 hours were used. Resulting in a total cardinality of 10000. The cardinality has less impact on the
performance and direct impact on the memory.

The results of an execution are pasted below

```
> Task :compileJava UP-TO-DATE
> Task :processResources NO-SOURCE
> Task :classes UP-TO-DATE
> Task :compileTestJava
> Task :processTestResources NO-SOURCE
> Task :testClasses
> Task :test
hour: 0 bytes_tx:10004446
hour: 0 bytes_rx:10004446
hour: 1 bytes_tx:9999073
hour: 1 bytes_rx:9999073
hour: 2 bytes_tx:10000696
hour: 2 bytes_rx:10000696
hour: 3 bytes_tx:9999537
hour: 3 bytes_rx:9999537
hour: 4 bytes_tx:9999038
hour: 4 bytes_rx:9999038
hour: 5 bytes_tx:9993851
hour: 5 bytes_rx:9993851
hour: 6 bytes_tx:10004941
hour: 6 bytes_rx:10004941
hour: 7 bytes_tx:9998987
hour: 7 bytes_rx:9998987
hour: 8 bytes_tx:9995890
hour: 8 bytes_rx:9995890
hour: 9 bytes_tx:10003541
hour: 9 bytes_rx:10003541
Total write HTTP calls made : 100000
Total flow log entries handled : 100000000
Total time taken in milliseconds : 54130
BUILD SUCCESSFUL in 54s
3 actionable tasks: 2 executed, 1 up-to-date
2:12:00 AM: Execution finished ':test --tests "LoadGenerator"'.


```

## Bottlenecks

- Javalin server thread count is at 250. However, this was not utilized by the LoadGenerator.
- Lowering latency will allow individual threads to handle more requests
- Serialization/Deserialization from JSON to Java POJOs slows down the API and consumes CPU cycles
- ConcurrentHashMap might not scale with very large cardinality of keys

## Future Improvements

- Currently, performance is very high as the API is very simple, in-memory and there is no network dependency
- Data aggregated locally will need to be persisted to a database or a more durable store
- The service is not a distributed system. A single instance is handling all flow logs
- Either a distributed system needs to be built or a smart client needs to be built that directs specific flow logs to
  specific service instances
- Java memory management can cause fragmentation especially if the entries for a single hour are located in various
  memory regions. Retrieving data for 1 hour results in accessing memory from various locations. The ideal solution
  would probably not use Java and allocate blocks of memory for each hour. Reading data for a single hour in a
  contiguous manner will improve the performance of the system.
- Service currently uses Gson to serialize/deserialize data. Since the data is simple and immutable there's no need for
  serializing multiple times (Javalin JSON layer, POJOs, etc). For a less resource intensive and faster service,
  protobuf would allow direct aggregation of the request data.
- Support for long lived connections or websockets to allow for agents to keep connections open.