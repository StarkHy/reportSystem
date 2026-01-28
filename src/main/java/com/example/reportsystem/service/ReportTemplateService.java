package com.example.reportsystem.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.reportsystem.entity.ReportTemplate;
import com.example.reportsystem.mapper.ReportTemplateMapper;
import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ReportTemplateService extends ServiceImpl<ReportTemplateMapper, ReportTemplate> {

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.templateBucketName:report-templates}")
    private String templateBucketName;

    public void initBucket() {
        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(templateBucketName)
                    .build())) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(templateBucketName)
                        .build());
                log.info("创建模板存储桶成功: {}", templateBucketName);
            }
        } catch (Exception e) {
            log.error("初始化存储桶失败", e);
        }
    }

    public Page<ReportTemplate> getTemplateList(Integer pageNum, Integer pageSize, String keyword) {
        Page<ReportTemplate> page = new Page<>(pageNum, pageSize);
        QueryWrapper<ReportTemplate> wrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like("name", keyword)
                    .or()
                    .like("description", keyword);
        }
        wrapper.orderByDesc("create_time");
        return page(page, wrapper);
    }

    public ReportTemplate getTemplateById(Long id) {
        return getById(id);
    }

    public ReportTemplate uploadTemplate(MultipartFile file, String name, String description,
                                        String apiUrl, String groovyScriptContent, String createdBy) {
        initBucket();

        try {
            String originalFilename = file.getOriginalFilename();
            String extension = FilenameUtils.getExtension(originalFilename);
            String fileName = System.currentTimeMillis() + "_" + originalFilename;
            String objectName = "templates/" + fileName;

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(templateBucketName)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            ReportTemplate template = new ReportTemplate();
            template.setName(name);
            template.setDescription(description);
            template.setFileName(fileName);
            template.setFileSize(file.getSize());
            template.setFileType(extension);
            template.setFilePath(objectName);
            template.setApiUrl(apiUrl);
            template.setGroovyScript(groovyScriptContent);
            template.setStatus(1);
            template.setCreatedBy(createdBy);
            template.setCreateTime(LocalDateTime.now());
            template.setUpdateTime(LocalDateTime.now());

            save(template);
            return template;

        } catch (Exception e) {
            log.error("上传模板失败", e);
            throw new RuntimeException("上传模板失败: " + e.getMessage());
        }
    }

    public ReportTemplate updateTemplate(Long id, String name, String description,
                                        String apiUrl, String groovyScriptContent) {
        ReportTemplate template = getById(id);
        if (template == null) {
            throw new RuntimeException("模板不存在");
        }

        try {
            template.setName(name);
            template.setDescription(description);
            template.setApiUrl(apiUrl);
            template.setGroovyScript(groovyScriptContent);

            template.setUpdateTime(LocalDateTime.now());
            updateById(template);
            return template;
        } catch (Exception e) {
            log.error("更新模板失败", e);
            throw new RuntimeException("更新模板失败: " + e.getMessage());
        }
    }

    public ReportTemplate updateTemplateFile(Long id, MultipartFile file) {
        initBucket();

        ReportTemplate template = getById(id);
        if (template == null) {
            throw new RuntimeException("模板不存在");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String extension = FilenameUtils.getExtension(originalFilename);
            String fileName = System.currentTimeMillis() + "_" + originalFilename;
            String objectName = "templates/" + fileName;

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(templateBucketName)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            template.setFileName(fileName);
            template.setFileSize(file.getSize());
            template.setFileType(extension);
            template.setFilePath(objectName);
            template.setUpdateTime(LocalDateTime.now());

            updateById(template);
            return template;

        } catch (Exception e) {
            log.error("更新模板文件失败", e);
            throw new RuntimeException("更新模板文件失败: " + e.getMessage());
        }
    }

    public void deleteTemplate(Long id) {
        ReportTemplate template = getById(id);
        if (template != null) {
            try {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(templateBucketName)
                                .object(template.getFilePath())
                                .build()
                );
            } catch (Exception e) {
                log.error("删除文件失败", e);
            }
            removeById(id);
        }
    }

    public InputStream downloadTemplate(Long id) {
        ReportTemplate template = getById(id);
        if (template == null) {
            throw new RuntimeException("模板不存在");
        }

        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(templateBucketName)
                            .object(template.getFilePath())
                            .build()
            );
        } catch (Exception e) {
            log.error("下载模板失败", e);
            throw new RuntimeException("下载模板失败: " + e.getMessage());
        }
    }

    public String getTemplateDownloadUrl(Long id) {
        ReportTemplate template = getById(id);
        if (template == null) {
            throw new RuntimeException("模板不存在");
        }

        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(templateBucketName)
                            .object(template.getFilePath())
                            .expiry(7, TimeUnit.DAYS)
                            .build()
            );
        } catch (Exception e) {
            log.error("获取下载链接失败", e);
            throw new RuntimeException("获取下载链接失败: " + e.getMessage());
        }
    }
}
