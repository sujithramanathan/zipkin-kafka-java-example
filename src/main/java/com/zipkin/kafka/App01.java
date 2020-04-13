package com.zipkin.kafka;

import brave.Tracer;
import brave.Tracing;
import brave.kafka.clients.KafkaTracing;
import brave.messaging.MessagingTracing;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import zipkin2.codec.Encoding;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.kafka.KafkaSender;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * @author Sujith Ramanathan
 */
public class App01 {

    private Producer<String, String> producer;
    private KafkaSender kafkaSender;
    private AsyncReporter reporter;
    private KafkaTracing kafkaTracing;
    private Tracer tracer;

    public App01() {
        Map<String, Object> producerConfigMap = new HashMap<>();
        producerConfigMap.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        producerConfigMap.put(ProducerConfig.ACKS_CONFIG, "all");
        producerConfigMap.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerConfigMap.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerConfigMap.put(ProducerConfig.RETRIES_CONFIG, 5);
        producer = new KafkaProducer<>(producerConfigMap);

        initProducer();
    }

    private Tracer tracer() {
        return tracer;
    }

    private void initProducer() {
        kafkaSender = KafkaSender.newBuilder().topic("zipkin").bootstrapServers("localhost:9092")
                .encoding(Encoding.JSON).build();
        reporter = AsyncReporter.create(kafkaSender);
        Tracing tracing = Tracing.newBuilder().localServiceName("CI-001").spanReporter(reporter)
                .build();
        tracer = tracing.tracer();
        MessagingTracing messagingTracing = MessagingTracing.newBuilder(tracing).build();
        kafkaTracing = KafkaTracing.create(messagingTracing);
        producer = kafkaTracing.producer(producer);
    }


    public static void main(String[] args) throws Exception {
        System.out.println("App starts");
        App01 obj = new App01();

        for (int i = 1; i <= 10; i++) {
            Future<RecordMetadata> metadata = obj.producer
                    .send(new ProducerRecord<String, String>("testZipkinTopic-1", "key2", "value2"));
            while (!metadata.isDone()) {
            }
            System.out.println("Message Sent :: Offset = " + metadata.get().offset());
        }
        obj.producer.flush();
        obj.producer.close();
        Thread.sleep(1000);
    }

}
