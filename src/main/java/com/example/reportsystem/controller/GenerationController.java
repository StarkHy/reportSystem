package com.example.reportsystem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.reportsystem.entity.ReportGeneration;
import com.example.reportsystem.entity.ReportTemplate;
import com.example.reportsystem.service.ReportGenerationService;
import com.example.reportsystem.service.ReportTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/generation")
public class GenerationController {

    @Autowired
    private ReportGenerationService generationService;

    @Autowired
    private ReportTemplateService templateService;

    @GetMapping("/list")
    public String list(@RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize,
                       @RequestParam(required = false) Long templateId,
                       Model model) {
        Page<ReportGeneration> page = generationService.getGenerationList(pageNum, pageSize, templateId);
        model.addAttribute("page", page);
        model.addAttribute("templateId", templateId);
        return "generation/list";
    }

    @GetMapping("/create/{templateId}")
    public String createForm(@PathVariable Long templateId, Model model) {
        ReportTemplate template = templateService.getTemplateById(templateId);
        model.addAttribute("template", template);
        return "generation/create";
    }

    @PostMapping("/generate")
    @ResponseBody
    public Map<String, Object> generate(@RequestParam Long templateId,
                                       @RequestBody Map<String, Object> params,
                                       HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            String createdBy = request.getRemoteUser();
            if (createdBy == null) createdBy = "admin";

            ReportGeneration generation = generationService.generateReport(templateId, params, createdBy);
            result.put("success", true);
            result.put("message", "生成成功");
            result.put("data", generation);
        } catch (Exception e) {
            log.error("生成失败", e);
            result.put("success", false);
            result.put("message", "生成失败: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        try {
            ReportGeneration generation = generationService.getGenerationById(id);
            InputStream inputStream = generationService.downloadReport(id);
            byte[] bytes = org.apache.commons.io.IOUtils.toByteArray(inputStream);

            String filename = URLEncoder.encode(generation.getFileName(), "UTF-8");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(bytes);
        } catch (Exception e) {
            log.error("下载失败", e);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            generationService.deleteGeneration(id);
            result.put("success", true);
            result.put("message", "删除成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "删除失败: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/api/list")
    @ResponseBody
    public Map<String, Object> apiList(@RequestParam(defaultValue = "1") Integer pageNum,
                                       @RequestParam(defaultValue = "10") Integer pageSize,
                                       @RequestParam(required = false) Long templateId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Page<ReportGeneration> page = generationService.getGenerationList(pageNum, pageSize, templateId);
            result.put("success", true);
            result.put("data", page.getRecords());
            result.put("total", page.getTotal());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public Map<String, Object> apiGet(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            ReportGeneration generation = generationService.getGenerationById(id);
            result.put("success", true);
            result.put("data", generation);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/api/templates")
    @ResponseBody
    public Map<String, Object> apiTemplates() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<ReportTemplate> templates = templateService.list();
            result.put("success", true);
            result.put("data", templates);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
