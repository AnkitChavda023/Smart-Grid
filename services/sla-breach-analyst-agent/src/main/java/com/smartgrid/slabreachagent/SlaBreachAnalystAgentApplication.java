package com.smartgrid.slabreachagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SlaBreachAnalystAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(SlaBreachAnalystAgentApplication.class, args);
    }
}
