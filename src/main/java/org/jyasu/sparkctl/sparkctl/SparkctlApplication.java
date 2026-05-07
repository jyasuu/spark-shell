package org.jyasu.sparkctl.sparkctl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(SparkConnectionConfig.class)
public class SparkctlApplication {

    public static void main(String[] args) {
        SpringApplication.run(SparkctlApplication.class, args);
    }
}
