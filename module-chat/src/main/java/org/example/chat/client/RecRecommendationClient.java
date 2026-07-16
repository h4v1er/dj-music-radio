package org.example.chat.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "module-rec", contextId = "recRecommendationClient")
public interface RecRecommendationClient {

    @GetMapping("/rec/daily")
    Object daily(@RequestParam("userId") Long userId);
}
