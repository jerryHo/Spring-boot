package com.example.demotwo;

import com.example.demotwo.repository.FileRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/")
public class HomeController {

  // private final FileRepository fileRepository;

  // public HomeController(FileRepository fileRepository) {
  //   this.fileRepository = fileRepository;
  // }

  // 🌟 Forwards root path to index.html (your home page)
  @GetMapping("/")
  public String redirectToHomePage() {
    return "forward:/index.html";
  }

  // 🌟 Your original endpoint (unchanged)
  @GetMapping("/indexx")
  @ResponseBody
  public String helloIndex() {
    return "index";
  }

  // 🌟 NEW: Dedicated API endpoint for health check (JS will fetch this)
  @GetMapping("/api/health")
  @ResponseBody
  public String healthCheck() {
    return "OK"; // Simple response (no HTML)
  }

  // @GetMapping("/home")
  // public String home(Authentication authentication, Model model) {
  //   UserDetails userDetails = (UserDetails) authentication.getPrincipal();
  //   String username = userDetails.getUsername();

  //   // 统计文件类型数量（适配演示）
  //   long docCount = fileRepository.countByUsernameAndFileTypeContaining(
  //     username,
  //     "doc"
  //   );
  //   long xlsxCount = fileRepository.countByUsernameAndFileTypeContaining(
  //     username,
  //     "xlsx"
  //   );
  //   long pngCount = fileRepository.countByUsernameAndFileTypeContaining(
  //     username,
  //     "png"
  //   );
  //   long otherCount =
  //     fileRepository.countByUsernameAndFileTypeContaining(username, "txt") +
  //     fileRepository.countByUsernameAndFileTypeContaining(username, "pdf");

  //   // 传递数据给ECharts
  //   model.addAttribute("docCount", docCount);
  //   model.addAttribute("xlsxCount", xlsxCount);
  //   model.addAttribute("pngCount", pngCount);
  //   model.addAttribute("otherCount", otherCount);

  //   return "home";
  // }
}
