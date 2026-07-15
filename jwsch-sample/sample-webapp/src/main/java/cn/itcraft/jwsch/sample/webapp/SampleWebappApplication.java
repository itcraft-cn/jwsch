package cn.itcraft.jwsch.sample.webapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SampleWebappApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(SampleWebappApplication.class, args);
    }
}