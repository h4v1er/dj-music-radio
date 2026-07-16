package org.example.mall.controller;

import org.example.mall.entity.Admin;
import org.example.mall.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @GetMapping("/list")
    public List<Admin> list() {
        return adminService.list();
    }

    @GetMapping("/{id}")
    public Admin getById(@PathVariable Integer id) {
        return adminService.getById(id);
    }

    @PostMapping("/save")
    public boolean save(@RequestBody Admin admin) {
        return adminService.save(admin);
    }

    @PutMapping("/update")
    public boolean update(@RequestBody Admin admin) {
        return adminService.updateById(admin);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Integer id) {
        return adminService.removeById(id);
    }
}
