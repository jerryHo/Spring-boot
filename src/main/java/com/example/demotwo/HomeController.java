package com.example.demotwo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller // 🌟 Use @Controller for routing to static files
@RequestMapping("/")
public class HomeController {

  // 🌟 Forward root path to index.html (no @ResponseBody)
  @GetMapping("/")
  public String redirectToHomePage() {
    return "forward:/index.html";
  }

  // 🌟 Use @ResponseBody for API endpoints that return strings/JSON
  @GetMapping("/indexx")
  @ResponseBody
  public String helloIndex() {
    return "index";
  }
}