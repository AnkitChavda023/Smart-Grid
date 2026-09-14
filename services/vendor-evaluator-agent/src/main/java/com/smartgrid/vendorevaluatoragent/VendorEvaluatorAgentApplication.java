package com.smartgrid.vendorevaluatoragent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class VendorEvaluatorAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(VendorEvaluatorAgentApplication.class, args);
    }
}
