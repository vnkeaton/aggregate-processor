package com.faith.dcentriq.app.config;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.EmitStrategy;
import org.apache.kafka.streams.kstream.GlobalKTable;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.ValueJoiner;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.WindowStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.config.KafkaStreamsInfrastructureCustomizer;
import org.springframework.stereotype.Component;

import com.faith.dcentriq.processors.common.config.ConfluentConfig;
import com.faith.dcentriq.processors.common.config.CustomTimestampExtractor;
import com.faith.dcentriq.processors.common.model.tagslatest.HeartbeatValue;
import com.faith.dcentriq.processors.common.model.tagslatest.HeartbeatValueSerde;
import com.faith.dcentriq.processors.common.model.tagslatest.TagsLatestKey;
import com.faith.dcentriq.processors.common.model.tagslatest.TagsLatestKeySerde;
import com.faith.dcentriq.processors.common.model.tagslatest.TelemetryTumblingWindowOutput;
import com.faith.dcentriq.processors.common.model.tagslatest.TelemetryTumblingWindowOutputSerde;
import com.faith.dcentriq.processors.common.model.tagslatest.TimestampedValue;
import com.faith.dcentriq.processors.common.model.tagslatest.TimestampedValueSerde;
import com.faith.dcentriq.processors.common.utils.CommonConstants;
import com.faith.dcentriq.processors.common.utils.KafkaCommonClientConfigs;
import com.faith.dcentriq.processors.common.utils.SerdesUtils;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CustomInfrastructureCustomizer implements KafkaStreamsInfrastructureCustomizer {

  @Autowired
	private ConfluentConfig confluentConfigs;

  public static final String HEARTBEAT = "Edge_Heartbeat";
  public static final String STORE_PREFIX = "telemetry-window-aggregation-store-";
  public static final String TOPIC_PREFIX = "dcentriq.aggregation.";
  public static final String JSON_SUFFIX = ".json";
  public static final int HEARTBEAT_DURATION_MINUTES = 5;
  public static final String HEARTBEAT_TABLE = "heartbeat-table";
  private final AggregateWindowConfig aggregateWindowConfig;

  /* Serializers and Deserializers */
  private static final Serde<TagsLatestKey> telemetryKeySerde = new TagsLatestKeySerde();
  private static final Serde<HeartbeatValue> heartbeatValueSerde = new HeartbeatValueSerde();
  private static final Serde<TimestampedValue> telemetryValueSerde = new TimestampedValueSerde();
  private static final Serde<TelemetryTumblingWindowOutput> telemetryTumblingWindowOutputSerde = new TelemetryTumblingWindowOutputSerde();

  CustomInfrastructureCustomizer(AggregateWindowConfig aggregateWindowConfig) {
    this.aggregateWindowConfig = aggregateWindowConfig;
  }

  /**
	 * Get a GenericRecord serde.
	 * @return
	 */
	public Serde<GenericRecord> getGenericSerde() {

		final Serde<GenericRecord> genericSerde = new GenericAvroSerde();
		Map<String, String> config = new HashMap<>();

		config.put(KafkaCommonClientConfigs.BASIC_AUTH_CRED_SRC_CONFIG, confluentConfigs.getAuthCredencialResource());
		config.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, confluentConfigs.getSchemaRegistryUrl());
		config.put(KafkaCommonClientConfigs.SECURITY_PROTOCOL_CONFIG, confluentConfigs.getSecurityProtocol());
		config.put(KafkaCommonClientConfigs.SCHEMA_REGISTRY_AUTH_USER_INFO_CONFIG, confluentConfigs.getSchemaRegistryBasicAuthInfo());
		
		genericSerde.configure(config, false);
		return genericSerde;
	}

	@Override
	public void configureBuilder(StreamsBuilder builder) {
    /* Get window configurations */
    Map<String, WindowConfiguration> windowConfigurations = getProcessorWindowMappings();

    /* Register Streams and Tables */
    Consumed<TagsLatestKey, TimestampedValue> consumerTelemetryOptions =
		  Consumed.with(SerdesUtils.jsonSerde(TagsLatestKey.class), SerdesUtils.jsonSerde(TimestampedValue.class))
			  .withTimestampExtractor(new CustomTimestampExtractor());

    // Define consumption options using custom Serdes (assuming HEARTBEAT_TOPIC uses them)
    Consumed<TagsLatestKey, TimestampedValue> heartbeatConsumerOptions =
    Consumed.with(telemetryKeySerde, telemetryValueSerde); // Use your custom Serdes

    // Create GlobalKTable from heartbeat topic
    // This table is used to check for recent heartbeats that is within the heartbeat duration

    GlobalKTable<TagsLatestKey, TimestampedValue> heartbeatTable = builder.globalTable(
      CommonConstants.TopicNames.HEARTBEAT_TOPIC,
      heartbeatConsumerOptions);

    /*
     * Topology
     */

    // Active Telemetry stream  
    KStream<TagsLatestKey, TimestampedValue> activeTelemetryStream = builder.stream(
      CommonConstants.TopicNames.AGGREGATION_ACTIVE_TELEMETRY_TOPIC);

    // Non-aggregated telemetry stream
    /*activeTelemetryStream
      .filterNot( (k, v) -> k.getName().equals(HEARTBEAT))
      .map((key, value) -> new KeyValue<>(key, setWindowOutput(key, value)))
      .to(CommonConstants.TopicNames.NON_AGGREGATE_TOPIC, Produced.with(telemetryKeySerde, telemetryTumblingWindowOutputSerde)); 
    */

    // Aggregated telemetry streams as per window configurations
    //for (Map.Entry<String,  WindowConfiguration> entry : windowConfigurations.entrySet()) {
     
      //WindowConfiguration windowConfiguration = entry.getValue();
      //log.info("windowConfiguration:{}", windowConfiguration.toString());
      
      // Define windows
      //TimeWindows tumblingWindows = TimeWindows.ofSizeAndGrace(Duration.ofMillis(windowConfiguration.getWindowDuration()), 
      //  Duration.ofMillis(windowConfiguration.getWindowGrace()));
      TimeWindows tumblingWindows = TimeWindows.ofSizeAndGrace(Duration.ofMillis(60000L), 
        Duration.ofMillis(0L)); // tumbling window has 0 grace period
  
      // Materialized data for tables and state stores
      //Materialized<TagsLatestKey, TimestampedValue, WindowStore<Bytes, byte[]>> telemetryAggTableMaterialized = Materialized
      //  .as(windowConfiguration.getAggregateTableStoreName());
      Materialized<TagsLatestKey, TimestampedValue, WindowStore<Bytes, byte[]>> telemetryAggTableMaterialized = Materialized
        .as("telemetry-window-aggregation-store-one_minute_telem");
      telemetryAggTableMaterialized.withKeySerde(telemetryKeySerde).withValueSerde(telemetryValueSerde);

      //Establish aggregation window for telemetry stream -> results in a windowed Ktable
      KTable<Windowed<TagsLatestKey>, TimestampedValue> telemetryAggTable = activeTelemetryStream
        .filterNot( (k, v) -> k.getName().equals(HEARTBEAT))
        .groupByKey()
        .windowedBy(tumblingWindows)
        .emitStrategy(EmitStrategy.onWindowClose()) // only emit when the window closes
        .reduce( 
          (o, n) -> n, // keep the latest value
          telemetryAggTableMaterialized);

      // Convert telemetryAggTable to a stream and extract the original key
      KStream<TagsLatestKey, TimestampedValue> telemetryWindowStream = telemetryAggTable
        .toStream()
        // Extract the original key from the windowed key
        .map((Windowed<TagsLatestKey> key, TimestampedValue value) -> new KeyValue<>(key.key(), value));

      // Define the ValueJoiner for the KTable join (only gets values)
      ValueJoiner<TimestampedValue, TimestampedValue, JoinedData> valueJoiner =
          (streamValue, tableValue) -> new JoinedData(streamValue, tableValue); // tableValue will be null if no match
      
       // Left join with heartbeat table, filter for matches, and transform
       telemetryWindowStream.leftJoin(
        heartbeatTable,
        (key, value) -> key, // Key extractor for the stream (the key itself)
        valueJoiner)
      // Check if heartbeat exists and transform accordingly
      .map((key, joinedValue) -> {
        return new KeyValue<>(key, setWindowOutput(key, joinedValue.getStreamValue(), joinedValue.getHeartbeatValue()));
      })
      // Write to the output topic
      //.to(windowConfiguration.getAggregateTopicName(), Produced.with(telemetryKeySerde, telemetryTumblingWindowOutputSerde)); 
      .to("dcentriq.aggregation.one_minute_telem.json", Produced.with(telemetryKeySerde, telemetryTumblingWindowOutputSerde));       
    //}

    log.info("***Notification TOPOLOGY***");
    builder.build().describe();
    final Topology topology = builder.build();
    System.out.println(topology.describe());

  }
  
  /*
   * Class methods
   */

  /* This method takes the epoch time stamp and determines 
   * if it's within the Heartbeat Time Duration 
   * 
   * Input: epoch time stamp, long
   * Output: boolean, true if within the duration, false otherwise
   */   
  private boolean isWithinHeartbeatDuration(long epochTimestamp) {
    Instant timestampInstant = Instant.ofEpochMilli(epochTimestamp);
    Instant now = Instant.now();
    Duration timeDifference = Duration.between(timestampInstant, now);
    Duration durationMinutes = Duration.ofMinutes(HEARTBEAT_DURATION_MINUTES);

    return !timeDifference.isNegative() && timeDifference.compareTo(durationMinutes) <= 0;
  }

  private TelemetryTumblingWindowOutput setWindowOutput(TagsLatestKey key, 
      TimestampedValue streamValue, 
      TimestampedValue heartbeatValue) {

    boolean isHeartbeatGood = false;
    // Check if heartbeat value is not null and within the heartbeat duration
    if (heartbeatValue != null) {
      isHeartbeatGood = isWithinHeartbeatDuration(heartbeatValue.getTs());
    }

      // Create the tumbling window output for this time duration.
    return TelemetryTumblingWindowOutput.builder()
      .tagSet(key.getTagSet())
      .name(key.getName())
      .myclass(key.getClazz())
      .refSiteId(streamValue.getRefSiteId())
      .assetId((String) streamValue.getAssetId())
      .siteDate(streamValue.getSiteDate())
      .value(isHeartbeatGood ? streamValue.getValue() : null)
      .latestValue(streamValue.getLatesValue())
      .ts(streamValue.getTs())
      .id(key.getId())
      .build();
  }

  public Map<String, WindowConfiguration> getProcessorWindowMappings() {

    Map<String, WindowConfiguration> windowConfigurations = new HashMap<>();
    
    // read the application configs for the window mappings
    if (this.aggregateWindowConfig == null) {
      log.error("aggregateWindowConfig is null");
      return windowConfigurations;
    }

    Map<String, Long> durationMap = this.aggregateWindowConfig.getDuration();

    // Set the aggregate window configurations
    for (Map.Entry<String, Long> entry : durationMap.entrySet()) {

      WindowConfiguration windowConfiguration = new WindowConfiguration();
      windowConfiguration.setWindowDuration(entry.getValue());
      windowConfiguration.setWindowGrace(0L); // tumbling window has 0 grace period

      StringBuilder storeName = new StringBuilder(STORE_PREFIX);
      storeName.append(entry.getKey());
      windowConfiguration.setAggregateTableStoreName(storeName.toString());

      StringBuilder topicName = new StringBuilder(TOPIC_PREFIX);
      topicName.append(entry.getKey());
      topicName.append(JSON_SUFFIX);
      windowConfiguration.setAggregateTopicName(topicName.toString());
      
      windowConfigurations.put(entry.getKey(), windowConfiguration);

    }
    return windowConfigurations;
  }
}