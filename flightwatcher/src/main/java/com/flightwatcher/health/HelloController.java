package com.flightwatcher.health;

import com.flightwatcher.health.HealthResponse;
import com.flightwatcher.health.HealthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/api/hello/{name}")
    public String hello(@PathVariable String name) {
        return "Hello, " + name + "!";
    }

}
