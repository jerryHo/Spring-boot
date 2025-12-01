// package com.example.demotwo;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.web.bind.annotation.*;

// import java.util.List;

// @RestController
// @RequestMapping("/api/test")
// public class TestEntityController {

//     @Autowired
//     private TestEntityRepository repository;

//     // Endpoint to create a new TestEntity
//     @PostMapping("/add")
//     public TestEntity addTestEntity(@RequestBody TestEntity testEntity) {
//         return repository.save(testEntity);
//     }

//     // Endpoint to retrieve all TestEntities
//     @GetMapping("/all")
//     public List<TestEntity> getAllTestEntities() {
//         return repository.findAll();
//     }

    
// }