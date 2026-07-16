package org.example.rec.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RecController {

    @GetMapping("/rec/hello")
    public String hello() {
        return "📊 推荐服务已就绪！";
    }
}
