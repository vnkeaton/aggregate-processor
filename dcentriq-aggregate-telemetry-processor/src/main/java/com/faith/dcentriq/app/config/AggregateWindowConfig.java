package com.faith.dcentriq.app.config;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@ConfigurationProperties(prefix = "aggregate.processor.windows")
@Data
public class AggregateWindowConfig {

  //private Map<String, Long> duration;
  //private Map<String, Long> grace;
  private Map<String, Long> duration;
}
