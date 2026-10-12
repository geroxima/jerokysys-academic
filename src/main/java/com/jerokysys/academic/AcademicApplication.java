package com.jerokysys.academic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {
        "com.jerokysys.academic",
        "com.jerokysys.commons.exception"
})
@EntityScan("com.jerokysys.commons.domain.entity")
@EnableDiscoveryClient
public class AcademicApplication {

    public static void main(String[] args) {
        SpringApplication.run(AcademicApplication.class, args);
    }

}
