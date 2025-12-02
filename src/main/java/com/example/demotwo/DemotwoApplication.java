package com.example.demotwo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// 🌟 No @RestController/@Controller here – just the main app class
@SpringBootApplication
public class DemotwoApplication {

  public static void main(String[] args) {
    SpringApplication.run(DemotwoApplication.class, args);
  }
}