package com.faith.dcentriq.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.faith.dcentriq.app.config.AggregateWindowConfig;
import com.faith.dcentriq.processors.common.utils.CommonConstants;

@SpringBootApplication(scanBasePackages = 
	{ 
	  CommonConstants.APP_BASE_PACKAGE, 
	  CommonConstants.COMMON_BASE_PACKAGE 
	}, 
	  exclude = 
	{ 
	  SpringDataWebAutoConfiguration.class 
	})
@EntityScan(basePackages = CommonConstants.ENTITY_BASE_PACKAGE)
@EnableJpaRepositories(basePackages = CommonConstants.REPOSITORY_BASE_PACKAGE)
@EnableConfigurationProperties({AggregateWindowConfig.class})
public class AggregateTelemetryProcessorApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(AggregateTelemetryProcessorApplication.class);
	    app.setRegisterShutdownHook(false);
	    app.run(args);
    }
}