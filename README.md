# ZIPKIN - KAFKA

Zipkin is a distributed tracing system. It helps gather timing data needed to troubleshoot latency problems in service architectures.
Features include both the collection and lookup of this data.

This is an example of zipkin-kafka integration at high level.

* App01 sends data to testZipkinTopic-1 topic.
* App02 reads data from testZipkinTopic-1, then process and business logic and again send data to testZipkinTopic-2
* App03 receives data from testZipkinTopic-2 topic.


# Zipkin-UI
Download zipkin-server jar and run the following command.
```
java -Dzipkin.collector.kafka.bootstrap-servers=localhost:9092 -jar zipkin-server-2.21.0-exec.jar
```

Once the above jar is executed it will connect the mentioned bootstrap kafka server and creates the AdminClient instance
then it will create a topic called zipkin.

# Zipkin-UI-Search

![zipkin-ui-search.png](docs/zipkin-ui-search.png?raw=true "Title")


# Zipkin-UI-Dependency

![zipkin-ui-dependency.png](docs/zipkin-ui-dependency.png?raw=true "Title")
