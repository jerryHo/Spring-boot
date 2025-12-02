package com.example.demotwo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Remove @RestController and @RequestMapping from here
@SpringBootApplication
public class DemotwoApplication {

  public static void main(String[] args) {
    SpringApplication.run(DemotwoApplication.class, args);
  }
}