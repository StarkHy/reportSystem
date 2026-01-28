package com.example.reportsystem.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    @GetMapping("/hello")
    public Map<String, Object> hello() {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Hello from Report Controller!");
        result.put("status", "success");
        return result;
    }

    @GetMapping("/info")
    public Map<String, Object> getInfo(@RequestParam(required = false) String name) {
        Map<String, Object> result = new HashMap<>();
        result.put("name", name != null ? name : "Guest");
        result.put("application", "Report System");
        result.put("version", "1.0.0");
        result.put("javaVersion", System.getProperty("java.version"));
        return result;
    }
}
