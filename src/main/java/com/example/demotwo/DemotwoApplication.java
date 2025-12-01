package com.example.demotwo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
@RequestMapping("/")
public class DemotwoApplication {

  public static void main(String[] args) {
    SpringApplication.run(DemotwoApplication.class, args);
  }

  @GetMapping("/")
  public String hello() {
    return "Hey, Spring Boot 的 Hello World !";
  }

  @GetMapping("/indexx")
  public String helloIndex() {
    return "index";
  }
}
