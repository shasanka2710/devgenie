package com.org.devgenie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;


@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableWebSocketMessageBroker
@EnableMongoRepositories
public class DevgenieApplication {
    public static void main(String[] args) {
        SpringApplication.run(DevgenieApplication.class, args);
    }
}
