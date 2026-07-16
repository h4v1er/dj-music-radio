package org.example.mall.controller;

import org.example.mall.entity.Category;
import org.example.mall.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/list")
    public List<Category> list() {
        return categoryService.list();
    }

    @GetMapping("/{id}")
    public Category getById(@PathVariable Integer id) {
        return categoryService.getById(id);
    }

    @PostMapping("/save")
    public boolean save(@RequestBody Category category) {
        return categoryService.save(category);
    }

    @PutMapping("/update")
    public boolean update(@RequestBody Category category) {
        return categoryService.updateById(category);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Integer id) {
        return categoryService.removeById(id);
    }
}
