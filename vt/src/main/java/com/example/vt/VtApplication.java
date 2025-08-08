package com.example.vt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestClient;

@SpringBootApplication
public class VtApplication {
    public static void main(String[] args) {
        SpringApplication.run(VtApplication.class, args);
    }

    @Bean
    RestClient rc(RestClient.Builder clientBuilder) {
        return clientBuilder.build();
    }
}

@Controller
@ResponseBody
class CoraIberkleidDemoController {
    private final RestClient rc;

    CoraIberkleidDemoController(RestClient rc) {
        this.rc = rc;
    }

    @GetMapping("/delay")
    String delay() {
        return rc
                .get()
                .uri("https://httpbin.org/delay/5")
                .retrieve().body(String.class);
    }
}