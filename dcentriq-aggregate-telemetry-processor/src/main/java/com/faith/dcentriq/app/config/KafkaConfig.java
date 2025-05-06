package com.faith.dcentriq.app.config;

import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.apache.kafka.streams.StreamsConfig.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler;
import org.apache.kafka.streams.processor.WallclockTimestampExtractor;
import org.apache.kafka.streams.processor.internals.assignment.StickyTaskAssignor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.config.KafkaStreamsInfrastructureCustomizer;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;

import com.faith.dcentriq.processors.common.config.ConfluentConfig;
import com.faith.dcentriq.processors.common.utils.CommonConstants;
import com.faith.dcentriq.processors.common.utils.KafkaCommonClientConfigs;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableKafka
@Slf4j
public class KafkaConfig {
  public final static String APPLICATION_ID = "kafka-aggregate-telemetry-processor";
	public final static String CLIENT_ID = "aggregateTelemetryProcessor";
	public static final String AUTO_OFFSET_RESET = CommonConstants.DEFAULT_AUTO_OFFSET_RESET;
	
	@Autowired
	private ConfluentConfig confluentConfigs;
	
	@Bean("streamsBuilderFactoryBean")
	@Primary
	public StreamsBuilderFactoryBean streamsBuilderFactoryBean(KafkaStreamsConfiguration configuration, KafkaStreamsInfrastructureCustomizer customizer)
			throws Exception {

		int replication = 3;

    /* Make sure to create the merge topic */
		this.createTopic(CommonConstants.TopicNames.NON_AGGREGATE_TOPIC, 12, replication);
		log.info("Created topic: {}", CommonConstants.TopicNames.NON_AGGREGATE_TOPIC);

		this.createTopic(CommonConstants.TopicNames.AGGREGATION_ONE_MINUTE_TOPIC, 12, replication);
		log.info("Created topic: {}", CommonConstants.TopicNames.AGGREGATION_ONE_MINUTE_TOPIC);	

		this.createTopic(CommonConstants.TopicNames.AGGREGATION_FIFTEEN_MINUTE_TOPIC, 12, replication);
		log.info("Created topic: {}", CommonConstants.TopicNames.AGGREGATION_FIFTEEN_MINUTE_TOPIC);	

		this.createTopic(CommonConstants.TopicNames.AGGREGATION_ONE_HOUR_TOPIC, 12, replication);
		log.info("Created topic: {}",  CommonConstants.TopicNames.AGGREGATION_ONE_HOUR_TOPIC);	

		StreamsBuilderFactoryBean streamsBuilderFactoryBean = new StreamsBuilderFactoryBean(configuration);
		streamsBuilderFactoryBean.afterPropertiesSet();
		streamsBuilderFactoryBean.setInfrastructureCustomizer(customizer);
		streamsBuilderFactoryBean.setCloseTimeout(10); 
		return streamsBuilderFactoryBean;

  }
	
  @Bean
	public KafkaStreamsConfiguration kafkaStreamsConfigConfiguration() {
		return new KafkaStreamsConfiguration(this.getConfigs());
	}

	/**
	 * Method to create topics
	 * @param topic
	 * @param partitions
	 * @param replication
	 */
	public void createTopic(final String topic,
			final int partitions,
			final int replication) {

		final NewTopic newTopic = new NewTopic(topic, partitions, (short) replication);
		try (final AdminClient adminClient = AdminClient.create(this.getConfigs())) {

			adminClient.createTopics(Collections.singletonList(newTopic)).all().get();

		} catch (final InterruptedException | ExecutionException e) {
			// Ignore if TopicExistsException, which may be valid if topic exists
			if(e.getCause() != null && e.getCause() instanceof TopicExistsException)  {
				log.info("Topic {} already exist: {}", topic);
			}
			else {
				log.error("Error creating topic: {}", e);
			}

		}
	}

	/*
	 * Set up configurations for Kafka Streams.
	 */
	
  private Map<String, Object> getConfigs() {
		Map<String, Object> configs = new HashMap<>();
		configs.put(APPLICATION_ID_CONFIG, APPLICATION_ID);
		configs.put(StreamsConfig.CLIENT_ID_CONFIG, CLIENT_ID);
		configs.put("num.standby.replicas", 0); // Disable standby replicas
		configs.put("max.warmup.replicas", 1); // Disable warmup replicas
		configs.put("acceptable.recovery.lag", Long.MAX_VALUE); // Disable recovery lag
		configs.put(StreamsConfig.InternalConfig.INTERNAL_TASK_ASSIGNOR_CLASS, StickyTaskAssignor.class.getName());
		
		configs.put(DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, WallclockTimestampExtractor.class.getName());
		configs.put(NUM_STREAM_THREADS_CONFIG, 1);
		configs.put(consumerPrefix(SESSION_TIMEOUT_MS_CONFIG), 30000);
		configs.put(consumerPrefix(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG), AUTO_OFFSET_RESET);
		configs.put(DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndContinueExceptionHandler.class);
		//configs.put(REPLICATION_FACTOR_CONFIG, 1);
		configs.put(STATESTORE_CACHE_MAX_BYTES_CONFIG, 10 * 1024 * 1024L); //10MB cache
		configs.put(topicPrefix(TopicConfig.RETENTION_MS_CONFIG), Integer.MAX_VALUE);
		
		configs.put(producerPrefix(ProducerConfig.ACKS_CONFIG), "1");
		//Added batch.size and compression.type to reduce the consumer lag
		configs.put(producerPrefix(ProducerConfig.BATCH_SIZE_CONFIG), "200000");
		configs.put(producerPrefix(ProducerConfig.COMPRESSION_TYPE_CONFIG), "lz4");
		configs.put(producerPrefix(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG), 2147483647);
		configs.put(producerPrefix(ProducerConfig.MAX_BLOCK_MS_CONFIG), 9223372036854775807L);
		
    /* Serialization configs. */
		//configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		//configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		//configs.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
		//configs.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, GenericAvroSerde.class);
		configs.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, "com.faith.dcentriq.processors.common.model.tagslatest.TagsLatestKeySerde");
		configs.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, "com.faith.dcentriq.processors.common.model.tagslatest.TimestampedValueSerde");
		//configs.put(producerPrefix(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG), StringDeserializer.class.getName());
		//configs.put(producerPrefix(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG), StringDeserializer.class.getName());
		configs.put(AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS, confluentConfigs.getAutoRegisterSchemas());
		//Added fetch.min.bytes to reduce the consumer lag
		configs.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, "100000");
		
		/* Security configs */
		configs.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, confluentConfigs.getBootstrapServers());
		configs.put(StreamsConfig.SECURITY_PROTOCOL_CONFIG, confluentConfigs.getSecurityProtocol());
		configs.put(CommonClientConfigs.CLIENT_DNS_LOOKUP_CONFIG,confluentConfigs.getDnsLookup());
		configs.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, confluentConfigs.getSchemaRegistryUrl());
		configs.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, false);
		configs.put(SaslConfigs.SASL_JAAS_CONFIG, confluentConfigs.getJaasConfig());
		configs.put(SaslConfigs.SASL_MECHANISM, confluentConfigs.getSaslMechanism());
		configs.put(KafkaCommonClientConfigs.BASIC_AUTH_CRED_SRC_CONFIG,confluentConfigs.getAuthCredencialResource());
		configs.put(KafkaCommonClientConfigs.SCHEMA_REGISTRY_AUTH_USER_INFO_CONFIG, confluentConfigs.getSchemaRegistryBasicAuthInfo());

		return configs;
	}
}