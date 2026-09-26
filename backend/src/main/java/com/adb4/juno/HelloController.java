package com.adb4.juno;

import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
class HelloController {
    @GetMapping("/hello")
    Map<String, String> hello() {
        return Map.of("message", "Hello from Spring Boot");
    }
}