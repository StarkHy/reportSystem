package com.example.reportsystem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.reportsystem.entity.ReportTemplate;
import com.example.reportsystem.service.ReportTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/template")
public class TemplateController {

    @Autowired
    private ReportTemplateService templateService;

    @GetMapping("/list")
    public String list(@RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize,
                       @RequestParam(required = false) String keyword,
                       Model model) {
        Page<ReportTemplate> page = templateService.getTemplateList(pageNum, pageSize, keyword);
        model.addAttribute("page", page);
        model.addAttribute("keyword", keyword);
        return "template/list";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("template", new ReportTemplate());
        return "template/add";
    }

    @PostMapping("/add")
    @ResponseBody
    public Map<String, Object> add(@RequestParam("file") MultipartFile file,
                                   @RequestParam String name,
                                   @RequestParam(required = false) String description,
                                   @RequestParam(required = false) String apiUrl,
                                   @RequestParam(required = false) String groovyScriptContent,
                                   HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            String createdBy = request.getRemoteUser();
            if (createdBy == null) createdBy = "admin";

            ReportTemplate template = templateService.uploadTemplate(file, name, description, apiUrl, groovyScriptContent, createdBy);
            result.put("success", true);
            result.put("message", "上传成功");
            result.put("data", template);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "上传失败: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        ReportTemplate template = templateService.getTemplateById(id);
        model.addAttribute("template", template);
        return "template/edit";
    }

    @GetMapping("/{id}")
    @ResponseBody
    public Map<String, Object> getById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            ReportTemplate template = templateService.getTemplateById(id);
            result.put("success", true);
            result.put("data", template);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PostMapping("/update/{id}")
    @ResponseBody
    public Map<String, Object> update(@PathVariable Long id,
                                       @RequestParam(required = false) MultipartFile file,
                                       @RequestParam String name,
                                       @RequestParam(required = false) String description,
                                       @RequestParam(required = false) String apiUrl,
                                       @RequestParam(required = false) String groovyScriptContent) {
        Map<String, Object> result = new HashMap<>();
        try {
            ReportTemplate template = templateService.updateTemplate(id, name, description, apiUrl, groovyScriptContent);
            if (file != null) {
                templateService.updateTemplateFile(id, file);
            }
            result.put("success", true);
            result.put("message", "更新成功");
            result.put("data", template);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "更新失败: " + e.getMessage());
        }
        return result;
    }

    @PostMapping("/updateFile")
    @ResponseBody
    public Map<String, Object> updateFile(@RequestParam Long id,
                                         @RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        try {
            ReportTemplate template = templateService.updateTemplateFile(id, file);
            result.put("success", true);
            result.put("message", "文件更新成功");
            result.put("data", template);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "文件更新失败: " + e.getMessage());
        }
        return result;
    }

    @PostMapping("/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            templateService.deleteTemplate(id);
            result.put("success", true);
            result.put("message", "删除成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "删除失败: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        try {
            ReportTemplate template = templateService.getTemplateById(id);
            InputStream inputStream = templateService.downloadTemplate(id);
            byte[] bytes = org.apache.commons.io.IOUtils.toByteArray(inputStream);

            String filename = URLEncoder.encode(template.getFileName(), "UTF-8");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(bytes);
        } catch (Exception e) {
            log.error("下载失败", e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/api/list")
    @ResponseBody
    public Map<String, Object> apiList(@RequestParam(defaultValue = "1") Integer pageNum,
                                       @RequestParam(defaultValue = "10") Integer pageSize,
                                       @RequestParam(required = false) String keyword) {
        Map<String, Object> result = new HashMap<>();
        try {
            Page<ReportTemplate> page = templateService.getTemplateList(pageNum, pageSize, keyword);
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
            ReportTemplate template = templateService.getTemplateById(id);
            result.put("success", true);
            result.put("data", template);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
