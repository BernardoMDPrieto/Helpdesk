package com.bduarte.helpdeskserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class HelpdeskServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(HelpdeskServerApplication.class, args);
    }

}
