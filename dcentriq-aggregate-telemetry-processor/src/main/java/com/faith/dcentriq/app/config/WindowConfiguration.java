package com.faith.dcentriq.app.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WindowConfiguration {

  private Long windowDuration;
  private Long windowGrace;
  private String aggregateTableStoreName;
  private String aggregateTopicName;

  @Override
  public String toString() {
    return "WindowConfiguration{" +
        "windowDuration=" + windowDuration +
        ", windowGrace=" + windowGrace +
        ", aggregateTableStoreName='" + aggregateTableStoreName + '\'' +
        ", aggregateTopicName='" + aggregateTopicName + '\'' +
        '}';
  }

}
