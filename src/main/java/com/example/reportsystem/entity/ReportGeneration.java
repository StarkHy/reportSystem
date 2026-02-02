package com.example.reportsystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("report_generation")
public class ReportGeneration {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long templateId;

    private String templateName;

    private String requestData;

    private String responseData;

    @TableField("data_source")
    private String dataSource;

    private String fileName;

    private Long fileSize;

    private String filePath;

    private String fileUrl;

    private Integer status;

    private String errorMessage;

    @TableField("execution_log")
    private String executionLog;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
