package org.example.mall.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.mall.entity.Item;
import org.example.mall.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/item")
public class ItemController {

    @Autowired
    private ItemService itemService;

    @GetMapping("/list")
    public List<Item> list() {
        return itemService.list();
    }

    @GetMapping("/{id}")
    public Item getById(@PathVariable Integer id) {
        return itemService.getById(id);
    }

    /** 根据分类ID查询商品 */
    @GetMapping("/listByCategory/{categoryId}")
    public List<Item> listByCategory(@PathVariable Integer categoryId) {
        QueryWrapper<Item> wrapper = new QueryWrapper<>();
        wrapper.eq("category_id", categoryId);
        return itemService.list(wrapper);
    }

    @PostMapping("/save")
    public boolean save(@RequestBody Item item) {
        return itemService.save(item);
    }

    @PutMapping("/update")
    public boolean update(@RequestBody Item item) {
        return itemService.updateById(item);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Integer id) {
        return itemService.removeById(id);
    }
}
