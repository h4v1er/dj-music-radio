package org.example.rec;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling        // 定时任务（每日推荐）
@MapperScan("org.example.rec.mapper")
public class RecApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecApplication.class, args);
    }
}
