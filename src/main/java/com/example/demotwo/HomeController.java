package com.example.demotwo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class HomeController {

  // Forward root path to index.html
  @GetMapping("/")
  public String redirectToHomePage() {
    return "forward:/index.html";
  }

  // Keep your existing endpoint
  @GetMapping("/indexx")
  public String helloIndex() {
    return "index";
  }
}