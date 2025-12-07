// package com.example.demotwo.controller;

// import com.example.demotwo.entity.UploadFile;
// import com.example.demotwo.repository.FileRepository;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.core.io.Resource;
// import org.springframework.core.io.UrlResource;
// import org.springframework.http.HttpHeaders;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.core.Authentication;
// import org.springframework.security.core.userdetails.UserDetails;
// import org.springframework.web.bind.annotation.*;
// import org.springframework.web.multipart.MultipartFile;
// import org.springframework.web.servlet.view.RedirectView;

// import java.io.File;
// import java.io.IOException;
// import java.net.MalformedURLException;
// import java.nio.file.Path;
// import java.nio.file.Paths;
// import java.util.List;
// import java.util.UUID;

// @RestController
// @RequestMapping("/file")
// public class FileController {

//     @Value("${file.upload.path}")
//     private String uploadPath;

//     private final FileRepository fileRepository;

//     public FileController(FileRepository fileRepository) {
//         this.fileRepository = fileRepository;
//     }

//     // 文件上传
//     @PostMapping("/upload")
//     public RedirectView uploadFile(@RequestParam("file") MultipartFile file, Authentication authentication) {
//         UserDetails userDetails = (UserDetails) authentication.getPrincipal();
//         String username = userDetails.getUsername();

//         if (file.isEmpty()) {
//             return new RedirectView("/upload.html?error=文件不能为空");
//         }

//         try {
//             // 创建上传目录
//             File uploadDir = new File(uploadPath);
//             if (!uploadDir.exists()) {
//                 uploadDir.mkdirs();
//             }

//             // 生成唯一文件名
//             String originalName = file.getOriginalFilename();
//             String suffix = originalName.substring(originalName.lastIndexOf("."));
//             String storeName = UUID.randomUUID().toString() + suffix;
//             File destFile = new File(uploadPath + File.separator + storeName);
//             file.transferTo(destFile);

//             // 保存到数据库
//             UploadFile uploadFile = new UploadFile();
//             uploadFile.setOriginalFileName(originalName);
//             uploadFile.setStoreFileName(storeName);
//             uploadFile.setFileSize(file.getSize());
//             uploadFile.setFileType(suffix.substring(1));
//             uploadFile.setFilePath(destFile.getAbsolutePath());
//             uploadFile.setUsername(username);
//             fileRepository.save(uploadFile);

//             return new RedirectView("/upload.html?success=true");
//         } catch (IOException e) {
//             return new RedirectView("/upload.html?error=上传失败：" + e.getMessage());
//         }
//     }

//     // 我的文件列表（返回JSON，供前端渲染）
//     @GetMapping("/list")
//     public ResponseEntity<List<UploadFile>> getFileList(Authentication authentication) {
//         UserDetails userDetails = (UserDetails) authentication.getPrincipal();
//         List<UploadFile> fileList = fileRepository.findByUsernameOrderByUploadTimeDesc(userDetails.getUsername());
//         return ResponseEntity.ok(fileList);
//     }

//     // 文件下载
//     @GetMapping("/download/{id}")
//     public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
//         UploadFile uploadFile = fileRepository.findById(id)
//                 .orElseThrow(() -> new RuntimeException("文件不存在"));

//         Path filePath = Paths.get(uploadFile.getFilePath());
//         Resource resource;
//         try {
//             resource = new UrlResource(filePath.toUri());
//         } catch (MalformedURLException e) {
//             throw new RuntimeException("文件路径错误");
//         }

//         return ResponseEntity.ok()
//                 .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + uploadFile.getOriginalFileName() + "\"")
//                 .body(resource);
//     }

//     // 删除文件
//     @GetMapping("/delete/{id}")
//     public RedirectView deleteFile(@PathVariable Long id, Authentication authentication) {
//         UserDetails userDetails = (UserDetails) authentication.getPrincipal();
//         UploadFile uploadFile = fileRepository.findById(id).orElseThrow(() -> new RuntimeException("文件不存在"));
//         if (uploadFile.getUsername().equals(userDetails.getUsername())) {
//             File file = new File(uploadFile.getFilePath());
//             if (file.exists()) file.delete();
//             fileRepository.delete(uploadFile);
//         }
//         return new RedirectView("/home.html");
//     }
// }