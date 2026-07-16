package org.example.mall.controller;

import org.example.mall.entity.MallOrder;
import org.example.mall.service.MallOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/order")
public class MallOrderController {

    @Autowired
    private MallOrderService mallOrderService;

    @GetMapping("/list")
    public List<MallOrder> list() {
        return mallOrderService.list();
    }

    @GetMapping("/{id}")
    public MallOrder getById(@PathVariable Integer id) {
        return mallOrderService.getById(id);
    }

    @PostMapping("/save")
    public boolean save(@RequestBody MallOrder order) {
        return mallOrderService.save(order);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Integer id) {
        return mallOrderService.removeById(id);
    }
}
