package com.smartgrid.disruptionagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DisruptionDetectorAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(DisruptionDetectorAgentApplication.class, args);
    }
}
