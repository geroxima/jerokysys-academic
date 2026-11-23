package com.jerokysys.academic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(scanBasePackages = {
        "com.jerokysys.academic",
        "com.jerokysys.commons.exception"
})
@EntityScan("com.jerokysys.commons.domain.entity")
@EnableDiscoveryClient
@EnableCaching
public class AcademicApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(AcademicApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(AcademicApplication.class, args);
    }

}
