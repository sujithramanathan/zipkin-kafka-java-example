package com.zipkin.kafka;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.kafka.clients.KafkaTracing;
import brave.messaging.MessagingTracing;
import zipkin2.codec.Encoding;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.kafka.KafkaSender;

/**
 *
 * @author Sujith Ramanathan
 *
 */
public class App02 {

	private Consumer<String, String> consumer;
	private Producer<String, String> producer;
	private KafkaSender kafkaSender;
	private AsyncReporter<zipkin2.Span> reporter;
	private KafkaTracing kafkaTracing;
	private Tracer tracer;

	public App02() {
		Map<String, Object> producerConfigMap = new HashMap<>();
		producerConfigMap.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
		producerConfigMap.put(ProducerConfig.ACKS_CONFIG, "all");
		producerConfigMap.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		producerConfigMap.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		producerConfigMap.put(ProducerConfig.RETRIES_CONFIG, 5);
		producer = new KafkaProducer<>(producerConfigMap);
		
		Map<String, Object> configMap = new HashMap<>();
		configMap.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
		configMap.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		configMap.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		configMap.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
		configMap.put(ConsumerConfig.GROUP_ID_CONFIG, "zipkin-consumer-2");
		configMap.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		consumer = new KafkaConsumer<>(configMap);
		initConsumer();
		initProducer();
	}
	
	private void initProducer() {
		producer = kafkaTracing.producer(producer);
	}

	private void initConsumer() {
		kafkaSender = KafkaSender.newBuilder().topic("zipkin").bootstrapServers("localhost:9092")
				.encoding(Encoding.JSON).build();
		reporter = AsyncReporter.create(kafkaSender);
		Tracing tracing = Tracing.newBuilder().localServiceName("CI-002").spanReporter(reporter).build();
		tracer = tracing.tracer();
		MessagingTracing messagingTracing = MessagingTracing.newBuilder(tracing).build();
		kafkaTracing = KafkaTracing.create(messagingTracing);
		consumer = kafkaTracing.consumer(consumer);
	}

	@SuppressWarnings("unused")
	private Tracer tracer() {
		return tracer;
	}

	public static void main(String[] args) throws Exception {
		App02 obj = new App02();
		System.out.println("Start Receiving");
		obj.consumer.subscribe(Arrays.asList("testZipkinTopic-1"));
		ConsumerRecords<String, String> records = obj.consumer.poll(Duration.ofSeconds(30));
		Iterator<ConsumerRecord<String, String>> consumerRecord = records.iterator();
		ConsumerRecord<String, String> keyValue = null;
		while (consumerRecord.hasNext()) {
			keyValue = consumerRecord.next();
			Span span = obj.kafkaTracing.nextSpan(keyValue).name("r-process").start();
			System.out.println(keyValue.key() + " - " + keyValue.value());
			obj.producer.send(new ProducerRecord<String, String>("testZipkinTopic-2", keyValue.key(),keyValue.value()+" - from ci-002")).get();
			span.finish();
		}
		obj.consumer.commitAsync();
		Thread.sleep(1000);
	}

}
