package com.example.reportsystem.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.config.Configure;
import com.deepoove.poi.config.ConfigureBuilder;
import com.deepoove.poi.plugin.table.LoopRowTableRenderPolicy;
import com.example.reportsystem.entity.ReportGeneration;
import com.example.reportsystem.entity.ReportTemplate;
import com.example.reportsystem.mapper.ReportGenerationMapper;
import com.example.reportsystem.util.ScriptLogger;
import io.minio.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ReportGenerationService extends ServiceImpl<ReportGenerationMapper, ReportGeneration> {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private ReportTemplateService templateService;

    @Value("${minio.bucketName:report-files}")
    private String bucketName;

    @Value("${minio.templateBucketName:report-templates}")
    private String templateBucketName;

    @Value("${word-service.timeout:30000}")
    private int wordServiceTimeout;

    private OkHttpClient httpClient;
    private ScriptEngineManager scriptEngineManager;

    public ReportGenerationService() {
        try {
            // 信任所有证书
            javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
                new javax.net.ssl.X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
            };

            javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            javax.net.ssl.SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            this.httpClient = new OkHttpClient.Builder()
                    .connectTimeout(wordServiceTimeout, TimeUnit.MILLISECONDS)
                    .readTimeout(wordServiceTimeout, TimeUnit.MILLISECONDS)
                    .sslSocketFactory(sslSocketFactory, (javax.net.ssl.X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true)
                    .build();
        } catch (Exception e) {
            log.error("初始化HTTP客户端失败", e);
            this.httpClient = new OkHttpClient.Builder()
                    .connectTimeout(wordServiceTimeout, TimeUnit.MILLISECONDS)
                    .readTimeout(wordServiceTimeout, TimeUnit.MILLISECONDS)
                    .build();
        }
        this.scriptEngineManager = new ScriptEngineManager();
    }

    public void initBucket() {
        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build())) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
                log.info("创建生成文件存储桶成功: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("初始化存储桶失败", e);
        }
    }

    private InputStream downloadTemplate(String templatePath) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(templateBucketName)
                            .object(templatePath)
                            .build()
            );
        } catch (Exception e) {
            log.error("下载模板文件失败: {}", templatePath, e);
            throw new RuntimeException("下载模板文件失败: " + e.getMessage());
        }
    }

    public Page<ReportGeneration> getGenerationList(Integer pageNum, Integer pageSize, Long templateId) {
        Page<ReportGeneration> page = new Page<>(pageNum, pageSize);
        QueryWrapper<ReportGeneration> wrapper = new QueryWrapper<>();
        if (templateId != null) {
            wrapper.eq("template_id", templateId);
        }
        wrapper.orderByDesc("create_time");
        return page(page, wrapper);
    }

    public ReportGeneration getGenerationById(Long id) {
        return getById(id);
    }

    public ReportGeneration generateReport(Long templateId, Map<String, Object> params, String createdBy) {
        ReportTemplate template = templateService.getTemplateById(templateId);
        if (template == null) {
            throw new RuntimeException("模板不存在");
        }

        initBucket();

        // 创建脚本日志收集器
        ScriptLogger scriptLogger = new ScriptLogger();

        ReportGeneration generation = new ReportGeneration();
        generation.setTemplateId(templateId);
        generation.setTemplateName(template.getName());
        boolean useApi = true;
        if (params != null && params.containsKey("_useApi")) {
            Object flag = params.get("_useApi");
            if (flag != null) {
                useApi = Boolean.parseBoolean(flag.toString());
            }
            params.remove("_useApi");
        }
        generation.setRequestData(params != null ? params.toString() : "{}");
        generation.setStatus(0);
        generation.setCreatedBy(createdBy);
        generation.setCreateTime(LocalDateTime.now());
        generation.setUpdateTime(LocalDateTime.now());

        try {
            scriptLogger.info("开始生成报表，模板ID: " + templateId + ", 模板名称: " + template.getName());
            scriptLogger.info("数据来源: " + (useApi ? "API接口" : "手动输入"));
            if (params != null) {
                scriptLogger.debug("请求参数: " + params.toString());
            }
            String fileName = LocalDateTime.now().toString().replace(":", "-") + "_" + template.getFileName();
            String objectName = "generated/" + templateId + "/" + fileName;

            generation.setFileName(fileName);

            save(generation);

            // 1. 从 API 获取数据（传递用户参数）
            Map<String, Object> apiData = params;
            if (useApi && template.getApiUrl() != null && !template.getApiUrl().trim().isEmpty()) {
                generation.setDataSource("API");
                scriptLogger.info("从 API 获取数据: " + template.getApiUrl());
                try {
                    apiData = fetchDataFromApi(template.getApiUrl(), params);
                    generation.setResponseData(apiData.toString());
                    scriptLogger.info("API 数据获取成功，数据大小: " + apiData.size() + " 条记录");
                } catch (Exception apiEx) {
                    log.error("API 数据获取失败，将回退到手动输入数据", apiEx);
                    scriptLogger.error("API 数据获取失败: " + apiEx.getMessage(), apiEx);
                    apiData = params != null ? params : java.util.Collections.emptyMap();
                    generation.setResponseData("API 调用失败，已回退到手动数据: " + apiEx.getMessage());
                    generation.setDataSource("MANUAL");
                }
            } else {
                generation.setDataSource("MANUAL");
                scriptLogger.info("使用手动输入数据");
            }

            // 2. 使用 Groovy 脚本处理数据
            Map<String, Object> renderData = apiData;
            String groovyScriptContent = template.getGroovyScript();

            ConfigureBuilder builder = Configure.builder();

            if (groovyScriptContent != null && !groovyScriptContent.trim().isEmpty()) {
                scriptLogger.info("开始执行 Groovy 脚本");
                renderData = executeGroovyScript(groovyScriptContent, apiData, params, builder, scriptLogger);
                scriptLogger.info("Groovy 脚本执行完成");
            } else {
                scriptLogger.info("未配置 Groovy 脚本，使用原始数据");
            }

            // 2.5 自动探测 renderData 中的 List 并绑定 LoopRowTableRenderPolicy
            // 只有绑定了 Policy，{{#list}} 标签才能被正确识别为表格循环
            if (renderData != null) {
                renderData.forEach((key, value) -> {
                    if (value instanceof java.util.List) {
                        scriptLogger.info("检测到列表数据 [" + key + "]，自动绑定 LoopRowTableRenderPolicy");

                        // 如果是复杂的监测报表（需要跨行合并），可以在 Groovy 脚本中预设合并列
                        // 这里我们先绑定最基础的循环策略
                        builder.bind(key, new LoopRowTableRenderPolicy());
                    }
                });
            }


            // 3. 渲染 Word
            scriptLogger.info("开始渲染 Word 文档");
            InputStream templateStream = downloadTemplate(template.getFilePath());
            XWPFTemplate wordTemplate = XWPFTemplate.compile(templateStream, builder.build());
            wordTemplate.render(renderData);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            wordTemplate.write(outputStream);
            wordTemplate.close();

            byte[] fileBytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(fileBytes);
            scriptLogger.info("Word 文档渲染完成，文件大小: " + fileBytes.length + " 字节");

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, fileBytes.length, -1)
                            .contentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                            .build()
            );

            generation.setFilePath(objectName);
            generation.setFileSize((long) fileBytes.length);
            generation.setStatus(1);
            generation.setExecutionLog(scriptLogger.getFullLog());

            scriptLogger.info("报表生成成功！");
            updateById(generation);

            return generation;

        } catch (Exception e) {
            log.error("生成报告失败", e);
            scriptLogger.error("生成报告失败: " + e.getMessage(), e);
            generation.setStatus(2);
            generation.setExecutionLog(scriptLogger.getFullLog());
            updateById(generation);
            throw new RuntimeException("生成报告失败: " + e.getMessage());
        }
    }

    private Map<String, Object> fetchDataFromApi(String apiUrl, Map<String, Object> params) {
        try {
            HttpUrl.Builder urlBuilder = HttpUrl.parse(apiUrl).newBuilder();
            if (params != null) {
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    urlBuilder.addQueryParameter(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : "");
                }
            }

            Request request = new Request.Builder()
                    .url(urlBuilder.build())
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("API 调用失败: " + response.code());
                }

                String responseBody = response.body().string();
                return com.alibaba.fastjson.JSON.parseObject(responseBody, Map.class);
            }
        } catch (Exception e) {
            log.error("调用 API 失败: {}", apiUrl, e);
            throw new RuntimeException("调用 API 失败: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> executeGroovyScript(String scriptContent, Map<String, Object> data, Map<String, Object> params, ConfigureBuilder builder, ScriptLogger scriptLogger) {
        try {
            ScriptEngine engine = scriptEngineManager.getEngineByName("groovy");
            if (engine == null) {
                log.error("Groovy 脚本引擎不可用");
                scriptLogger.error("Groovy 脚本引擎不可用");
                return data;
            }

            // 注入变量
            engine.put("data", data);
            engine.put("params", params);
            engine.put("config", builder);
            engine.put("log", scriptLogger);

            Object result = engine.eval(scriptContent);

            if (result instanceof Map) {
                scriptLogger.debug("Groovy 脚本返回 Map 类型数据");
                return (Map<String, Object>) result;
            } else {
                scriptLogger.debug("Groovy 脚本返回非 Map 类型数据，将作为 scriptResult 添加到数据中");
                Map<String, Object> resultMap = data;
                resultMap.put("scriptResult", result);
                return resultMap;
            }
        } catch (Exception e) {
            log.error("执行 Groovy 脚本失败", e);
            scriptLogger.error("执行 Groovy 脚本失败: " + e.getMessage(), e);
            return data;
        }
    }

    public void saveGeneratedFile(Long id, InputStream inputStream, long fileSize) {
        initBucket();

        ReportGeneration generation = getById(id);
        if (generation == null) {
            throw new RuntimeException("生成记录不存在");
        }

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(generation.getFilePath())
                            .stream(inputStream, fileSize, -1)
                            .contentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                            .build()
            );

            generation.setFileSize(fileSize);
            generation.setStatus(1);
            generation.setUpdateTime(LocalDateTime.now());
            updateById(generation);

        } catch (Exception e) {
            log.error("保存生成文件失败", e);
            throw new RuntimeException("保存生成文件失败: " + e.getMessage());
        }
    }

    public InputStream downloadReport(Long id) {
        ReportGeneration generation = getById(id);
        if (generation == null) {
            throw new RuntimeException("生成记录不存在");
        }

        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(generation.getFilePath())
                            .build()
            );
        } catch (Exception e) {
            log.error("下载报告失败", e);
            throw new RuntimeException("下载报告失败: " + e.getMessage());
        }
    }

    public String getDownloadUrl(Long id) {
        ReportGeneration generation = getById(id);
        if (generation == null) {
            throw new RuntimeException("生成记录不存在");
        }

        try {
            return minioClient.getPresignedObjectUrl(
                    io.minio.GetPresignedObjectUrlArgs.builder()
                            .method(io.minio.http.Method.GET)
                            .bucket(bucketName)
                            .object(generation.getFilePath())
                            .expiry(7, TimeUnit.DAYS)
                            .build()
            );
        } catch (Exception e) {
            log.error("获取下载链接失败", e);
            throw new RuntimeException("获取下载链接失败: " + e.getMessage());
        }
    }

    public void deleteGeneration(Long id) {
        ReportGeneration generation = getById(id);
        if (generation != null) {
            try {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(generation.getFilePath())
                                .build()
                );
            } catch (Exception e) {
                log.error("删除文件失败", e);
            }
            removeById(id);
        }
    }
}
