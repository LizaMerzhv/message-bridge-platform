package com.example.notifi.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NotifiWorkerApplication {
  public static void main(String[] args) {
    SpringApplication.run(NotifiWorkerApplication.class, args);
  }
}
