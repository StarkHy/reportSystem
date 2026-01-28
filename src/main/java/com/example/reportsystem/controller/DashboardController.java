package com.example.reportsystem.controller;

import com.example.reportsystem.service.ReportGenerationService;
import com.example.reportsystem.service.ReportTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.Map;

@Controller
public class DashboardController {

    @Autowired
    private ReportTemplateService templateService;

    @Autowired
    private ReportGenerationService generationService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Map<String, Object> stats = new HashMap<>();

        int templateCount = Math.toIntExact(templateService.count());
        int generationCount = Math.toIntExact(generationService.count());

        stats.put("templateCount", templateCount);
        stats.put("generationCount", generationCount);

        model.addAttribute("stats", stats);
        return "dashboard";
    }
}
