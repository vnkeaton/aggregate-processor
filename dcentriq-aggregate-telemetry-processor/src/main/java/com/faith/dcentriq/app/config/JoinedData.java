package com.faith.dcentriq.app.config;

import com.faith.dcentriq.processors.common.model.tagslatest.TimestampedValue;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JoinedData {
   private TimestampedValue streamValue;
  private TimestampedValue heartbeatValue; // This will be null if no matching key in GlobalKTable
  

}
