// package com.example.demotwo.controller;

// import com.example.demotwo.entity.User;
// import com.example.demotwo.repository.UserRepository;
// import org.springframework.security.crypto.password.PasswordEncoder;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestParam;
// import org.springframework.web.bind.annotation.RestController;
// import org.springframework.web.servlet.view.RedirectView;

// @RestController
// public class AuthController {

//     private final UserRepository userRepository;
//     private final PasswordEncoder passwordEncoder;

//     public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
//         this.userRepository = userRepository;
//         this.passwordEncoder = passwordEncoder;
//     }

//     // 注册提交
//     @PostMapping("/register")
//     public RedirectView register(@RequestParam String username,
//                                  @RequestParam String password,
//                                  @RequestParam String email) {
//         if (userRepository.existsByUsername(username)) {
//             // 注册失败，重定向到注册页并带错误参数
//             return new RedirectView("/register.html?error=true");
//         }
//         // 保存用户
//         User user = new User();
//         user.setUsername(username);
//         user.setPassword(passwordEncoder.encode(password));
//         user.setEmail(email);
//         userRepository.save(user);
//         // 注册成功，重定向到登录页
//         return new RedirectView("/login.html?success=true");
//     }
// }